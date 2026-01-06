import os
import logging
import cv2
import numpy as np
import pytesseract
from flask import Flask, request, jsonify
from waitress import serve

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

def preprocess_image(image_path):
    """
    Prepares image for Tesseract to improve accuracy.
    Converts to grayscale and applies thresholding.
    """
    try:
        # Read image
        img = cv2.imread(image_path)
        if img is None:
            return None

        # Convert to gray
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        # Apply simple thresholding to make text pop black-on-white
        # This helps Tesseract read card titles clearly
        _, thresh = cv2.threshold(gray, 150, 255, cv2.THRESH_BINARY)

        return thresh
    except Exception as e:
        logger.error(f"Preprocessing failed: {e}")
        return None

def clean_name_for_api(text):
    if not text: return None
    import re

    # Remove special chars but keep spaces and apostrophes
    cleaned = re.sub(r'[^\w\s\-\']', '', text)
    # Collapse multiple spaces
    cleaned = ' '.join(cleaned.split())

    # Heuristic: Card names are rarely longer than 50 chars or shorter than 3
    if len(cleaned) < 3 or len(cleaned) > 60:
        return None

    return cleaned.strip()

@app.route('/extract', methods=['POST'])
def extract():
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404

        # 1. Preprocess
        processed_img = preprocess_image(file_path)
        if processed_img is None:
            return jsonify({'error': 'Could not process image'}), 500

        # 2. Extract Text using Tesseract
        # --psm 6 assumes a single uniform block of text (good for card titles if cropped,
        # but we use --psm 3 for fully automatic page segmentation)
        raw_text = pytesseract.image_to_string(processed_img, config='--psm 3')

        # 3. Parse result
        lines = raw_text.split('\n')
        candidates = []

        for line in lines:
            cleaned = clean_name_for_api(line)
            if cleaned:
                candidates.append(cleaned)

        # Heuristic: The card title is usually the first valid text line detected at the top
        card_name = candidates[0] if candidates else None

        logger.info(f"Extracted: {card_name}")
        return jsonify({'card_name': card_name})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': 'Internal Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract'}), 200

if __name__ == '__main__':
    port = 5000
    logger.info(f"Starting Tesseract OCR Server on port {port}...")
    serve(app, host='127.0.0.1', port=port, threads=4)