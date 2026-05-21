output "rds_proxy_endpoint" {
  description = "The endpoint of the RDS Proxy"
  value       = aws_db_proxy.proxy.endpoint
}

output "aurora_cluster_endpoint" {
  description = "The endpoint of the Aurora cluster"
  value       = aws_rds_cluster.aurora-cluster.endpoint
}
