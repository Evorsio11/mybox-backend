# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
    && JAR_PATH=$(find target -maxdepth 1 -type f -name "*.jar" ! -name "*original" | head -n 1) \
    && cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/app.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
