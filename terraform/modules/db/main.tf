resource "aws_rds_cluster" "aurora-cluster" {
  cluster_identifier     = "aurora-cluster"
  engine                 = "aurora-postgresql"
  engine_mode            = "provisioned"
  engine_version         = "15.15"
  database_name          = "quickstack"
  master_username        = "quickstack"
  master_password        = "must_be_eight_characters"
  storage_encrypted      = true
  db_subnet_group_name   = var.db_subnet_group_name
  vpc_security_group_ids = [var.security_group_id]
  skip_final_snapshot    = true

  serverlessv2_scaling_configuration {
    max_capacity             = 1.0
    min_capacity             = 0.0
    seconds_until_auto_pause = 3600
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