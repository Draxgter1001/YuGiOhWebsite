#!/bin/bash
set -e

echo "----------------------------------------"
echo "Starting Yu-Gi-Oh Scanner (Pure Java)"
echo "----------------------------------------"

# ==============================================================================
# 1. FIX DATABASE URL (Bash Version)
# ==============================================================================
# Since we removed Python from this image to save space, we parse the URL using Bash.
if [ -n "$DATABASE_URL" ]; then
    echo "Detected Heroku DATABASE_URL. Converting to JDBC format..."

    # Format: postgres://user:password@host:port/dbname

    # Remove 'postgres://' prefix
    clean_url=${DATABASE_URL#"postgres://"}

    # Extract user:password string
    user_pass=${clean_url%%@*}

    # Extract user and password
    user=${user_pass%%:*}
    pass=${user_pass#*:}

    # Extract host:port/dbname string
    host_db=${clean_url#*@}

    # Extract host:port and dbname
    host_port=${host_db%%/*}
    dbname=${host_db#*/}

    # Export variables for Spring Boot
    export SPRING_DATASOURCE_URL="jdbc:postgresql://${host_port}/${dbname}?sslmode=require"
    export SPRING_DATASOURCE_USERNAME="${user}"
    export SPRING_DATASOURCE_PASSWORD="${pass}"

    echo "Database Configured successfully."
else
    echo "No DATABASE_URL found. Assuming local development."
fi

# ==============================================================================
# 2. START JAVA
# ==============================================================================
echo "Starting Spring Boot Application..."

# -Xmx400m: We can now give almost all 512MB RAM to Java because
# there is no Python OCR server running alongside it.
exec java -Xmx400m -XX:+UseSerialGC -jar app.jar