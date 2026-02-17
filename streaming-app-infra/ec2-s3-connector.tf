resource "aws_ecr_repository" "page_view_sink" {
  name                 = "page-view-sink"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_s3_bucket" "page_view_sink" {
  bucket = "${var.environment}-page-view-sink-output"
}

resource "aws_s3_bucket" "page_view_raw" {
  bucket = "${var.environment}-page-view-raw-output"
}

resource "aws_iam_role" "s3_connector_role" {
  name = "${var.environment}-page-view-sink-ec2-role"

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

resource "aws_iam_role_policy" "s3_connector_ecr_access" {
  name = "ecr-access"
  role = aws_iam_role.s3_connector_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchCheckLayerAvailability"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy" "s3_connector_cloudwatch_logs" {
  name = "cloudwatch-logs"
  role = aws_iam_role.s3_connector_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

resource "aws_iam_role_policy" "s3_connector_s3_access" {
  name = "s3-sink-access"
  role = aws_iam_role.s3_connector_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:GetBucketLocation"
        ]
        Resource = [
          aws_s3_bucket.page_view_sink.arn,
          "${aws_s3_bucket.page_view_sink.arn}/*",
          aws_s3_bucket.page_view_raw.arn,
          "${aws_s3_bucket.page_view_raw.arn}/*"
        ]
      }
    ]
  })
}

resource "aws_security_group" "s3_connector_sg" {
  name        = "${var.environment}-page-view-sink-sg"
  description = "Security group for page-view-sink EC2 instance"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Kafka Connect REST API"
    from_port   = 8083
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_iam_instance_profile" "s3_connector_profile" {
  name = "${var.environment}-page-view-sink-ec2-profile"
  role = aws_iam_role.s3_connector_role.name
}

resource "aws_instance" "page_view_sink" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = "t3.small"
  iam_instance_profile        = aws_iam_instance_profile.s3_connector_profile.name
  vpc_security_group_ids      = [aws_security_group.s3_connector_sg.id]
  key_name                    = var.ec2_key_pair_name
  associate_public_ip_address = true

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  tags = {
    Name        = "${var.environment}-page-view-sink"
    Environment = var.environment
  }
}