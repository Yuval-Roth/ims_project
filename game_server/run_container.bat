call docker build -t imsproject_game_server .
call docker container rm imsproject_game_server
docker run -p 8640:8640 -p 8641:8641/udp --name imsproject_game_server imsproject_game_server
 