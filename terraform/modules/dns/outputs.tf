output "zone_id" {
  description = "The Hosted Zone ID of the domain"
  value       = aws_route53_zone.main.zone_id
}

output "name_servers" {
  description = "Name servers for the Hosted Zone"
  value       = aws_route53_zone.main.name_servers
}
