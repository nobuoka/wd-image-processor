FROM quay.io/nobuoka/wdip-app

COPY ./imageProcessors /cfg/imageProcessors
ENV PROCESSORS_CONFIG_PATH /cfg/imageProcessors/processors.json
# See : https://aws.amazon.com/blogs/compute/task-networking-in-aws-fargate/
ENV WD_BASE_URLS=http://127.0.0.1:9516
EXPOSE 8080
