FROM ubuntu:24.04

RUN apt update && apt install -y openjdk-21-jre-headless

COPY target/game_server-0.0.1-SNAPSHOT.jar /app/game_server.jar

EXPOSE 8640
EXPOSE 8641/udp

ENTRYPOINT ["java", "-jar", "/app/game_server.jar"]