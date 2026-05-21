variable "db_subnet_group_name" {
  description = "The name of the subnet group for the Aurora cluster."
  type        = string
}

variable "security_group_id" {
  description = "The ID of the security group for the Aurora cluster."
  type        = string
}
