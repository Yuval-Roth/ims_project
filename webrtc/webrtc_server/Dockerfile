FROM openjdk:21

COPY target/webrtc_server-0.0.1-SNAPSHOT.jar /~/webrtc_server.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/~/webrtc_server.jar"]