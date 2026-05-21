variable "aws_region" {
  description = "Región de AWS donde se desplegará la infraestructura"
  type        = string
}

variable "environment" {
  description = "Entorno de despliegue (ej. dev, staging, prod)"
  type        = string
}