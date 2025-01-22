@echo off
set image_name=imsproject_manager

call docker build -t yuvalroth123/%image_name% .
call docker push yuvalroth123/%image_name%:latest
pause


