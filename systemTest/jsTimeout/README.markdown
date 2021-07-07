JS timeout test
==========

## How to run

```
# Build test target application
cd ../../; ./gradlew installDist; cd systemTest/jsTimeout
docker-compose build

# Build Rust script
docker build -t wdip-js-timeout-test test

# Execute test
docker-compose up -d
docker run --network=host -it --rm wdip-js-timeout-test
docker-compose down
```
