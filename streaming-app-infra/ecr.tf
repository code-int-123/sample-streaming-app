resource "aws_ecr_repository" "page_view_aggregator" {
  name                 = "page-view-aggregator"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}