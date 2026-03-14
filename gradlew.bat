@echo off
setlocal
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar;%APP_HOME%\gradle\wrapper\gradle-wrapper-shared.jar;%APP_HOME%\gradle\wrapper\gradle-cli.jar

if not exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" (
  echo gradle-wrapper.jar is missing. Please run with a full Gradle distribution or add the wrapper jar.
  exit /b 1
)

"%JAVA_HOME%\bin\java.exe" %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
