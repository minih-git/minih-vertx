FROM minihdocker/minih-jre

ENV JAVA_OPTS="-Xms512M -Xmx512M -XX:+UseZGC -XX:MaxGCPauseMillis=50"
ENV APP_HOME=/app

WORKDIR $APP_HOME

# Assuming the use of 'installDist' task from Gradle Application plugin
# which creates a script in bin/ and libraries in lib/
COPY build/install/minih-system-service $APP_HOME

EXPOSE 8081

# The start script usually has the same name as the project
CMD ["./bin/minih-system-service"]
