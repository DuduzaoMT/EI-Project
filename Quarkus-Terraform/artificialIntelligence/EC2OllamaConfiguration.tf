terraform {
  required_version = ">= 1.0.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region      = "us-east-1"
}

resource "aws_instance" "exampleDeployQuarkus" {
  ami = "ami-0eb38b817b93460ac"
  instance_type = "t3.large"
  vpc_security_group_ids = [aws_security_group.instance.id]

  root_block_device {
    volume_size = 50 # In Gb
  }

  key_name = "vockey"
  user_data = "${file("quarkus.sh")}"
  user_data_replace_on_change = true

  tags = {
    Name = "terraform-example-ollama"
  }
}

resource "aws_security_group" "instance" {
  name = var.security_group_name
  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = 8080
    to_port = 8080
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = 11434
    to_port = 11434
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

variable "security_group_name" {
  description = "The name of the security group"
  type = string
  default = "terraform-ollama-example-instance"
}

output "address" {
  value = aws_instance.exampleDeployQuarkus[*].public_dns
  description = "Address of the Kafka EC2 machine with ollama"
}
