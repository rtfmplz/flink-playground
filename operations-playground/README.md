# Flink Operations Playground

It's based on a [docker-compose](https://docs.docker.com/compose/) environment and is super easy to setup.

## Setup

The operations playground requires a custom Docker image, as well as public images for Flink, Kafka, and ZooKeeper.

The `docker-compose.yaml` file of the operations playground is located in the `operations-playground` directory. Assuming you are at the root directory of the [`flink-playgrounds`](https://github.com/apache/flink-playgrounds) repository, change to the `operations-playground` folder by running

```bash
cd operations-playground
```

### Building the custom Docker image

Build the Docker image by running

```bash
docker-compose build
```

### Starting the Playground

Once you built the Docker image, run the following command to start the playground

```bash
docker-compose up -d
```

### Stopping the Playground

To stop the playground, run the following command

```bash
docker-compose down
```

## Play with Flink Applications

### Create Application

* develop

아래 경로 하위에 maven project를 만들어서 개발

```bash
~/git/flink-playground/docker/ops-playground-image/java
```

* build

```bash
mvn clean install
```

* copy application

아래 경로에 jar 파일을 복사

```bash
~/git/flink-playground/operations-playground/apps
```

* Run flink application

client container에 접속해서 `/opt/apps` 하위의 jar를 flink로 실행

```bash
docker exec -it operations-playground_client_1 /bin/bash
flink run -d -p 2 /opt/apps/flink-playground-async-db-1-FLINK-1.10_2.11.jar --bootstrap.servers kafka:9092 --checkpointing --event-time
flink run -d -p 2 /opt/apps/flink-playground-clickcountjob-1-FLINK-1.10_2.11.jar --bootstrap.servers kafka:9092 --checkpointing --event-time
flink run -d -p 2 /opt/apps/flink-playground-example01-1-FLINK-1.10_2.11.jar
```

## Monitoring

* [Flink Monitoring](http://localhost:8081)
* [Kafdrop](http://localhost:9000)