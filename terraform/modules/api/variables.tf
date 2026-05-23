variable "sufix" {
  description = "Sufijo para nombrar recursos"
  type        = string
}

variable "vpc_subnet_ids" {
  description = "IDs de las subredes privadas de la VPC"
  type        = list(string)
}

variable "security_group_id" {
  description = "ID del grupo de seguridad"
  type        = string
}

variable "cluster_endpoint" {
  description = "Endpoint del clúster de base de datos"
  type        = string
}

variable "cognito_issuer_uri" {
  description = "URI del emisor de JWT (Cognito)"
  type        = string
}

variable "cognito_user_pool_id" {
  description = "ID del User Pool de Cognito"
  type        = string
}

variable "domain_name" {
  description = "The root domain name"
  type        = string
}

variable "zone_id" {
  description = "The Route53 Zone ID"
  type        = string
}
