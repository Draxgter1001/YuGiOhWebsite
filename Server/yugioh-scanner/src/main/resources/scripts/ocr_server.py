import os
import logging
import pytesseract
from PIL import Image
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

def clean_name_for_api(card_name):
    """
    Cleans the extracted card name to match the format expected by your API.
    """
    if not card_name: return None
    import re

    # Basic cleanup
    cleaned = card_name.replace('|', 'I').replace('0', 'O')
    cleaned = ' '.join(cleaned.split())
    cleaned = cleaned.replace('"', '').replace("'", "")
    cleaned = re.sub(r'[^\w\s\-\']', '', cleaned)

    # Title Case restoration
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

        # 1. Open Image (Using PIL as requested)
        try:
            image = Image.open(file_path)
        except Exception as e:
            return jsonify({'error': 'Invalid image'}), 400

        # 2. Extract Data (Coordinates + Text)
        # We use image_to_data to get the position (top/left) of every word
        d = pytesseract.image_to_data(image, output_type=Output.DICT)

        # 3. CRITICAL FIX: Group words into Lines
        # Tesseract outputs separate words. We must group them by "Block" and "Line"
        # so "Blue-Eyes", "White", and "Dragon" become one sentence.
        lines = {}
        n_boxes = len(d['text'])

        for i in range(n_boxes):
            # Check confidence (convert string to float safely)
            try:
                conf = float(d['conf'][i])
            except:
                conf = -1

            text = d['text'][i].strip()

            # Filter low confidence trash
            if conf > 40 and len(text) > 0:
                # Group key: (Block Number, Line Number)
                line_id = (d['block_num'][i], d['line_num'][i])

                if line_id not in lines:
                    lines[line_id] = {
                        'text': [],
                        'top': d['top'][i] # Y-position of this line
                    }

                lines[line_id]['text'].append(text)
                # Keep the highest vertical position (smallest Y) for the line
                if d['top'][i] < lines[line_id]['top']:
                    lines[line_id]['top'] = d['top'][i]

        # 4. Filter and Format Lines
        valid_detections = []
        for line_data in lines.values():
            full_line = " ".join(line_data['text'])

            # Apply your cleaning immediately to check if it's valid text
            cleaned = clean_name_for_api(full_line)

            if cleaned and len(cleaned) >= 3:
                # Store (Y-Position, Cleaned Text)
                valid_detections.append((line_data['top'], cleaned))

        if not valid_detections:
            return jsonify({'card_name': None})

        # 5. Sort by Y-Position (Top to Bottom) - EXACTLY mimicking your original code
        valid_detections.sort(key=lambda x: x[0])

        # The first item is the text closest to the top of the image -> The Card Name
        card_name = valid_detections[0][1]

        logger.info(f"Top-most Line: {card_name}")
        return jsonify({'card_name': card_name})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': 'Internal Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract-simple-sort'}), 200

if __name__ == '__main__':
    port = 5000
    # Waitress is required for production stability
    logger.info(f"Starting Production OCR Server on port {port}...")
    serve(app, host='127.0.0.1', port=port, threads=4)