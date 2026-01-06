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

def preprocess_image(image_path):
    """
    Standard preprocessing to make text stand out.
    """
    try:
        img = cv2.imread(image_path)
        if img is None: return None

        # Convert to grayscale
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        # Otsu's thresholding (Automatic black/white contrast)
        _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        return thresh
    except Exception as e:
        logger.error(f"Preprocessing failed: {e}")
        return None

def clean_name_for_api(text):
    """
    Same cleaning logic as your original script to ensure database matches.
    """
    if not text: return None
    import re

    # 1. Basic cleanup
    cleaned = text.replace('|', 'I').replace('0', 'O')
    cleaned = re.sub(r'[^\w\s\-\']', '', cleaned)
    cleaned = ' '.join(cleaned.split())

    if len(cleaned) < 3: return None

    # 2. Smart Capitalization (from your original script)
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
    try:
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404

        # 1. Preprocess
        processed_img = preprocess_image(file_path)
        if processed_img is None:
            return jsonify({'error': 'Could not process image'}), 500

        # 2. Extract Data (Coordinates + Text)
        # output_type=Output.DICT gives us: 'text', 'top', 'conf', 'line_num', etc.
        d = pytesseract.image_to_data(processed_img, output_type=Output.DICT)

        # 3. Reconstruct Lines with Coordinates
        # Tesseract gives words; we need to group them into lines to find the "Title Line"
        lines = {}
        n_boxes = len(d['text'])

        for i in range(n_boxes):
            # Filter low confidence or empty text
            conf = int(d['conf'][i])
            text = d['text'][i].strip()

            if conf > 40 and len(text) > 1:
                # Group by 'block_num' and 'line_num' to reconstruct the sentence
                line_id = (d['block_num'][i], d['line_num'][i])

                if line_id not in lines:
                    lines[line_id] = {
                        'text': [],
                        'top': d['top'][i], # Y-coordinate of the line
                        'height': d['height'][i]
                    }

                lines[line_id]['text'].append(text)
                # Keep the minimum Y coordinate for the line (closest to top)
                lines[line_id]['top'] = min(lines[line_id]['top'], d['top'][i])

        # 4. Find the Valid Title
        candidates = []
        for line_data in lines.values():
            full_line_text = " ".join(line_data['text'])
            clean_text = clean_name_for_api(full_line_text)

            if clean_text:
                # Store (Y-Position, Cleaned Text)
                candidates.append((line_data['top'], clean_text))

        if not candidates:
            return jsonify({'card_name': None})

        # 5. Sort by Y-Position (Top to Bottom) - EXACTLY like your original code
        candidates.sort(key=lambda x: x[0])

        # The first item is the text closest to the top of the image -> The Card Name
        best_match = candidates[0][1]

        logger.info(f"Top-most text found: {best_match}")
        return jsonify({'card_name': best_match})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': 'Internal Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract-coord-sort'}), 200

if __name__ == '__main__':
    port = 5000
    logger.info(f"Starting OCR Server on port {port}...")
    serve(app, host='127.0.0.1', port=port, threads=4)