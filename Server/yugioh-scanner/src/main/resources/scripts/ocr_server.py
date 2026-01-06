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

def preprocess_for_title(image_path):
    """
    Crops the image to just the top header (where the name is)
    and applies heavy contrast to isolate text.
    """
    try:
        # Read image
        img = cv2.imread(image_path)
        if img is None:
            return None

        # 1. Geometry: Crop to top 15% of the card (The layout is standard)
        height, width = img.shape[:2]
        # Crop top 15% (Name Box)
        # We also crop a tiny bit from left/right/top to remove borders
        header_crop = img[int(height*0.02):int(height*0.15), int(width*0.02):int(width*0.98)]

        # 2. Convert to Gray
        gray = cv2.cvtColor(header_crop, cv2.COLOR_BGR2GRAY)

        # 3. Scaling: Double the size (Tesseract loves big text)
        scaled = cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)

        # 4. Thresholding: Make text BLACK and background WHITE (or inverse)
        # Otsu's binarization automatically finds the best separation
        _, thresh = cv2.threshold(scaled, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        return thresh
    except Exception as e:
        logger.error(f"Preprocessing failed: {e}")
        return None

def clean_name_for_api(text):
    if not text: return None
    import re

    # Keep alphanumeric, spaces, and specific Yu-Gi-Oh chars like apostrophes
    cleaned = re.sub(r'[^\w\s\-\']', '', text)
    cleaned = ' '.join(cleaned.split())

    # Filter out garbage noise (too short)
    if len(cleaned) < 2:
        return None

    return cleaned.strip()

@app.route('/extract', methods=['POST'])
def extract():
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404

        # 1. Preprocess (Crop to Title + Threshold)
        processed_img = preprocess_for_title(file_path)
        if processed_img is None:
            return jsonify({'error': 'Could not process image'}), 500

        # 2. Extract Text using Tesseract
        # --psm 7: Treat the image as a single text line (Since we cropped it)
        # This is CRITICAL. It tells Tesseract "Don't look for paragraphs, just read this one line".
        raw_text = pytesseract.image_to_string(processed_img, config='--psm 7')

        # 3. Clean result
        card_name = clean_name_for_api(raw_text)

        logger.info(f"Raw OCR: {raw_text.strip()} -> Extracted: {card_name}")

        return jsonify({'card_name': card_name})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': 'Internal Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract-cropped'}), 200

if __name__ == '__main__':
    port = 5000
    logger.info(f"Starting Smart-Crop OCR Server on port {port}...")
    serve(app, host='127.0.0.1', port=port, threads=4)