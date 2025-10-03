# Kafka Worker

A simple Kafka consumer/producer worker that listens to the `ocr` topic and sends processed messages to the `result` topic.

## Functionality

1. **Consumes** messages from the `ocr` topic

   - Expected message format: `{ "fileKey": "string" }`

2. **Logs** the received message

3. **Acknowledges** the message (manual commit)

4. **Produces** a response to the `result` topic
   - Output message format: `{ "metadata1": "TEST" }`

## Configuration

The worker is configured via environment variables:

- `BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `localhost:9092`)
- `GROUP_ID`: Consumer group ID (default: `demo-consumer-group`)
- `TOPIC`: Input topic to consume from (default: `ocr`)
- `OUTPUT_TOPIC`: Output topic to produce to (default: `result`)
- `AUTO_OFFSET_RESET`: Offset reset strategy (default: `earliest`)
- `ENABLE_AUTO_COMMIT`: Auto commit enabled (default: `false`)
- `POLL_MS`: Poll duration in milliseconds (default: `1000`)

## Running with Docker Compose

### Single worker instance

```bash
docker compose up --build
```

### Multiple worker instances (scaled)

```bash
docker compose up --build --scale worker=3
```

This will start 3 worker instances that will share the load from the `ocr` topic.

## Local Development

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/worker-1.0.0.jar
```

Make sure Kafka is running locally or set the `BOOTSTRAP_SERVERS` environment variable to point to your Kafka cluster.
