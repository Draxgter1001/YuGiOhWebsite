# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Run (Java Only - No Python)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh
RUN mkdir -p /tmp/yugioh-uploads

CMD ["./entrypoint.sh"]