import os
import sys
import easyocr
import logging
from flask import Flask, request, jsonify

# Configure logging for production
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 1. Load the model ONCE when the server starts
print("Loading OCR Model... this may take a moment.")
reader = easyocr.Reader(['en'], gpu=False) # Set gpu=True if you have a GPU server
print("OCR Model Loaded and Ready.")

def clean_name_for_api(card_name):
    """Same cleaning logic as your original script"""
    if not card_name: return None
    import re
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
        # Get image path from request
        data = request.json
        file_path = data.get('image_path')

        if not file_path or not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 400

        # Perform OCR
        result = reader.readtext(file_path)

        if not result:
            return jsonify({'card_name': None})

        # Filter results (Logic preserved from your original script)
        valid_detections = []
        for detection in result:
            text = detection[1].strip()
            confidence = detection[2]
            if len(text) >= 3 and confidence > 0.5:
                bbox = detection[0]
                top_y = bbox[0][1]
                valid_detections.append((top_y, text))

        if not valid_detections:
            return jsonify({'card_name': None})

        # Sort by Y position to get the title
        valid_detections.sort(key=lambda x: x[0])
        raw_name = valid_detections[0][1]
        cleaned_name = clean_name_for_api(raw_name)

        return jsonify({'card_name': cleaned_name})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    # Run on localhost port 5000
    app.run(host='127.0.0.1', port=5000)