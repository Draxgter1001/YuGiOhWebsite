#!/bin/bash
set -e

echo "----------------------------------------"
echo "Starting Yu-Gi-Oh Scanner Container"
echo "----------------------------------------"

# ==============================================================================
# 1. FIX DATABASE URL
# ==============================================================================
# Heroku provides DATABASE_URL as "postgres://...", but JDBC needs "jdbc:postgresql://..."
# We create a new env var SPRING_DATASOURCE_URL that Spring Boot picks up automatically.
if [ -n "$DATABASE_URL" ]; then
    echo "Detected Heroku DATABASE_URL. converting to JDBC format..."
    # Replace 'postgres://' with 'jdbc:postgresql://'
    export SPRING_DATASOURCE_URL="${DATABASE_URL/postgres:\/\//jdbc:postgresql:\/\/}"
else
    echo "No DATABASE_URL found. Assuming local development or POSTGRES_DATABASE_URL is set."
fi

# ==============================================================================
# 2. START PYTHON OCR
# ==============================================================================
echo "Starting Python OCR Server..."
python3 scripts/ocr_server.py &
PYTHON_PID=$!
sleep 5

if ps -p $PYTHON_PID > /dev/null
then
   echo "Python OCR Server is running (PID: $PYTHON_PID)."
else
   echo "Error: Python OCR Server failed to start."
   exit 1
fi

echo "----------------------------------------"

# ==============================================================================
# 3. START JAVA (With Memory Limits)
# ==============================================================================
echo "Starting Spring Boot Application..."

# -Xmx300m: Limits Java Heap to 300MB (prevents Error R14 crashes)
exec java -Xmx300m -XX:+UseSerialGC -jar app.jar