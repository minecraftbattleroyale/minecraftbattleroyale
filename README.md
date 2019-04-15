# Minecraft Battle Royale

Soon TM

# Building and Running

This project uses Docker to create the Minecraft server,
the `docker/` folder are files that are used to create the server.
The `run/` folder are auto generated files from the Docker image to aid in changing things.

The project needs to be built first or there will be issues loading the plugin.
Then you can build the docker image.

> ./gradlew
> docker-compose build
