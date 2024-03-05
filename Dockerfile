FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy
LABEL authors="mxr"
WORKDIR /app
COPY target/scala-3.3.1/http4s-learn-assembly-0.1.0-SNAPSHOT.jar ./app.jar
CMD ["java", "-jar", "app.jar"]