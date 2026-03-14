FROM amazoncorretto:21-alpine

WORKDIR /app

COPY booking-system-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]