FROM store/oracle/serverjre:8

WORKDIR  /opt/minecraft

# Copy the defaults into the root of the folder
COPY ./docker/ .

# Download the Sponge and Minecraft jar
ENV LAUNCH_VERSION=1.12
ENV MC_VERSION=1.12.2
ENV SPONGE_VERSION=${MC_VERSION}-7.1.5
ADD https://libraries.minecraft.net/net/minecraft/launchwrapper/${LAUNCH_VERSION}/launchwrapper-${LAUNCH_VERSION}.jar libraries/net/minecraft/launchwrapper/${LAUNCH_VERSION}/launchwrapper-${LAUNCH_VERSION}.jar
ADD https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/${SPONGE_VERSION}/spongevanilla-${SPONGE_VERSION}.jar spongevanilla.jar
ADD https://s3.amazonaws.com/Minecraft.Download/versions/${MC_VERSION}/minecraft_server.${MC_VERSION}.jar minecraft_server.${MC_VERSION}.jar

EXPOSE 25565
CMD ["java", "-jar", "spongevanilla.jar"]
