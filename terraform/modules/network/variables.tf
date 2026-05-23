variable "cidr_block" {
  description = "CIDR block for the VPC"
  type        = string
}

variable "sufix" {
  description = "Sufix for the resources"
  type        = string
}

variable "s3_service_name" {
  description = "Service name for the S3 endpoint"
  type        = string
}

variable "resource_count" {
  description = "Number of resources to create"
  type        = number
}

variable "subnets_cidr" {
  description = "List of CIDR blocks for the subnets"
  type        = list(string)
}

variable "subnets_az" {
  description = "List of availability zones for the subnets"
  type        = list(string)
}

variable "cognito_service_name" {
  description = "Service name for the Cognito IDP endpoint"
  type        = string
}