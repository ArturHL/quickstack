variable "aws_region" {
  description = "Región de AWS donde se desplegará la infraestructura"
  type        = string
  default     = "mx-central-1"
}

variable "environment" {
  description = "Entorno de despliegue (ej. dev, staging, prod)"
  type        = string
}

variable "domain_name" {
  description = "Domain name for the application"
  type        = string
  default     = "quickstack.com.mx"
}