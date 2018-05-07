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

resource "aws_security_group" "wdip_app" {
  name        = "wdip-demo"
  description = "Allow tcp 8080 inbound traffic"
  vpc_id      = "${aws_vpc.wdip_demo.id}"

  ingress {
    protocol = "tcp"
    from_port = 8080
    to_port = 8080
  }
}

resource "aws_ecs_cluster" "ecs_cluster" {
  name = "wdip-demo"
}

resource "aws_ecs_service" "wdip_demo" {
  name = "wdip-demo"
  cluster = "${aws_ecs_cluster.ecs_cluster.id}"
  task_definition = "${aws_ecs_task_definition.wdip_demo.arn}"
  desired_count = 1
  launch_type = "FARGATE"

  network_configuration {
    subnets = ["${aws_subnet.public_subnet.*.id}"]
    security_groups = [
      "${aws_security_group.wdip_app.id}"
    ]
  }
}
