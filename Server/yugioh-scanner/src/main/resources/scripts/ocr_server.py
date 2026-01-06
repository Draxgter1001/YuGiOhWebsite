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

def clean_name_for_api(text):
    if not text: return None
    import re
    # Keep alphanumeric, spaces, hyphens, apostrophes, and ampersands
    cleaned = re.sub(r'[^\w\s\-\'&]', '', text)
    cleaned = ' '.join(cleaned.split())
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

def process_and_read(image_crop, mode):
    """
    Applies a filter and runs generic OCR (not line-restricted).
    Returns the found text string.
    """
    try:
        # 1. Convert to Gray
        gray = cv2.cvtColor(image_crop, cv2.COLOR_BGR2GRAY)

        # 2. Scale Up (3x) for readability
        scaled = cv2.resize(gray, None, fx=3, fy=3, interpolation=cv2.INTER_CUBIC)

        if mode == 'standard':
            # Simple threshold
            _, processed = cv2.threshold(scaled, 127, 255, cv2.THRESH_BINARY)
        elif mode == 'clahe':
            # Anti-Glare (High Contrast)
            clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8,8))
            processed = clahe.apply(scaled)
            _, processed = cv2.threshold(processed, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        elif mode == 'denoise':
            # Remove "sparkles" (Slow but effective for foil)
            processed = cv2.fastNlMeansDenoising(scaled, None, 10, 7, 21)
            _, processed = cv2.threshold(processed, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        elif mode == 'inverted':
            # White text on dark card
            _, processed = cv2.threshold(scaled, 127, 255, cv2.THRESH_BINARY)
            processed = cv2.bitwise_not(processed)
        else:
            return None

        # 3. Read Data (More robust than image_to_string)
        # We assume PSM 6 (Assume a block of text) to catch disjointed words
        d = pytesseract.image_to_data(processed, config='--psm 6', output_type=Output.DICT)

        # 4. Reconstruct found words
        found_words = []
        n_boxes = len(d['text'])
        for i in range(n_boxes):
            # Confidence > 30 is low but lets us catch "faint" holographic text
            if int(d['conf'][i]) > 30 and len(d['text'][i].strip()) > 1:
                found_words.append(d['text'][i])

        return " ".join(found_words)
    except Exception as e:
        logger.warning(f"Strategy {mode} failed: {e}")
        return ""

@app.route('/extract', methods=['POST'])
def extract():
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404

        img = cv2.imread(file_path)
        if img is None: return jsonify({'error': 'Invalid image'}), 400

        height, width = img.shape[:2]

        # 1. CROP HEADER (Top 18%)
        # Slightly wider crop than before to ensure we don't cut tall letters
        # Cut right side to avoid Attribute symbol noise
        header = img[int(height*0.015):int(height*0.18), int(width*0.02):int(width*0.88)]

        # 2. BRUTE FORCE STRATEGIES
        # We run multiple visual filters and pick the longest valid text result.
        # Shiny cards usually fail 'standard' but pass 'clahe' or 'denoise'.
        strategies = ['standard', 'clahe', 'denoise', 'inverted']

        best_candidate = ""

        for mode in strategies:
            raw_text = process_and_read(header, mode)
            cleaned = clean_name_for_api(raw_text)

            if cleaned:
                logger.info(f"Strategy '{mode}' found: {cleaned}")
                # Heuristic: The longest extracted string is usually the correct full name
                # (prevents "Dragon" from beating "Rainbow Overdragon")
                if len(cleaned) > len(best_candidate):
                    best_candidate = cleaned

        if best_candidate:
            return jsonify({'card_name': best_candidate})
        else:
            logger.warning("All strategies failed to find text in header.")
            return jsonify({'card_name': None})

    except Exception as e:
        logger.error(f"Error: {str(e)}")
        return jsonify({'error': 'Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract-multi-strat'}), 200

if __name__ == '__main__':
    port = 5000
    serve(app, host='127.0.0.1', port=port, threads=4)