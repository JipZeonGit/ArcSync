#!/usr/bin/env sh

# Simplified Gradle wrapper script

APP_BASE_NAME=${0##*/}
APP_HOME=$(cd "${0%/*}" && pwd -P)

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar:$APP_HOME/gradle/wrapper/gradle-cli.jar"

if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "gradle-wrapper.jar is missing. Please run with a full Gradle distribution or add the wrapper jar." >&2
  exit 1
fi

exec java ${JAVA_OPTS:-} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
