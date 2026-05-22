variable "sufix" {
  description = "Sufijo para nombrar recursos"
  type        = string
}

variable "api_gateway_arn" {
  description = "ARN del API Gateway stage para asociar el AWS WAF"
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
