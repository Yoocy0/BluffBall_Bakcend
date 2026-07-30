#!/bin/bash
set -e
pkill -f '/home/ubuntu/bluffball.jar' || true
sleep 2
nohup java -jar /home/ubuntu/bluffball.jar > /home/ubuntu/bluffball.log 2>&1 &
disown
sleep 2
pgrep -af bluffball.jar || echo NO_PROCESS
echo RESTART_DONE
