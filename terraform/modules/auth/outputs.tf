output "user_pool_id" {
  description = "ID of the Cognito User Pool"
  value       = aws_cognito_user_pool.pool.id
}

output "client_id" {
  description = "ID of the Cognito User Pool Client"
  value       = aws_cognito_user_pool_client.client.id
}

output "issuer_uri" {
  description = "Issuer URI for the User Pool"
  value       = aws_cognito_user_pool.pool.endpoint
}
