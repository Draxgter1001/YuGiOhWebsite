#!/bin/bash
set -e

echo "----------------------------------------"
echo "Starting Yu-Gi-Oh Scanner Container"
echo "----------------------------------------"

# 1. Start the Python OCR Server in the background
echo "Starting Python OCR Server..."
# The virtual env path is already in $PATH from Dockerfile
python3 scripts/ocr_server.py &

# Save the PID of the python process
PYTHON_PID=$!

# Wait a few seconds to ensure Python starts (optional but safer)
sleep 5

# Check if Python is still running
if ps -p $PYTHON_PID > /dev/null
then
   echo "Python OCR Server is running (PID: $PYTHON_PID)."
else
   echo "Error: Python OCR Server failed to start."
   exit 1
fi

echo "----------------------------------------"

# 2. Start the Spring Boot Application
echo "Starting Spring Boot Application..."
# JAVA_OPTS can be used to pass memory limits from Heroku
exec java -jar app.jar