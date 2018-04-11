#!/usr/bin/env bash
set -o nounset                                  # Treat unset variables as an error
function error_exit
{
    echo "$1" 1>&2
    exit 1
}
PROJECT_ID=`gcloud config get-value project`
FB_PROJECT_ID=`firebase list |grep -i  $PROJECT_ID | awk 'BEGIN { FS="│" } { printf $3 }' | sed 's/ //g'`
if [ -z "$FB_PROJECT_ID" ]; then
 echo Firebase Project  Not Found!
 exit
fi

#gcloud beta pubsub topics create banias-control --project=$PROJECT_ID --quiet >/dev/null || error_exit "Error creating Pub/Sub topics"
cd firebase
firebase use --add $PROJECT_ID
DB=$PROJECT_ID-banias
firebase target:apply database banias $DB
firebase deploy
firebase database:set /schemas --data '{"event_name": "click","event_version": 1, "payload":"xx"}' --project aviv-playground-60b73
cd ..