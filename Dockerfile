FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy executable jar
COPY vetautet-start/target/vetautet-start-1.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 1122

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
