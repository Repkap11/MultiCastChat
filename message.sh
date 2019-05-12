#!/bin/bash
echo -n "$@" | socat - udp-datagram:$(ip address show $(ip addr | awk '/state UP/ {print $2}' | sed 's/.$//') | grep 'inet .* brd ' | head -1 | sed -e 's/^.* brd \([0-9\.]*\) .*$/\1/'):56789,broadcast
