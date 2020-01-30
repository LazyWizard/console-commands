#!/bin/bash
cd ~/Code/Console/
echo Waiting 5 seconds to ensure remote is up-to-date...
sleep 5

echo Updating repo...
hg pull --update
AUTHTOKEN=$(<authtoken)
BUILD=$(hg id -n)
BUILD=${BUILD:0:-1}
COMMIT=$(hg id -i)
COMMIT=${COMMIT:0:-1}
echo Generating dev for build number $BUILD, commit $COMMIT...

echo Building mod archive...
./gradlew zipMod -PbuildNumber=$BUILD

if [ $? -ne 0 ]; then
	BUILD_STATUS="{\"key\": \"dev\", \"state\": \"FAILED\", \"name\": \"Console Dev\", \"url\": \"https://bitbucket.org/LazyWizard/console-commands/\"}"
	echo Build failed! Publishing failure status...
	curl -sS -o /dev/null --fail -H "Content-Type: application/json" -X POST -d "${BUILD_STATUS}" "https://${AUTHTOKEN}@api.bitbucket.org/2.0/repositories/LazyWizard/console-commands/commit/${COMMIT}/statuses/build"
	read -p "Error! Press enter to exit."
else
	cd build/zip
	# There should only be one file - may want to revisit for safety's sake later on
	for file in *
	do
		echo Uploading $file...
		curl -sS -o /dev/null --fail -X POST "https://${AUTHTOKEN}@api.bitbucket.org/2.0/repositories/LazyWizard/console-commands/downloads" --form files=@"$file"
		url=$(urlencode -m $file)
		BUILD_STATUS="{\"key\": \"dev\", \"state\": \"SUCCESSFUL\", \"name\": \"Console Dev\", \"url\": \"https://bitbucket.org/LazyWizard/console-commands/downloads/${url}\"}"
		echo Publishing $file as $url...
		curl -sS -o /dev/null --fail -H "Content-Type: application/json" -X POST -d "${BUILD_STATUS}" "https://${AUTHTOKEN}@api.bitbucket.org/2.0/repositories/LazyWizard/console-commands/commit/${COMMIT}/statuses/build"
	done

	cd ../..
	read -p "Finished! Press enter to exit."
fi