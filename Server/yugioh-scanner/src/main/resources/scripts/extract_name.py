import warnings
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'  # Suppress TensorFlow warnings
warnings.filterwarnings('ignore')  # Suppress all warnings

import easyocr
import sys
import re

def clean_name_for_api(card_name):
    """Clean the card name for API usage"""
    if not card_name:
        return None
    
    # Remove common OCR artifacts
    cleaned = card_name.replace('|', 'I').replace('0', 'O')
    
    # Remove extra whitespace and normalize
    cleaned = ' '.join(cleaned.split())
    
    # Remove common punctuation that might interfere with API
    cleaned = cleaned.replace('"', '').replace("'", "")
    
    # Remove special characters but keep hyphens and apostrophes in names
    cleaned = re.sub(r'[^\w\s\-\']', '', cleaned)
    
    # Convert to proper title case (handles ALL CAPS from OCR)
    # Split by common delimiters and capitalize each part
    words = cleaned.split()
    capitalized_words = []
    
    # Words that should remain lowercase unless they're the first word
    small_words = {'the', 'of', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'from', 'with', 'by'}
    
    for i, word in enumerate(words):
        word_lower = word.lower()
        if i == 0 or word_lower not in small_words:
            # Capitalize first letter, rest lowercase
            capitalized_words.append(word_lower.capitalize())
        else:
            # Keep small words lowercase unless they're first
            capitalized_words.append(word_lower)
    
    cleaned = ' '.join(capitalized_words)
    
    return cleaned.strip()

def extract_card_name(image_path):
    """
    Extract the card name from a Yu-Gi-Oh card image.
    The name is typically the topmost text on the card.
    """
    try:
        # Suppress warnings during reader initialization
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            reader = easyocr.Reader(['en'], gpu=False, verbose=False)
        
        result = reader.readtext(image_path)
        
        if not result:
            return None
        
        # Filter out very short text (likely noise) and get positions
        valid_detections = []
        for detection in result:
            text = detection[1].strip()
            confidence = detection[2]
            
            # Filter out very short text and low confidence detections
            if len(text) >= 3 and confidence > 0.5:
                bbox = detection[0]
                # Get the top-left y coordinate (vertical position)
                top_y = bbox[0][1]
                valid_detections.append((top_y, text, detection))
        
        if not valid_detections:
            return None
        
        # Sort by y-coordinate (top to bottom) and get the topmost text
        valid_detections.sort(key=lambda x: x[0])
        card_name = valid_detections[0][1]
        
        # Clean the name for API usage
        cleaned_name = clean_name_for_api(card_name)
        
        return cleaned_name
        
    except Exception as e:
        print(f"ERROR: {str(e)}", file=sys.stderr)
        return None

def main():
    # Suppress all warnings and redirect them to devnull
    import logging
    logging.getLogger().setLevel(logging.ERROR)
    
    if len(sys.argv) != 2:
        print("ERROR: Usage: python extract_name.py <image_path>", file=sys.stderr)
        sys.exit(1)
    
    image_path = sys.argv[1]
    
    # Check if file exists
    if not os.path.exists(image_path):
        print(f"ERROR: Image file not found: {image_path}", file=sys.stderr)
        sys.exit(1)
    
    # Extract card name
    card_name = extract_card_name(image_path)
    
    if card_name and card_name.strip():
        # Output ONLY the card name to stdout (this will be captured by Java)
        print(card_name.strip())
        sys.exit(0)
    else:
        print("ERROR: Could not extract card name from image", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()