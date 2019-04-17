#!/bin/bash
rm -Rf ./minecraftbattleroyale-world-master master.zip
wget https://github.com/minecraftbattleroyale/minecraftbattleroyale-world/archive/master.zip
unzip master.zip
exec "$@"
