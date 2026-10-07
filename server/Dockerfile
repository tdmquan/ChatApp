# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/chat-server-*.jar app.jar
USER app
EXPOSE 8099
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
