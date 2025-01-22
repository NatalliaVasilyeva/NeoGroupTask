# Use official Java runtime as a parent image
FROM openjdk:23-jdk-slim-bullseye

# Set working directory in the container
WORKDIR /time-log

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve

COPY src ./src

CMD ["./mvnw", "spring-boot:run"]
EXPOSE 8080