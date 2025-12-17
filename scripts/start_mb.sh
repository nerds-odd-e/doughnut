#!/bin/bash

# Start mountebank if not already running
# - Checks if port 2525 (mountebank default) is already in use
# - If running, skips starting
# - If not running, starts mountebank but continues even if it fails

MB_PORT=2525

# Check if mountebank is already running
if nc -z localhost $MB_PORT 2>/dev/null; then
    echo "Mountebank is already running on port $MB_PORT, skipping start"
    # Keep the script running to prevent run-p from completing
    while true; do sleep 3600; done
else
    echo "Starting mountebank on port $MB_PORT..."
    mb || {
        echo "Mountebank failed to start, continuing anyway"
        # Keep the script running to prevent run-p from killing other processes
        while true; do sleep 3600; done
    }
fi
