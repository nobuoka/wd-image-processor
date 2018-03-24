# Use openjdk:8 because openjdk:9 has SSL issue
# See : https://github.com/docker-library/openjdk/issues/145
FROM openjdk:8 AS build-env

RUN mkdir -p /app
WORKDIR /app

ADD ./gradle/ ./gradle/
ADD ./gradlew ./gradlew
RUN chmod +x ./gradlew && ./gradlew -v

ADD ./settings.gradle.kts ./settings.gradle.kts
ADD ./build.gradle.kts ./build.gradle.kts
RUN ./gradlew dependencies

ADD ./ ./
RUN ./gradlew installDist

FROM openjdk:8

COPY --from=build-env /app/build/install/app /app

ENV PATH $PATH:/app/bin
CMD ["app"]
