FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /tmp/kafka-streams

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]