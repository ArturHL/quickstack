// IAM Role for Lambda
resource "aws_iam_role" "lambda_exec_role" {
  name = "quickstack-lambda-exec-role-${var.sufix}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_vpc_access" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

// Lambda Function
resource "aws_lambda_function" "api_lambda" {
  function_name = "quickstack-api-${var.sufix}"
  role          = aws_iam_role.lambda_exec_role.arn

  // The code will be uploaded outside of Terraform (via CI/CD or aws cli)
  // For initial creation, we can use a dummy payload or reference the target jar
  filename      = "../backend/quickstack-app/target/quickstack-app-0.0.1-SNAPSHOT.jar"
  
  // Terraform needs the file to exist to compute hash.
  
  handler       = "com.quickstack.StreamLambdaHandler::handleRequest"
  runtime       = "java17"
  memory_size   = 2048
  timeout       = 30

  vpc_config {
    subnet_ids         = var.vpc_subnet_ids
    security_group_ids = [var.security_group_id]
  }

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE = "prod"
      SPRING_DATASOURCE_URL  = "jdbc:postgresql://${var.rds_proxy_endpoint}:5432/quickstack"
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI = var.cognito_issuer_uri
      // Las credenciales de DB deberían ser inyectadas o resueltas con Spring Cloud AWS
      // Por simplicidad en este paso las dejamos pendientes o como dummy
    }
  }

  snap_start {
    apply_on = "PublishedVersions"
  }
}

// API Gateway REST API
resource "aws_api_gateway_rest_api" "api" {
  name        = "quickstack-api-${var.sufix}"
  description = "API Gateway para QuickStack Serverless"
}

resource "aws_api_gateway_resource" "proxy" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "proxy_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.proxy.id
  http_method   = "ANY"
  authorization = "NONE" // Can use COGNITO_USER_POOLS here later
}

resource "aws_api_gateway_integration" "lambda_integration" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_method.proxy_method.resource_id
  http_method = aws_api_gateway_method.proxy_method.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.api_lambda.invoke_arn
}

resource "aws_lambda_permission" "apigw_lambda" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.api_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_api_gateway_deployment" "api_deployment" {
  depends_on = [aws_api_gateway_integration.lambda_integration]
  rest_api_id = aws_api_gateway_rest_api.api.id
}

resource "aws_api_gateway_stage" "api_stage" {
  deployment_id = aws_api_gateway_deployment.api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.api.id
  stage_name    = "v1"
}
