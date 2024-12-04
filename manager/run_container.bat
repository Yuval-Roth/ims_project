call docker build -t imsproject_manager .
call docker container rm imsproject_manager
start docker run -p 80:80 --name imsproject_manager imsproject_manager
 