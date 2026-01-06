import os
import logging
import cv2
import numpy as np
import pytesseract
from PIL import Image
from flask import Flask, request, jsonify
from waitress import serve

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

def clean_name_for_api(text):
    if not text: return None
    import re
    # Remove non-name characters but keep spaces and apostrophes
    cleaned = re.sub(r'[^\w\s\-\']', '', text)
    cleaned = ' '.join(cleaned.split())
    # Heuristic: Valid card names are rarely shorter than 3 chars
    if len(cleaned) < 3: return None

    # Title Case Logic
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

def process_image_variant(image_crop, mode):
    """Applies different filters to handle glare/foil."""
    try:
        # 1. Convert to Gray
        gray = cv2.cvtColor(image_crop, cv2.COLOR_BGR2GRAY)

        # 2. Scale Up (Critical for Tesseract)
        scaled = cv2.resize(gray, None, fx=3, fy=3, interpolation=cv2.INTER_CUBIC)

        if mode == 'standard':
            # Simple thresholding (Good for common cards)
            _, processed = cv2.threshold(scaled, 127, 255, cv2.THRESH_BINARY)
        elif mode == 'clahe':
            # Glare Removal (Good for Ghost/Gold Rares)
            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
            processed = clahe.apply(scaled)
            _, processed = cv2.threshold(processed, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        elif mode == 'inverted':
            # Invert (Good for Dark/XYZ cards with white text)
            _, processed = cv2.threshold(scaled, 127, 255, cv2.THRESH_BINARY)
            processed = cv2.bitwise_not(processed)
        else:
            return scaled

        return processed
    except:
        return None

@app.route('/extract', methods=['POST'])
def extract():
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404

        # 1. Load Image
        img = cv2.imread(file_path)
        if img is None: return jsonify({'error': 'Invalid image'}), 400

        height, width = img.shape[:2]

        # 2. STRICT CROP: Name Box Only
        # Top 1.5% to 13% of the card.
        # Cut off the right side (88%) to avoid the Attribute Symbol (Light/Dark/etc)
        # This completely REMOVES the artwork and description from memory.
        name_box = img[int(height*0.015):int(height*0.13), int(width*0.03):int(width*0.88)]

        # 3. Strategy Loop: Try different filters until one works
        # This mimics "smart" detection by brute-forcing visual styles
        strategies = ['standard', 'clahe', 'inverted']

        extracted_name = None

        for mode in strategies:
            processed_img = process_image_variant(name_box, mode)
            if processed_img is None: continue

            # --psm 7: Treat image as a SINGLE TEXT LINE.
            # Since we cropped everything else out, this is the most accurate mode.
            raw_text = pytesseract.image_to_string(processed_img, config='--psm 7')

            cleaned = clean_name_for_api(raw_text)
            if cleaned:
                logger.info(f"Success with mode '{mode}': {cleaned}")
                extracted_name = cleaned
                break # Stop as soon as we find a valid name

        if extracted_name:
            return jsonify({'card_name': extracted_name})
        else:
            logger.warning("All OCR strategies failed on the cropped header.")
            return jsonify({'card_name': None})

    except Exception as e:
        logger.error(f"Error: {str(e)}")
        return jsonify({'error': 'Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract-strict-crop'}), 200

if __name__ == '__main__':
    port = 5000
    serve(app, host='127.0.0.1', port=port, threads=4)