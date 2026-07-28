# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN addgroup --system forge && adduser --system --ingroup forge forge
COPY --from=build /app/target/forge-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R forge:forge /app
USER forge
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
