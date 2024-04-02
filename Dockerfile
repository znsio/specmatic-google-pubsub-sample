# Use an official Google Cloud SDK image as the base
FROM google/cloud-sdk:latest

# Install the Pub/Sub emulator using apt-get
RUN apt-get install google-cloud-sdk-pubsub-emulator -y

# Set the environment variable for the Pub/Sub emulator's port
ENV PUBSUB_EMULATOR_HOST=localhost:8085

# Expose the port the emulator runs on
EXPOSE 8085

# Copy the initialization script into the container
COPY start-pubsub-emulator.sh /start-pubsub-emulator.sh

# Make the script executable
RUN chmod +x /start-pubsub-emulator.sh

# Run the Pub/Sub emulator
ENTRYPOINT ["/start-pubsub-emulator.sh"]