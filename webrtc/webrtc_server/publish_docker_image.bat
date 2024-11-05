call mvn clean package -DskipTests
call docker build -t yuvalroth123/webrtc_server .
call docker tag webrtc_server yuvalroth123/webrtc_server:latest
call docker push yuvalroth123/webrtc_server:latest

@echo off
echo:
echo:
echo:
echo ================================================================================
echo only yuval can push to this docker repository, so if it didn't work you know why
echo ================================================================================
echo:
echo:
echo:
pause


