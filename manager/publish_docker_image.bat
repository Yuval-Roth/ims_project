call docker build -t yuvalroth123/imsproject_manager .
call docker tag imsproject_manager yuvalroth123/imsproject_manager:latest
call docker push yuvalroth123/imsproject_manager:latest

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


