import os
import logging
import cv2
import numpy as np
import pytesseract
from pytesseract import Output
from flask import Flask, request, jsonify
from waitress import serve

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# ==========================================
# HELPER: Text Cleaning
# ==========================================
def clean_name_for_api(text):
    if not text: return None
    import re

    # Allow letters, numbers, spaces, apostrophes, hyphens
    cleaned = re.sub(r'[^\w\s\-\']', '', text)
    cleaned = ' '.join(cleaned.split())

    # Heuristic: Card names are rarely super short
    if len(cleaned) < 3: return None

    # Title Case
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

# ==========================================
# METHOD 1: Anti-Glare (For Foil/Shiny Cards)
# ==========================================
def extract_with_glare_removal(image_path):
    try:
        img = cv2.imread(image_path)
        if img is None: return None

        # 1. Crop to top 20% (Header)
        height, width = img.shape[:2]
        header_crop = img[int(height*0.02):int(height*0.20), int(width*0.02):int(width*0.98)]

        # 2. Glare Removal (CLAHE)
        gray = cv2.cvtColor(header_crop, cv2.COLOR_BGR2GRAY)
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
        gray = clahe.apply(gray)

        # 3. Resize & Denoise
        scaled = cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)
        denoised = cv2.fastNlMeansDenoising(scaled, None, 10, 7, 21)

        # 4. Threshold (Otsu)
        _, thresh = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        # 5. Extract (PSM 7 = Single Line)
        raw_text = pytesseract.image_to_string(thresh, config='--psm 7')
        return clean_name_for_api(raw_text)
    except Exception as e:
        logger.warning(f"Method 1 (Glare) failed: {e}")
        return None

# ==========================================
# METHOD 2: Standard Sort (For Normal Cards)
# ==========================================
def extract_standard_sort(image_path):
    try:
        img = cv2.imread(image_path)
        if img is None: return None

        # 1. Standard Preprocessing
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # Simple thresholding often works better for clear text
        _, thresh = cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)

        # 2. Extract Data (Coordinates)
        d = pytesseract.image_to_data(thresh, output_type=Output.DICT)

        # 3. Group Lines & Find Top-Most
        lines = {}
        n_boxes = len(d['text'])

        for i in range(n_boxes):
            conf = int(d['conf'][i])
            text = d['text'][i].strip()

            # Lower confidence threshold for standard scan
            if conf > 30 and len(text) > 1:
                line_id = (d['block_num'][i], d['line_num'][i])
                if line_id not in lines:
                    lines[line_id] = {'text': [], 'top': d['top'][i]}

                lines[line_id]['text'].append(text)
                lines[line_id]['top'] = min(lines[line_id]['top'], d['top'][i])

        candidates = []
        for line_data in lines.values():
            full_line = " ".join(line_data['text'])
            cleaned = clean_name_for_api(full_line)
            if cleaned:
                candidates.append((line_data['top'], cleaned))

        if not candidates: return None

        # Sort by Y position (Top to Bottom)
        candidates.sort(key=lambda x: x[0])
        return candidates[0][1]

    except Exception as e:
        logger.warning(f"Method 2 (Standard) failed: {e}")
        return None

# ==========================================
# MAIN ROUTE
# ==========================================
@app.route('/extract', methods=['POST'])
def extract():
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404

        # ATTEMPT 1: Anti-Glare (Best for Holographics)
        logger.info("Attempting Method 1: Anti-Glare...")
        card_name = extract_with_glare_removal(file_path)

        if card_name:
            logger.info(f"Method 1 Success: {card_name}")
            return jsonify({'card_name': card_name})

        # ATTEMPT 2: Standard Fallback (Best for Normal Cards)
        logger.info("Method 1 returned nothing. Attempting Method 2: Standard Sort...")
        card_name = extract_standard_sort(file_path)

        if card_name:
            logger.info(f"Method 2 Success: {card_name}")
            return jsonify({'card_name': card_name})

        # Failure
        logger.info("All OCR methods failed.")
        return jsonify({'card_name': None})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': 'Internal Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'hybrid-fallback'}), 200

if __name__ == '__main__':
    port = 5000
    serve(app, host='127.0.0.1', port=port, threads=2)