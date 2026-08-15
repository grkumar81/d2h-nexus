@echo off
SET JAVA_HOME=D:\Java\jdk25.0.4_7
SET PATH=%JAVA_HOME%\bin;%PATH%
call mvnw.cmd %*
