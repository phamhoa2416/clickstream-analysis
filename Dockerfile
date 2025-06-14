FROM maven:3.8.6-openjdk-11-slim AS build
LABEL authors="phamviethoa"

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package

FROM openjdk:11-jre-slim

WORKDIR /app

COPY --from=build /app/target/clickstream-analysis-1.0-SNAPSHOT.jar ./app.jar
# COPY --from=build /app/src/main/resources/schema/clickstream.sql ./schema.sql

ENTRYPOINT ["java", "-jar", "app.jar"]
