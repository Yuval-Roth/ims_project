call mvn package -DskipTests
call docker build -t webrtc_server .
call docker container rm webrtc_server
start docker run -p 8080:8080 --name webrtc_server webrtc_server
 