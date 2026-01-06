# ==============================================================================
# STAGE 1: Build the Spring Boot Application
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
# Go offline to cache dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the JAR (skip tests to speed up build)
RUN mvn clean package -DskipTests

# ==============================================================================
# STAGE 2: Final Runtime Image (Java + Python)
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# ------------------------------------------------------------------------------
# Install Python and System Dependencies for EasyOCR/OpenCV
# ------------------------------------------------------------------------------
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    # Libraries required by OpenCV \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    && rm -rf /var/lib/apt/lists/*

# ------------------------------------------------------------------------------
# Setup Python Environment
# ------------------------------------------------------------------------------
# Create a virtual environment to avoid system package conflicts
ENV VIRTUAL_ENV=/app/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Copy requirements and install
COPY requirements.txt .
# Install dependencies (CPU versions of torch to save space)
RUN pip install --no-cache-dir -r requirements.txt

# ------------------------------------------------------------------------------
# Setup Application
# ------------------------------------------------------------------------------
# Copy the built JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Copy the Python OCR script
# We place it in a known location so entrypoint can find it
COPY src/main/resources/scripts/ocr_server.py ./scripts/ocr_server.py

# Copy the entrypoint script
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

# Create a temp directory for file uploads
RUN mkdir -p /tmp/yugioh-uploads

# ------------------------------------------------------------------------------
# Run
# ------------------------------------------------------------------------------
# Heroku runs as a non-root user, so we don't switch users explicitly here
# as the base image usually handles it, but we ensure permissions are open if needed.

CMD ["./entrypoint.sh"]