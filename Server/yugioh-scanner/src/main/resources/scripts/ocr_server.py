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
    # Keep alphanumeric, spaces, hyphens, apostrophes
    cleaned = re.sub(r'[^\w\s\-\']', '', text)
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

def process_super_res(image_path):
    try:
        img = cv2.imread(image_path)
        if img is None: return []

        height, width = img.shape[:2]

        # 1. CROP: Top 15% (Name Box Only)
        header = img[int(height*0.015):int(height*0.16), int(width*0.02):int(width*0.90)]

        # 2. SUPER SCALE (5x) - Critical for Tesseract Accuracy
        # Using INTER_CUBIC to smooth pixelation
        scaled = cv2.resize(header, None, fx=5, fy=5, interpolation=cv2.INTER_CUBIC)

        gray = cv2.cvtColor(scaled, cv2.COLOR_BGR2GRAY)

        processed_images = []

        # Strategy A: Standard Threshold (For common cards)
        # 100-255 range catches dark text on light backgrounds
        _, thresh_std = cv2.threshold(gray, 100, 255, cv2.THRESH_BINARY)
        processed_images.append(thresh_std)

        # Strategy B: DILATION (For Thin/Shiny Text)
        # This "thickens" the letters so the foil reflection doesn't break them apart
        kernel = np.ones((2,2), np.uint8)
        dilated = cv2.erode(thresh_std, kernel, iterations=1) # Erode black text = thicken it
        processed_images.append(dilated)

        # Strategy C: INVERTED + GLARE REMOVAL (For "Rainbow" / Ghost Rares)
        # 1. CLAHE to remove glare
        clahe = cv2.createCLAHE(clipLimit=4.0, tileGridSize=(8,8))
        no_glare = clahe.apply(gray)
        # 2. Threshold
        _, thresh_glare = cv2.threshold(no_glare, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        # 3. Invert (Make letters white, background black) -> Tesseract reads this well
        inverted = cv2.bitwise_not(thresh_glare)
        processed_images.append(inverted)

        return processed_images
    except Exception as e:
        logger.error(f"Processing error: {e}")
        return []

@app.route('/extract', methods=['POST'])
def extract():
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404

        processed_variants = process_super_res(file_path)

        candidates = []

        for i, img in enumerate(processed_variants):
            # PSM 7: Single Line Mode (Since we cropped perfectly)
            # PSM 6: Block Mode (Backup if crop isn't perfect)
            for psm in [7, 6]:
                raw_text = pytesseract.image_to_string(img, config=f'--psm {psm}').strip()
                cleaned = clean_name_for_api(raw_text)

                if cleaned:
                    logger.info(f"Strategy {i} (PSM {psm}) found: {cleaned}")
                    candidates.append(cleaned)

        if not candidates:
            logger.warning("No text found in header.")
            return jsonify({'card_name': None})

        # Heuristic: Pick the longest valid name extracted
        best_match = max(candidates, key=len)

        return jsonify({'card_name': best_match})

    except Exception as e:
        logger.error(f"Error: {str(e)}")
        return jsonify({'error': 'Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract-super-res'}), 200

if __name__ == '__main__':
    port = 5000
    serve(app, host='127.0.0.1', port=port, threads=4)