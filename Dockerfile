FROM openjdk:17
WORKDIR /app
COPY . .
EXPOSE 8080
CMD ["java", "-jar", "build/libs/techTest-0.0.1-SNAPSHOT.jar"]