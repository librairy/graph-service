FROM anapsix/alpine-java:8u162b12_server-jre
VOLUME /tmp
ADD graph-service.jar /app.jar
ADD bin /bin
ENTRYPOINT exec java $JAVA_OPTS -server -jar /app.jar
