# syntax=docker/dockerfile:1

# ---- Build stage: compiles the jar with the Maven Wrapper ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy only what is needed to download dependencies (better layer caching)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /workspace/target/*.jar /app/app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
