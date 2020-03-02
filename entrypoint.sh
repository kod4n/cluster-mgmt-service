#!/usr/bin/env sh

dropwizard() {
  java -jar /app/${CRATEKUBE_APP}.jar "$@" app.yml
}

for command in "$@"
do
  case "$command" in
    status)
      dropwizard "db" "status"
      ;;
    migrate)
      dropwizard "db" "migrate"
      ;;
    server)
      dropwizard "server"
      ;;
  esac
done