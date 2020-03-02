## setup the Build target
FROM openjdk:8u131-jdk-alpine as Build

## build args required for coveralls reporting
ARG TRAVIS
ARG TRAVIS_JOB_ID

WORKDIR /app
RUN apk --no-cache add libstdc++

COPY gradle/wrapper ./gradle/wrapper
COPY gradlew ./
RUN ./gradlew --no-daemon --version

COPY *gradle* ./
COPY gradle/*.gradle ./gradle/
COPY .git ./.git/

## Build the application fat jar, invalidate only if the source changes
COPY src/main ./src/main
RUN ./gradlew --no-daemon shadowJar

## build the dropwizard client code
COPY swagger-config.json ./
RUN ./gradlew --no-daemon buildClient

## run the tests
COPY src/test ./src/test
RUN ./gradlew --no-daemon test

## run the static analysis
COPY codenarc.groovy ./
RUN ./gradlew --no-daemon check

## run code coverage report, send to coveralls when executing in Travis CI
RUN ./gradlew --no-daemon jacocoTestReport coveralls

## setup the Package target
FROM openjdk:8u131-jre-alpine as Package
WORKDIR /app
RUN apk --no-cache add libstdc++

## setup env var for the app name
ENV CRATEKUBE_APP cluster-mgmt-service

## add in files needed at runtime
COPY app.yml entrypoint.sh ./
COPY --from=Build /app/build/libs/${CRATEKUBE_APP}-*-all.jar /app/${CRATEKUBE_APP}.jar
ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["server"]

## setup the BintrayPublish target
FROM Build as BintrayPublish

## setup args needed for bintray tasks
ARG APP_VERSION
ARG BINTRAY_USER
ARG BINTRAY_KEY
ARG BINTRAY_PUBLISH

RUN ./gradlew --no-daemon bintrayUpload
