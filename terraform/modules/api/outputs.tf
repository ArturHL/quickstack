output "api_gateway_url" {
  description = "Base URL for API Gateway stage"
  value       = aws_api_gateway_stage.api_stage.invoke_url
}

output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.api_lambda.function_name
}

output "api_gateway_stage_arn" {
  description = "ARN of the API Gateway stage"
  value       = aws_api_gateway_stage.api_stage.arn
}
