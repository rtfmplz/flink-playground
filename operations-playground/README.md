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

## Monitoring

* [Flink Monitoring](http://localhost:8081)
* [Kafdrop](http://localhost:9000)
