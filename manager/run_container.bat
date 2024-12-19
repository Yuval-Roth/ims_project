call docker build -t imsproject_manager .
call docker container rm imsproject_manager
docker run -p 5000:5000 --name imsproject_manager imsproject_manager
 