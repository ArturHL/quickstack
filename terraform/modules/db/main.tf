resource "random_password" "master" {
  length  = 16
  special = false
}

resource "aws_rds_cluster" "aurora-cluster" {
  cluster_identifier     = "aurora-cluster"
  engine                 = "aurora-postgresql"
  engine_mode            = "provisioned"
  engine_version         = "15.15"
  database_name          = "quickstack"
  master_username        = "quickstack"
  master_password        = random_password.master.result
  storage_encrypted      = true
  db_subnet_group_name   = var.db_subnet_group_name
  vpc_security_group_ids = [var.security_group_id]
  skip_final_snapshot    = true

  serverlessv2_scaling_configuration {
    max_capacity = 1.0
    min_capacity = 0.5
  }
}

resource "aws_rds_cluster_instance" "aurora-cluster-instance" {
  count                = 2
  cluster_identifier   = aws_rds_cluster.aurora-cluster.id
  instance_class       = "db.serverless"
  engine               = aws_rds_cluster.aurora-cluster.engine
  engine_version       = aws_rds_cluster.aurora-cluster.engine_version
  db_subnet_group_name = var.db_subnet_group_name
}

resource "aws_secretsmanager_secret" "db_secret" {
  name                    = "quickstack/db/proxy-secret-${var.sufix}"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_secret_val" {
  secret_id     = aws_secretsmanager_secret.db_secret.id
  secret_string = jsonencode({
    username             = aws_rds_cluster.aurora-cluster.master_username
    password             = aws_rds_cluster.aurora-cluster.master_password
    engine               = "postgres"
    host                 = aws_rds_cluster.aurora-cluster.endpoint
    port                 = 5432
    dbClusterIdentifier  = aws_rds_cluster.aurora-cluster.cluster_identifier
  })
}

resource "aws_iam_role" "proxy_role" {
  name = "rds-proxy-role-${var.sufix}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "rds.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "proxy_policy" {
  name = "rds-proxy-policy-${var.sufix}"
  role = aws_iam_role.proxy_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = [aws_secretsmanager_secret.db_secret.arn]
      }
    ]
  })
}

resource "aws_db_proxy" "proxy" {
  name                   = "quickstack-proxy-${var.sufix}"
  debug_logging          = false
  engine_family          = "POSTGRESQL"
  idle_client_timeout    = 1800
  require_tls            = true
  role_arn               = aws_iam_role.proxy_role.arn
  vpc_security_group_ids = [var.security_group_id]
  vpc_subnet_ids         = var.private_subnet_ids

  auth {
    auth_scheme = "SECRETS"
    description = "DB proxy auth"
    iam_auth    = "DISABLED"
    secret_arn  = aws_secretsmanager_secret.db_secret.arn
  }
}

resource "aws_db_proxy_default_target_group" "proxy_target_group" {
  db_proxy_name = aws_db_proxy.proxy.name

  connection_pool_config {
    connection_borrow_timeout    = 120
    max_connections_percent      = 100
    max_idle_connections_percent = 50
  }
}

resource "aws_db_proxy_target" "proxy_target" {
  db_cluster_identifier  = aws_rds_cluster.aurora-cluster.id
  db_proxy_name          = aws_db_proxy.proxy.name
  target_group_name      = aws_db_proxy_default_target_group.proxy_target_group.name
}