output "frontend_bucket_name" {
  description = "Name of the S3 bucket hosting the frontend"
  value       = module.frontend.s3_bucket_name
}

output "cloudfront_distribution_id" {
  description = "ID of the CloudFront distribution"
  value       = module.frontend.cloudfront_distribution_id
}

output "api_gateway_url" {
  description = "URL of the API Gateway"
  value       = module.api.api_gateway_url
}
