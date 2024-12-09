FROM openjdk:17

COPY target/game_server-0.0.1-SNAPSHOT.jar /game_server.jar
EXPOSE 8640
EXPOSE 8641/udp

ENTRYPOINT ["java", "-jar", "/game_server.jar"]