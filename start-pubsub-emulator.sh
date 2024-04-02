#!/bin/bash

# Start the Pub/Sub emulator in the background
gcloud beta emulators pubsub start --host-port=0.0.0.0:8085 &
EMULATOR_PID=$!

# Wait a bit for the emulator to be fully up and running
sleep 5

# Set the environment variable for the Pub/Sub emulator endpoint
export PUBSUB_EMULATOR_HOST=localhost:8085

# Function to create a topic via REST API
create_topic() {
    local project=$1
    local topic=$2
    curl -X PUT http://localhost:8085/v1/projects/${project}/topics/${topic}
}

# Create the topics
create_topic "pub-sub-demo-414308" "place-order"
create_topic "pub-sub-demo-414308" "process-order"
create_topic "pub-sub-demo-414308" "notification"

# Keep the script running to not terminate the Docker container
wait $EMULATOR_PID
