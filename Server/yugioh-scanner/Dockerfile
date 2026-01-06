# ==============================================================================
# STAGE 1: Build the Spring Boot Application
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ==============================================================================
# STAGE 2: Final Runtime Image
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 1. Install Python, Tesseract OCR, and dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    tesseract-ocr \
    libtesseract-dev \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# 2. Setup Python Environment
ENV VIRTUAL_ENV=/app/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# 3. Install Python Libraries
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. Setup Application Files
COPY --from=build /app/target/*.jar app.jar
COPY src/main/resources/scripts/ocr_server.py ./scripts/ocr_server.py
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh
RUN mkdir -p /tmp/yugioh-uploads

CMD ["./entrypoint.sh"]