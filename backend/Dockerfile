FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -B -DskipTests dependency:go-offline

COPY backend/ backend/
COPY database/ database/
RUN mvn -f backend/pom.xml -B -DskipTests package

FROM eclipse-temurin:25-jre-noble

RUN groupadd --system tutor && useradd --system --gid tutor --home-dir /app tutor
WORKDIR /app
COPY --from=build /workspace/backend/target/tutor-inteligente-backend-1.0.0.jar app.jar
RUN chown tutor:tutor /app/app.jar

USER tutor
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
