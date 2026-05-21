// Application Load Balancer

resource "aws_lb" "alb" {
  name               = "alb-${var.sufix}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.security_group_alb_id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = {
    Name = "alb-${var.sufix}"
  }
}

// Application Load Balancer Target Group

resource "aws_lb_target_group" "alb_target_group" {
  name     = "alb-target-group-${var.sufix}"
  port     = 80
  protocol = "HTTP"
  vpc_id   = var.vpc_id

  tags = {
    Name = "alb-target-group-${var.sufix}"
  }
}

// Application Load Balancer Listener

resource "aws_lb_listener" "alb_listener" {
  load_balancer_arn = aws_lb.alb.arn
  port                = 80
  protocol            = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.alb_target_group.arn
  }

  tags = {
    Name = "alb-listener-${var.sufix}"
  }
}

// Auto Scaling Group

resource "aws_autoscaling_group" "bar" {
  name                      = "foobar3-terraform-test"
  
  max_size                  = 2
  min_size                  = 1
  desired_capacity          = 1

  health_check_grace_period = 300
  health_check_type         = "EC2"

  force_delete              = true

  vpc_zone_identifier       = var.private_subnet_ids

  tag {
    key                 = "Name"
    value               = "ecs-instance-${var.sufix}"
    propagate_at_launch = true
  }

  timeouts {
    delete = "15m"
  }
}

// Launch Template

resource "aws_launch_template" "ec2_task_executer_lt" {
  name = "ec2_task_executer_lt-${var.sufix}"
  ebs_optimized = true
  instance_type = "t3.micro"
  vpc_security_group_ids = [var.security_group_ecs_id]
  image_id = "ami-test"

  instance_market_options {
    market_type = "spot"
  }

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size = 20
    }
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
    instance_metadata_tags      = "enabled"
  }

  iam_instance_profile {
    name = var.ecs_instance_profile_name
  }

  monitoring {
    enabled = true
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "ecs-instance-${var.sufix}"
    }
  }

  user_data = base64encode("echo ECS_CLUSTER=cluster-${var.sufix} >> /etc/ecs/ecs.config")

}