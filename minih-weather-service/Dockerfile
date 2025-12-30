FROM minihdocker/minih-jre

ENV JAVA_OPTS="-Xms512M -Xmx512M -XX:+UseZGC -XX:MaxGCPauseMillis=50"
ENV APP_HOME=/app

WORKDIR $APP_HOME

COPY build/install/minih-weather-service $APP_HOME

EXPOSE 8082

CMD ["./bin/minih-weather-service"]
