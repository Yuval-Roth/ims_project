call mvn package -DskipTests
call docker build -t game_server .
call docker container rm game_server
start docker run -p 8640:8640 -p 8641:8641/udp --name game_server game_server
 