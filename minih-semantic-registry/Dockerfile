FROM minihdocker/minih-jre

ENV JAVA_OPTS="-Xms512M -Xmx512M -XX:+UseZGC -XX:MaxGCPauseMillis=50"
ENV APP_HOME=/app

WORKDIR $APP_HOME

COPY build/install/minih-semantic-registry $APP_HOME

EXPOSE 8099

CMD ["./bin/minih-semantic-registry"]
