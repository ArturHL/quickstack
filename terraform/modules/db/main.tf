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
    max_capacity             = 1.0
    min_capacity             = 0
    seconds_until_auto_pause = 300
  }
}

resource "aws_rds_cluster_instance" "aurora-cluster-instance" {
  count                = 1
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
