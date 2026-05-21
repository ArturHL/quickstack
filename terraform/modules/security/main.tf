// Aurora Security Group

resource "aws_security_group" "aurora_cluster" {
  name        = "aurora_cluster"
  description = "Security group for Aurora cluster"
  vpc_id      = var.vpc_id

  tags = {
    Name = "aurora_cluster-${var.sufix}"
  }
}

resource "aws_vpc_security_group_ingress_rule" "allow_db_from_ecs" {
  security_group_id = aws_security_group.aurora_cluster.id
  reference_security_group_id = aws_security_group.ecs_instance.id
  from_port         = 5432
  ip_protocol       = "tcp"
  to_port           = 5432
}

resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_to_ecs" {
  security_group_id = aws_security_group.aurora_cluster.id
  reference_security_group_id = aws_security_group.ecs_instance.id
  ip_protocol       = "-1"
}

// Application Load Balancer Security Group

resource "aws_security_group" "application_load_balancer" {
  name        = "application_load_balancer"
  description = "Security group for application load balancer"
  vpc_id      = var.vpc_id

  tags = {
    Name = "application_load_balancer-${var.sufix}"
  }
}

resource "aws_vpc_security_group_ingress_rule" "allow_http_ipv4" {
  security_group_id = aws_security_group.application_load_balancer.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  ip_protocol       = "tcp"
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "allow_https_ipv4" {
  security_group_id = aws_security_group.application_load_balancer.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  ip_protocol       = "tcp"
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_to_ecs" {
  security_group_id = aws_security_group.application_load_balancer.id
  reference_security_group_id = aws_security_group.ecs_instance.id
  ip_protocol       = "-1"
}

// ECS Instance Security Group

resource "aws_security_group" "ecs_instance" {
  name        = "ecs_instance"
  description = "Security group for ECS instances"
  vpc_id      = var.vpc_id

  tags = {
    Name = "ecs_instance-${var.sufix}"
  }
}

resource "aws_vpc_security_group_ingress_rule" "allow_http_from_alb" {
  security_group_id = aws_security_group.ecs_instance.id
  reference_security_group_id = aws_security_group.application_load_balancer.id
  from_port         = 80
  ip_protocol       = "tcp"
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "allow_https_from_alb" {
  security_group_id = aws_security_group.ecs_instance.id
  reference_security_group_id = aws_security_group.application_load_balancer.id
  from_port         = 443
  ip_protocol       = "tcp"
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_to_alb" {
  security_group_id = aws_security_group.ecs_instance.id
  reference_security_group_id = aws_security_group.application_load_balancer.id
  ip_protocol       = "-1"
}

resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_to_db" {
  security_group_id = aws_security_group.ecs_instance.id
  reference_security_group_id = aws_security_group.aurora_cluster.id
  ip_protocol       = "-1"
}

// ECS Role and Instance Profile

resource "aws_iam_role" "ec2_task_execution_role" {
  name = "ec2_task_execution_role-${var.sufix}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_task_execution_role" {
  role       = aws_iam_role.ec2_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_instance_profile" "ec2_instance_profile" {
  name = "ec2_instance_profile-${var.sufix}"
  role = aws_iam_role.ec2_task_execution_role.name
}

// Outputs

output "security_group_aurora_cluster_id" {
  value = aws_security_group.aurora_cluster.id
}

output "security_group_application_load_balancer_id" {
  value = aws_security_group.application_load_balancer.id
}

output "security_group_ecs_instance_id" {
  value = aws_security_group.ecs_instance.id
}

output "ecs_task_execution_role_id" {
  value = aws_iam_role.ecs_task_execution_role.id
}

output "ecs_instance_profile_name" {
  value = aws_iam_instance_profile.ecs_instance_profile.name
}