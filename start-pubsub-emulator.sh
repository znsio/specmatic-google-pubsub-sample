#!/bin/bash

# Start the Pub/Sub emulator in the background
gcloud beta emulators pubsub start --host-port=0.0.0.0:8085 &
EMULATOR_PID=$!

# Wait for the emulator to start
sleep 5

# Set the environment variable for the Pub/Sub emulator endpoint
export PUBSUB_EMULATOR_HOST=localhost:8085

# Create the topics
curl -X PUT http://localhost:8085/v1/projects/pub-sub-demo-414308/topics/place-order
curl -X PUT http://localhost:8085/v1/projects/pub-sub-demo-414308/topics/process-order
curl -X PUT http://localhost:8085/v1/projects/pub-sub-demo-414308/topics/notification

# Keep the script running to not terminate the Docker container
wait $EMULATOR_PID