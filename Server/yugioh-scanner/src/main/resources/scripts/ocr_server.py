import os
import sys
import easyocr
import logging
from flask import Flask, request, jsonify
# Import waitress for production serving
from waitress import serve

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Load model on startup
logger.info("Loading EasyOCR Model...")
try:
    # gpu=False for Heroku (unless you have a GPU dyno, which is rare/expensive)
    reader = easyocr.Reader(['en'], gpu=False)
    logger.info("OCR Model Loaded Successfully.")
except Exception as e:
    logger.error(f"Failed to load OCR model: {e}")
    sys.exit(1)

def clean_name_for_api(card_name):
    if not card_name: return None
    import re
    # Normalize text
    cleaned = card_name.replace('|', 'I').replace('0', 'O')
    cleaned = ' '.join(cleaned.split())
    cleaned = cleaned.replace('"', '').replace("'", "")
    cleaned = re.sub(r'[^\w\s\-\']', '', cleaned)

    words = cleaned.split()
    capitalized_words = []
    small_words = {'the', 'of', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'from', 'with', 'by'}

    for i, word in enumerate(words):
        word_lower = word.lower()
        if i == 0 or word_lower not in small_words:
            capitalized_words.append(word_lower.capitalize())
        else:
            capitalized_words.append(word_lower)

    return ' '.join(capitalized_words).strip()

@app.route('/extract', methods=['POST'])
def extract():
    file_path = None
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path:
            return jsonify({'error': 'No image_path provided'}), 400

        if not os.path.exists(file_path):
            logger.warning(f"File not found: {file_path}")
            return jsonify({'error': 'File not found'}), 404

        # Perform OCR
        # detail=1 returns position info
        result = reader.readtext(file_path)

        if not result:
            return jsonify({'card_name': None})

        # Filter results: Text length >= 3 and Confidence > 0.5
        valid_detections = []
        for detection in result:
            bbox, text, confidence = detection
            text = text.strip()
            if len(text) >= 3 and confidence > 0.5:
                # Store Y-coordinate (bbox[0][1]) to sort by top-most text
                top_y = bbox[0][1]
                valid_detections.append((top_y, text))

        if not valid_detections:
            return jsonify({'card_name': None})

        # Sort by Y position (top to bottom) to get the card title
        valid_detections.sort(key=lambda x: x[0])

        # Take the top-most valid text as the name
        raw_name = valid_detections[0][1]
        cleaned_name = clean_name_for_api(raw_name)

        logger.info(f"Extracted: {cleaned_name}")
        return jsonify({'card_name': cleaned_name})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': 'Internal Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up'}), 200

if __name__ == '__main__':
    # Use Waitress for production
    port = 5000
    logger.info(f"Starting OCR Server on port {port} using Waitress...")
    serve(app, host='127.0.0.1', port=port, threads=4)