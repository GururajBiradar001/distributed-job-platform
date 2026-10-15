FROM maven:3.9-eclipse-temurin-24 AS build

WORKDIR /app

COPY job-service/pom.xml ./job-service/pom.xml
COPY pom.xml ./pom.xml
COPY job-service/src ./job-service/src

RUN mvn -f job-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:24-jre

WORKDIR /app

COPY --from=build /app/job-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]