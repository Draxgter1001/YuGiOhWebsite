#!/bin/bash
set -e

echo "----------------------------------------"
echo "Starting Yu-Gi-Oh Scanner Container"
echo "----------------------------------------"

# ==============================================================================
# 1. FIX DATABASE URL (Robust Parsing)
# ==============================================================================
if [ -n "$DATABASE_URL" ]; then
    echo "Detected Heroku DATABASE_URL. Parsing credentials..."

    # Use Python to parse the URL correctly into JDBC format + User + Pass
    eval $(python3 -c '
import os
from urllib.parse import urlparse

try:
    url = urlparse(os.environ["DATABASE_URL"])
    # Construct proper JDBC URL with SSL enabled (Required for Heroku)
    jdbc_url = f"jdbc:postgresql://{url.hostname}:{url.port}{url.path}?sslmode=require"

    print(f"export SPRING_DATASOURCE_URL=\"{jdbc_url}\"")
    print(f"export SPRING_DATASOURCE_USERNAME=\"{url.username}\"")
    print(f"export SPRING_DATASOURCE_PASSWORD=\"{url.password}\"")
except Exception as e:
    print("echo Error parsing DATABASE_URL")
')
else
    echo "No DATABASE_URL found. Assuming local development."
fi

# ==============================================================================
# 2. START PYTHON OCR
# ==============================================================================
echo "Starting Tesseract OCR Server..."
python3 scripts/ocr_server.py &
PYTHON_PID=$!
sleep 3

if ps -p $PYTHON_PID > /dev/null
then
   echo "Python OCR Server is running (PID: $PYTHON_PID)."
else
   echo "Error: Python OCR Server failed to start."
   exit 1
fi

echo "----------------------------------------"

# ==============================================================================
# 3. START JAVA
# ==============================================================================
echo "Starting Spring Boot Application..."

# -Xmx350m: We can now afford 350MB for Java because Tesseract is lightweight.
exec java -Xmx350m -XX:+UseSerialGC -jar app.jar