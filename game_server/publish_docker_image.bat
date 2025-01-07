@echo off
set image_name=imsproject_game_server

call mvn clean package -DskipTests
call docker build -t yuvalroth123/%image_name% .
call docker push yuvalroth123/%image_name%:latest
pause


