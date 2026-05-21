// S3 Bucket for Frontend Hosting
resource "aws_s3_bucket" "frontend_bucket" {
  bucket = "quickstack-frontend-${var.sufix}"
}

resource "aws_s3_bucket_public_access_block" "frontend_bucket_public_access_block" {
  bucket                  = aws_s3_bucket.frontend_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

// CloudFront Origin Access Control
resource "aws_cloudfront_origin_access_control" "oac" {
  name                              = "quickstack-oac-${var.sufix}"
  description                       = "OAC for QuickStack Frontend"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

// S3 Bucket Policy to allow CloudFront OAC
resource "aws_s3_bucket_policy" "frontend_bucket_policy" {
  bucket = aws_s3_bucket.frontend_bucket.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.frontend_bucket.arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.cdn.arn
          }
        }
      }
    ]
  })
}

// CloudFront Distribution
resource "aws_cloudfront_distribution" "cdn" {
  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"

  origin {
    domain_name              = aws_s3_bucket.frontend_bucket.bucket_regional_domain_name
    origin_id                = "S3Origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3Origin"

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  // Handle SPA routing
  custom_error_response {
    error_caching_min_ttl = 300
    error_code            = 403 // S3 returns 403 for missing files without ListBucket permission
    response_code         = 200
    response_page_path    = "/index.html"
  }
  custom_error_response {
    error_caching_min_ttl = 300
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "quickstack-cdn-${var.sufix}"
  }
}

// WAFv2 WebACL (Regional) for API Gateway
resource "aws_wafv2_web_acl" "api_waf" {
  name        = "quickstack-api-waf-${var.sufix}"
  description = "WAF for QuickStack API Gateway"
  scope       = "REGIONAL"

  default_action {
    allow {}
  }

  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 1

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "AWSManagedRulesCommonRuleSetMetric"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "quickstack-api-waf-metric"
    sampled_requests_enabled   = true
  }
}

// WAF Association with API Gateway
resource "aws_wafv2_web_acl_association" "api_waf_assoc" {
  resource_arn = var.api_gateway_arn
  web_acl_arn  = aws_wafv2_web_acl.api_waf.arn
}
