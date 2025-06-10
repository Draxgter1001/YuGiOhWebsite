"""
Yu-Gi-Oh Card Name OCR API
Flask application for extracting card names from images using EasyOCR
"""

import easyocr
import cv2
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS
import base64
import io
from PIL import Image
import re
from typing import Tuple, List
import logging
import os

class SimpleCardNameOCR:
    """
    Simple OCR model to extract only the card name from Yu-Gi-Oh cards.
    """
    
    def __init__(self):
        self.reader = easyocr.Reader(['en'], gpu=False)
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
    
    def extract_card_name_from_path(self, image_path: str) -> Tuple[str, float, List]:
        """
        Extract card name from image file path.
        
        Args:
            image_path: Path to the image file
            
        Returns:
            Tuple of (card_name, confidence, all_detections)
        """
        try:
            # Read all text from the image
            result = self.reader.readtext(image_path)
            return self._process_ocr_results(result)
        except Exception as e:
            self.logger.error(f"Error processing image: {str(e)}")
            return "", 0.0, []
    
    def extract_card_name_from_array(self, image_array: np.ndarray) -> Tuple[str, float, List]:
        """
        Extract card name from image array.
        
        Args:
            image_array: Image as numpy array
            
        Returns:
            Tuple of (card_name, confidence, all_detections)
        """
        try:
            # Read all text from the image
            result = self.reader.readtext(image_array)
            return self._process_ocr_results(result)
        except Exception as e:
            self.logger.error(f"Error processing image: {str(e)}")
            return "", 0.0, []
    
    def _process_ocr_results(self, ocr_results: List) -> Tuple[str, float, List]:
        """
        Process OCR results to extract the card name.
        Card names are typically at the top of the card.
        
        Args:
            ocr_results: Raw OCR results from EasyOCR
            
        Returns:
            Tuple of (card_name, confidence, all_detections)
        """
        if not ocr_results:
            return "", 0.0, []
        
        # Convert results to a more workable format
        detections = []
        for detection in ocr_results:
            bbox = detection[0]
            text = detection[1]
            confidence = detection[2]
            
            # Calculate center Y position (vertical position)
            center_y = (bbox[0][1] + bbox[2][1]) / 2
            
            detections.append({
                'text': text,
                'confidence': confidence,
                'bbox': bbox,
                'center_y': center_y
            })
        
        # Filter potential card names
        card_name_candidates = self._filter_card_name_candidates(detections)
        
        if not card_name_candidates:
            return "", 0.0, [d['text'] for d in detections]
        
        # Get the best candidate (highest confidence among top candidates)
        best_candidate = max(card_name_candidates, key=lambda x: x['confidence'])
        
        # Clean the text
        cleaned_name = self._clean_card_name(best_candidate['text'])
        
        return cleaned_name, best_candidate['confidence'], [d['text'] for d in detections]
    
    def _filter_card_name_candidates(self, detections: List) -> List:
        """
        Filter detections to find likely card name candidates.
        
        Args:
            detections: List of detection dictionaries
            
        Returns:
            List of potential card name detections
        """
        if not detections:
            return []
        
        # Sort by vertical position (top to bottom)
        sorted_detections = sorted(detections, key=lambda x: x['center_y'])
        
        # Card name is typically in the top 30% of the card
        image_height = max(d['center_y'] for d in detections)
        top_threshold = image_height * 0.3
        
        # Filter detections in the top region
        top_detections = [d for d in sorted_detections if d['center_y'] <= top_threshold]
        
        # If no detections in top region, take the topmost detection
        if not top_detections and sorted_detections:
            top_detections = [sorted_detections[0]]
        
        # Filter out likely non-name text
        candidates = []
        for detection in top_detections:
            text = detection['text'].strip()
            
            # Skip if text is too short or looks like an ID/number
            if len(text) < 2:
                continue
                
            # Skip if it's mostly numbers (likely card ID)
            if re.match(r'^\d+$', text):
                continue
                
            # Skip if it's ATK/DEF pattern
            if re.match(r'^\d+/\d+$', text):
                continue
                
            # Skip common card elements that aren't names
            skip_patterns = [
                r'^\[.*\]$',  # Bracketed text
                r'^ATK$|^DEF$',  # ATK/DEF labels
                r'^\d+$',  # Pure numbers
                r'^\*+$',  # Star ratings
            ]
            
            if any(re.match(pattern, text, re.IGNORECASE) for pattern in skip_patterns):
                continue
            
            candidates.append(detection)
        
        return candidates
    
    def _clean_card_name(self, text: str) -> str:
        """
        Clean the extracted card name text.
        
        Args:
            text: Raw text from OCR
            
        Returns:
            Cleaned card name
        """
        if not text:
            return ""
        
        # Remove extra whitespace
        cleaned = re.sub(r'\s+', ' ', text.strip())
        
        # Remove common OCR artifacts
        cleaned = re.sub(r'[^\w\s\-\'\.\!\?]', '', cleaned)
        
        # Capitalize properly (Yu-Gi-Oh names are typically title case)
        cleaned = ' '.join(word.capitalize() for word in cleaned.split())
        
        return cleaned

# Flask API for integration with Spring Boot
app = Flask(__name__)
CORS(app)  # Enable CORS for React frontend

# Initialize OCR model
ocr_model = SimpleCardNameOCR()

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({"status": "healthy", "service": "yugioh-ocr"}), 200

@app.route('/extract-card-name', methods=['POST'])
def extract_card_name():
    """
    Extract card name from uploaded image.
    
    Expected JSON payload:
    {
        "image": "base64_encoded_image_string",
        "format": "jpg|png|jpeg" (optional)
    }
    
    Returns:
    {
        "success": true,
        "card_name": "Blue-Eyes White Dragon",
        "confidence": 0.95,
        "all_text": ["Blue-Eyes White Dragon", "ATK/3000", "DEF/2500", "..."],
        "message": "Card name extracted successfully"
    }
    """
    try:
        # Get JSON data
        data = request.get_json()
        
        if not data or 'image' not in data:
            return jsonify({
                "success": False,
                "error": "No image data provided",
                "message": "Please provide base64 encoded image in 'image' field"
            }), 400
        
        # Decode base64 image
        try:
            image_data = base64.b64decode(data['image'])
            image = Image.open(io.BytesIO(image_data))
            image_array = np.array(image)
            
            # Convert RGB to BGR for OpenCV (if needed)
            if len(image_array.shape) == 3 and image_array.shape[2] == 3:
                image_array = cv2.cvtColor(image_array, cv2.COLOR_RGB2BGR)
                
        except Exception as e:
            return jsonify({
                "success": False,
                "error": "Invalid image data",
                "message": f"Failed to decode image: {str(e)}"
            }), 400
        
        # Extract card name
        card_name, confidence, all_text = ocr_model.extract_card_name_from_array(image_array)
        
        # Prepare response
        if card_name:
            return jsonify({
                "success": True,
                "card_name": card_name,
                "confidence": round(confidence, 3),
                "all_text": all_text,
                "message": "Card name extracted successfully"
            }), 200
        else:
            return jsonify({
                "success": False,
                "card_name": "",
                "confidence": 0.0,
                "all_text": all_text,
                "message": "No card name detected in the image"
            }), 200
            
    except Exception as e:
        return jsonify({
            "success": False,
            "error": "Internal server error",
            "message": str(e)
        }), 500

@app.route('/extract-card-name-file', methods=['POST'])
def extract_card_name_file():
    """
    Extract card name from uploaded file.
    
    Expected: multipart/form-data with 'image' file
    
    Returns: Same as /extract-card-name
    """
    try:
        # Check if file is present
        if 'image' not in request.files:
            return jsonify({
                "success": False,
                "error": "No file uploaded",
                "message": "Please upload an image file"
            }), 400
        
        file = request.files['image']
        
        if file.filename == '':
            return jsonify({
                "success": False,
                "error": "No file selected",
                "message": "Please select a file to upload"
            }), 400
        
        # Read image file
        try:
            image = Image.open(io.BytesIO(file.read()))
            image_array = np.array(image)
            
            # Convert RGB to BGR for OpenCV (if needed)
            if len(image_array.shape) == 3 and image_array.shape[2] == 3:
                image_array = cv2.cvtColor(image_array, cv2.COLOR_RGB2BGR)
                
        except Exception as e:
            return jsonify({
                "success": False,
                "error": "Invalid image file",
                "message": f"Failed to process image: {str(e)}"
            }), 400
        
        # Extract card name
        card_name, confidence, all_text = ocr_model.extract_card_name_from_array(image_array)
        
        # Prepare response
        if card_name:
            return jsonify({
                "success": True,
                "card_name": card_name,
                "confidence": round(confidence, 3),
                "all_text": all_text,
                "message": "Card name extracted successfully"
            }), 200
        else:
            return jsonify({
                "success": False,
                "card_name": "",
                "confidence": 0.0,
                "all_text": all_text,
                "message": "No card name detected in the image"
            }), 200
            
    except Exception as e:
        return jsonify({
            "success": False,
            "error": "Internal server error",
            "message": str(e)
        }), 500

# Test function for your current setup
def test_local_image(image_path: str):
    """Test the OCR with a local image file."""
    print(f"Testing OCR with: {image_path}")
    print("=" * 50)
    
    card_name, confidence, all_text = ocr_model.extract_card_name_from_path(image_path)
    
    print(f"🎯 Card Name: '{card_name}'")
    print(f"📊 Confidence: {confidence:.3f}")
    print(f"📝 All detected text: {all_text}")
    
    return card_name, confidence

if __name__ == '__main__':
    # Test with your image first (optional)
    # test_image_path = 'card_images/51xpHakpcWL._AC_UF894,1000_QL80_.jpg'
    # if os.path.exists(test_image_path):
    #     print("Testing OCR Model:")
    #     test_local_image(test_image_path)
    #     print("\n" + "="*50)
    
    print("Starting Flask API server...")
    print("API will be available at:")
    print("- POST http://localhost:5000/extract-card-name (JSON)")
    print("- POST http://localhost:5000/extract-card-name-file (File upload)")
    print("- GET  http://localhost:5000/health (Health check)")
    print("="*50)
    
    # Start Flask development server
    app.run(debug=True, host='0.0.0.0', port=5000)