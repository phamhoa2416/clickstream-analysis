#!/bin/bash
# endpoint.sh

CURRENT_IP=$(ip route get 1.1.1.1 | grep -oP 'src \K[^ ]+')
CONFIG_FILE="/etc/nginx/"

sed -i "s/proxy_pass http:\/\/.*:8080/proxy_pass http:\/\/$CURRENT_IP:8080/"