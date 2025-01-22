# Time-log

Service for saving time each second

# INFO
## Purpose
The main purpose is to demonstrate how we can create a simple web application for saving time each second to DB.

Application work description
This application save time and check db availability  by provided scheduled methods. 
Timelogs are started to log once after application started and continue work periodically (each second). 
Database is checked every 5 second

## Built With
* Java JDK (JDK 23)
* Maven
* Database Postgres
* Redis
* Docker

### Docker Compose support

This project contains a Docker Compose file named `docker-compose.yaml`.
In this file, the following services have been defined:

* postgres: [`postgres:latest`](https://hub.docker.com/_/postgres)
* redis: [`redis:latest`](https://hub.docker.com/_/redis)

### Testcontainers support

This project
uses [Testcontainers at development time](https://docs.spring.io/spring-boot/3.4.1/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`postgres:latest`](https://hub.docker.com/_/postgres)

### How to run app locally

Build postgres image from docker/postgres dir `docker build -t time-log-db .`
Build redis image from /docker/redis dir `docker build -t redis .`
Start db from scripts dir `start_db.sh`
Start redis from scripts dir `start_redis.sh`

### Create intellij configuration
Run app using intellij (command line is currently cumbersome)

### Building
Use the following command: `mvn clean install`

It will clean, check for dependencies conflict and finally run integration and unit tests.

To run the tests only, the command is:` mvn clean test`

### debugging local db
To access the db directly - useful to test queries in the raw data, or fix broken liquibase operations, do the
following:

* get the ID of the Postgresql container: `$ docker ps`
* get a shell inside container: `docker exec -it [DOCKER_CONTAINER_ID] /bin/bash`

### access the database:
for local profile `psql -U postgres log-time`
for test profile `psql -U postgres log-time-test`

# API Endpoints

## Logs
GET /api/v1/time_logs: Fetch all time logs