FROM openjdk:17-jdk-alpine


RUN apk update && apk add --no-cache mysql mysql-client bash

ENV MYSQL_ROOT_PASSWORD=root@password
ENV MYSQL_DATABASE=rule_engine_db
ENV MYSQL_USER=user123
ENV MYSQL_PASSWORD=root@password

WORKDIR /app

#Uncomment this if you are building the project yourself and comment or remove the line below it
#COPY target/rule-engine-0.0.1-SNAPSHOT.jar app.jar
COPY rule-engine-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 3306
EXPOSE 8080


CMD ["sh", "-c", "mysqld & while ! mysqladmin ping -h localhost --silent; do sleep 1; done && java -jar app.jar --spring.profiles.active=docker"]
