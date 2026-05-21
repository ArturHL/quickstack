variable "sufix" {
  description = "Sufix to append to all resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "Private subnet IDs"
  type        = list(string)
}

variable "security_group_alb_id" {
  description = "Security group ID for ALB"
  type        = string
}

variable "security_group_ecs_id" {
  description = "Security group ID for ECS"
  type        = string
}

variable "security_group_db_id" {
  description = "Security group ID for DB"
  type        = string
}