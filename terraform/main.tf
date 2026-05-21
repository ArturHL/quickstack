# main.tf
# Aquí declararemos los módulos principales que construirán la infraestructura.


module "network" {
  source              = "./modules/network"
  cidr_block          = "10.0.0.0/16"
  sufix               = local.sufix
  s3_service_name     = "com.amazonaws.${var.aws_region}.s3"
  resource_count      = 2
  subnets_cidr        = ["10.0.0.0/24", "10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  subnets_az          = ["${var.aws_region}a", "${var.aws_region}c"]
}

module "security" {
  source = "./modules/security"
  vpc_id = module.network.vpc_id
  vpc_cidr_block = module.network.vpc_cidr_block
  sufix  = local.sufix
}

module "db" {
  source = "./modules/db"
  db_subnet_group_name = module.network.db_subnet_group_name
  security_group_id    = module.security.security_group_id
}
