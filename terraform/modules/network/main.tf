// VPC

resource "aws_vpc" "vpc_principal" {
  cidr_block = var.cidr_block
  tags = {
    Name = "vpc_principal-${var.sufix}"
  }
}

// Internet Gateway

resource "aws_internet_gateway" "igw_principal" {
  vpc_id = aws_vpc.vpc_principal.id
  tags = {
    Name = "igw_principal-${var.sufix}"
  }
}

// NAT Gateway

resource "aws_nat_gateway" "nat_gateway_principal" {
  count = var.resource_count
  allocation_id = aws_eip.nat_gateway_principal_eip[count.index].id
  subnet_id     = aws_subnet.public_subnet[count.index].id

  tags = {
    Name = "nat_gateway_principal-${var.sufix}-${count.index}"
  }

  depends_on = [aws_internet_gateway.igw_principal]
}

resource "aws_eip" "nat_gateway_principal_eip" {
  count = var.resource_count
  tags = {
    Name = "nat_gateway_principal_eip-${var.sufix}-${count.index}"
  }
}

// Endpoint Gateway

resource "aws_vpc_endpoint" "s3" {
  vpc_id              = aws_vpc.vpc_principal.id
  service_name        = var.s3_service_name
  route_table_ids     = [for i in range(var.resource_count) : aws_route_table.private_route_table[i].id]
  
  tags = {
    Name = "s3_endpoint-${var.sufix}"
  }
}

// Public Subnet

resource "aws_subnet" "public_subnet" {
  count = var.resource_count
  vpc_id                  = aws_vpc.vpc_principal.id
  cidr_block              = var.subnets_cidr[count.index]
  availability_zone       = var.subnets_az[count.index % length(var.subnets_az)]
  map_public_ip_on_launch = true
  tags = {
    Name = "public_subnet-${var.sufix}-${count.index}"
  }
}

resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.vpc_principal.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_principal.id
  }

  tags = {
    Name = "public_route_table-${var.sufix}"
  }
}

resource "aws_route_table_association" "public_route_table_association" {
  count = 2
  subnet_id      = aws_subnet.public_subnet[count.index].id
  route_table_id = aws_route_table.public_route_table.id
}

resource "aws_network_acl" "public_network_acl" {
  vpc_id     = aws_vpc.vpc_principal.id
  subnet_ids = [for i in range(var.resource_count) : aws_subnet.public_subnet[i].id]

  //Ingress
  //Allow all traffic from the internet
  ingress {
    protocol   = "tcp"
    rule_no    = 100
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 80
    to_port    = 80
  }

  ingress {
    protocol   = "tcp"
    rule_no    = 200
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 443
    to_port    = 443
  }

  ingress {
    protocol   = "tcp"
    rule_no    = 300
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 1024
    to_port    = 65535
  }

  //Egress
  //Allow all traffic to the internet
  egress {
    protocol   = "tcp"
    rule_no    = 400
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 80
    to_port    = 80
  }

  egress {
    protocol   = "tcp"
    rule_no    = 500
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 443
    to_port    = 443
  }

  egress {
    protocol   = "tcp"
    rule_no    = 600
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 1024
    to_port    = 65535
  }

  tags = {
    Name = "public_network_acl-${var.sufix}"
  }
}

// Private Subnet

resource "aws_subnet" "private_subnet" {
  count = 2
  vpc_id            = aws_vpc.vpc_principal.id
  cidr_block        = var.subnets_cidr[count.index + var.resource_count]
  availability_zone = var.subnets_az[count.index % length(var.subnets_az)]
  tags = {
    Name = "private_subnet-${var.sufix}-${count.index}"
  }
}

resource "aws_route_table" "private_route_table" {
  count = 2
  vpc_id = aws_vpc.vpc_principal.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat_gateway_principal[count.index].id
  }

  tags = {
    Name = "private_route_table-${var.sufix}-${count.index}"
  }
}

resource "aws_route_table_association" "private_route_table_association" {
  count = 2
  subnet_id      = aws_subnet.private_subnet[count.index].id
  route_table_id = aws_route_table.private_route_table[count.index].id
}

resource "aws_network_acl" "private_network_acl" {
  vpc_id     = aws_vpc.vpc_principal.id
  subnet_ids = [for i in range(var.resource_count) : aws_subnet.private_subnet[i].id]

  //Ingress
  //Allow all internal traffic from the VPC
  ingress {
    protocol   = "tcp"
    rule_no    = 100
    action     = "allow"
    cidr_block = aws_vpc.vpc_principal.cidr_block
    from_port  = 80
    to_port    = 80
  }

  ingress {
    protocol   = "tcp"
    rule_no    = 200
    action     = "allow"
    cidr_block = aws_vpc.vpc_principal.cidr_block
    from_port  = 443
    to_port    = 443
  }

  ingress {
    protocol   = "tcp"
    rule_no    = 300
    action     = "allow"
    cidr_block = aws_vpc.vpc_principal.cidr_block
    from_port  = 1024
    to_port    = 65535
  }

  //Egress
  //Allow all traffic to the internet & to the public subnet
  egress {
    protocol   = "tcp"
    rule_no    = 400
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 80
    to_port    = 80
  }

  egress {
    protocol   = "tcp"
    rule_no    = 500
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 443
    to_port    = 443
  }

  egress {
    protocol   = "tcp"
    rule_no    = 600
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 1024
    to_port    = 65535
  }

  tags = {
    Name = "private_network_acl-${var.sufix}"
  }
}

// Subnet Groups

resource "aws_db_subnet_group" "aurora_db_subnet_group" {
  name       = "aurora_db_subnet_group"
  subnet_ids = [for i in range(var.resource_count) : aws_subnet.private_subnet[i].id]

  tags = {
    Name = "aurora_db_subnet_group"
  }
}

// Outputs

output "vpc_id" {
  value = aws_vpc.vpc_principal.id
}

output "vpc_cidr_block" {
  value = aws_vpc.vpc_principal.cidr_block
}

output "db_subnet_group_name" {
  value = aws_db_subnet_group.aurora_db_subnet_group.name
}

output "public_subnet_ids" {
  value = [for i in range(var.resource_count) : aws_subnet.public_subnet[i].id]
}

output "private_subnet_ids" {
  value = [for i in range(var.resource_count) : aws_subnet.private_subnet[i].id]
}