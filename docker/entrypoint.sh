#!/bin/bash
wget https://github.com/minecraftbattleroyale/minecraftbattleroyale-world/archive/master.zip
unzip master.zip
#rm ./minecraftbattleroyale-world-master/level.dat
exec "$@"
