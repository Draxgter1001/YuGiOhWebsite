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
    Advanced preprocessing to handle Holographic/Shiny Yu-Gi-Oh cards.
    """
    try:
        img = cv2.imread(image_path)
        if img is None: return None

        # 1. Geometry: Crop to top 18% (Name Box) to exclude artwork noise
        height, width = img.shape[:2]
        header_crop = img[int(height*0.025):int(height*0.18), int(width*0.035):int(width*0.965)]

        # 2. Convert to Grayscale
        gray = cv2.cvtColor(header_crop, cv2.COLOR_BGR2GRAY)

        # 3. GLARE REMOVAL (CLAHE) - Critical for Shiny Cards
        # This equalizes light distribution, removing the "rainbow" reflection
        clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8,8))
        gray = clahe.apply(gray)

        # 4. Resize: Scale up 2.5x to separate letters
        # INTER_CUBIC is slower but builds better letter edges
        scaled = cv2.resize(gray, None, fx=2.5, fy=2.5, interpolation=cv2.INTER_CUBIC)

        # 5. Denoising: Remove "sparkles" from the foil texture
        denoised = cv2.fastNlMeansDenoising(scaled, None, 10, 7, 21)

        # 6. Thresholding: Binarize the text
        _, thresh = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        return thresh
    except Exception as e:
        logger.error(f"Preprocessing failed: {e}")
        return None

def clean_name_for_api(text):
    if not text: return None
    import re

    # Filter junk characters often found in noisy OCR
    # We allow: Letters, Numbers, Spaces, Hyphens, Apostrophes
    cleaned = re.sub(r'[^\w\s\-\']', '', text)
    cleaned = ' '.join(cleaned.split())

    if len(cleaned) < 3: return None

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

        # 1. Advanced Preprocessing
        processed_img = preprocess_image(file_path)
        if processed_img is None:
            return jsonify({'error': 'Could not process image'}), 500

        # 2. Extract Data (Coordinates + Text)
        # --psm 7 tells Tesseract to treat the image as a "Single Text Line"
        # This prevents it from reading noise as separate lines
        d = pytesseract.image_to_data(processed_img, config='--psm 7', output_type=Output.DICT)

        # 3. Reconstruct the line with highest confidence
        words = []
        n_boxes = len(d['text'])

        for i in range(n_boxes):
            text = d['text'][i].strip()
            conf = int(d['conf'][i])

            # Confidence threshold > 40 helps ignore "sparkle" noise interpreted as text
            if conf > 40 and len(text) > 1:
                words.append(text)

        raw_text = " ".join(words)
        card_name = clean_name_for_api(raw_text)

        logger.info(f"Raw: {raw_text} -> Extracted: {card_name}")
        return jsonify({'card_name': card_name})

    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return jsonify({'error': 'Internal Processing Error'}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'up', 'engine': 'tesseract-clahe-optimized'}), 200

if __name__ == '__main__':
    port = 5000
    # Use 2 threads to save memory for image processing
    serve(app, host='127.0.0.1', port=port, threads=2)