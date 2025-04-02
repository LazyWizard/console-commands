@echo off
if exist "../docs/" echo Deleting existing documentation... && rd /S /Q "../docs/"
java -jar dokka-cli-2.0.0.jar dokka-config.json
pause