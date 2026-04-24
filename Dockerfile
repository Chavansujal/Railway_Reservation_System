FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . .

RUN javac -cp ".:mysql-connector-j-9.6.0.jar" web/backend/ApiServer.java

CMD ["java", "-cp", ".:mysql-connector-j-9.6.0.jar:web/backend", "ApiServer"]
