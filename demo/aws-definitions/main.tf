# 参考にした : https://github.com/yukkyun/terraform-ecs-sample

variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "aws_region" {}
variable "vpc_cidr" {
  description = "CIDR for the whole VPC"
  default = "10.0.0.0/16"
}
variable "public_subnet_cidr" {
  description = "CIDR for the Public Subnet"
  default = "10.0.0.0/24"
}

provider "aws" {
  version = "~> 1.17"
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
  region = "${var.aws_region}"
}
provider "template" {
  version = "~> 1.0"
}

resource "aws_cloudwatch_log_group" "wdip_demo" {
  name = "/ecs/wdip-demo"
  retention_in_days = 1
}

# Log driver : https://docs.aws.amazon.com/AmazonECS/latest/developerguide/using_awslogs.html

# See : https://www.terraform.io/docs/providers/aws/d/iam_policy_document.html
data "aws_iam_policy_document" "task_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}
resource "aws_iam_role" "task_role" {
  name = "wdip-demo-ecs-task-role"
  assume_role_policy = "${data.aws_iam_policy_document.task_role_policy.json}"
}
resource "aws_iam_role_policy_attachment" "task_attach" {
  role = "${aws_iam_role.task_role.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# See : https://www.terraform.io/docs/providers/template/d/file.html
data "template_file" "task_definitions_wdip_demo" {
  template = "${file("task-definitions/wdip-demo.json")}"

  vars {
    log_group = "${aws_cloudwatch_log_group.wdip_demo.name}"
    region = "${var.aws_region}"
  }
}

# AWS ECS task definitions
# https://www.terraform.io/docs/providers/aws/r/ecs_task_definition.html
resource "aws_ecs_task_definition" "wdip_demo" {
  # A unique name for task definition.
  family = "wdip-demo"
  requires_compatibilities = ["FARGATE"]
  execution_role_arn = "${aws_iam_role.task_role.arn}"
  container_definitions = "${data.template_file.task_definitions_wdip_demo.rendered}"
  network_mode = "awsvpc"
  cpu = "256"
  memory = "512"

  # ログ設定
  # ログドライバー:: awslogs
  # キー	値
  # awslogs-group	/ecs/wdip-demo
  # awslogs-region	us-east-1
  # awslogs-stream-prefix	ecs
}

resource "aws_vpc" "wdip_demo" {
  cidr_block = "${var.vpc_cidr}"
  enable_dns_hostnames = true
  tags {
    Name = "wdip-demo-vpc"
  }
}

resource "aws_internet_gateway" "default" {
  vpc_id = "${aws_vpc.wdip_demo.id}"
}

resource "aws_subnet" "public_subnet" {
  vpc_id = "${aws_vpc.wdip_demo.id}"
  cidr_block = "${var.public_subnet_cidr}"
  #availability_zone = "eu-west-1a"
  tags {
    Name = "Public Subnet for wdip-demo"
  }
}
resource "aws_route_table" "public_subnet" {
  vpc_id = "${aws_vpc.wdip_demo.id}"
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = "${aws_internet_gateway.default.id}"
  }
  tags {
    Name = "Public Subnet for wdip-demo"
  }
}
resource "aws_route_table_association" "public_subnet" {
  subnet_id = "${aws_subnet.public_subnet.id}"
  route_table_id = "${aws_route_table.public_subnet.id}"
}
