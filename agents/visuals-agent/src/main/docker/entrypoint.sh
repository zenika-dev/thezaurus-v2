#!/bin/sh
set -eu

# Compose starts this small bootstrap as root solely to read a private host ADC.
# The copy lives in tmpfs; neither the image nor a persistent volume contains it.
if [ "$(id -u)" = 0 ]; then
    if [ -n "${VISUALS_ADC_SOURCE:-}" ] && [ -f "$VISUALS_ADC_SOURCE" ]; then
        install -d -m 0700 -o 10001 -g 10001 /run/visuals
        install -m 0400 -o 10001 -g 10001 "$VISUALS_ADC_SOURCE" /run/visuals/credentials.json
    fi
    exec gosu 10001:10001 "$@"
fi
exec "$@"
