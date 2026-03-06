FROM eclipse-temurin:23-jre
WORKDIR /app
COPY target/JettyServer-1.0-SNAPSHOT.jar .
EXPOSE 8080
CMD ["java", "-jar", "JettyServer-1.0-SNAPSHOT.jar"]
