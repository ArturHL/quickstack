// Main Security Group for the VPC resources (DB, Proxy, Lambda)
resource "aws_security_group" "main_sg" {
  name        = "main-sg-${var.sufix}"
  description = "Security group for QuickStack resources"
  vpc_id      = var.vpc_id

  ingress {
    description = "Allow all internal VPC traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [var.vpc_cidr_block]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "main-sg-${var.sufix}"
  }
}

output "security_group_id" {
  value = aws_security_group.main_sg.id
}