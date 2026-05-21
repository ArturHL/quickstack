variable "sufix" {
  description = "Sufijo para nombrar recursos"
  type        = string
}

variable "api_gateway_arn" {
  description = "ARN del API Gateway stage para asociar el AWS WAF"
  type        = string
}
