import easyocr
import cv2
from matplotlib import pyplot as plt
import numpy as np

def extract_card_name(image_path):
    """
    Extract the card name from a Yu-Gi-Oh card image.
    The name is typically the topmost text on the card.
    """
    reader = easyocr.Reader(['en'], gpu=False)
    result = reader.readtext(image_path)
    
    if not result:
        return None, "No text detected"
    
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
        return None, "No valid text detected"
    
    # Sort by y-coordinate (top to bottom) and get the topmost text
    valid_detections.sort(key=lambda x: x[0])
    card_name = valid_detections[0][1]
    
    return card_name, valid_detections[0][2]

def visualize_name_detection(image_path, show_all_text=False):
    """
    Visualize the card name detection on the image.
    """
    reader = easyocr.Reader(['en'], gpu=False)
    result = reader.readtext(image_path)
    
    card_name, name_detection = extract_card_name(image_path)
    
    image = cv2.imread(image_path)
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    
    if show_all_text:
        # Draw all detected text in blue
        for detection in result:
            bbox = detection[0]
            text = detection[1]
            top_left = tuple([int(val) for val in bbox[0]])
            bottom_right = tuple([int(val) for val in bbox[2]])
            image_rgb = cv2.rectangle(image_rgb, top_left, bottom_right, (0, 0, 255), 2)
            image_rgb = cv2.putText(image_rgb, text, (top_left[0], top_left[1] - 10), 
                                  cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
    
    # Highlight the card name in green
    if name_detection:
        bbox = name_detection[0]
        top_left = tuple([int(val) for val in bbox[0]])
        bottom_right = tuple([int(val) for val in bbox[2]])
        image_rgb = cv2.rectangle(image_rgb, top_left, bottom_right, (0, 255, 0), 3)
        image_rgb = cv2.putText(image_rgb, f"NAME: {card_name}", 
                              (top_left[0], top_left[1] - 10), 
                              cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
    
    plt.figure(figsize=(12, 8))
    plt.imshow(image_rgb)
    plt.title(f"Detected Card Name: {card_name}")
    plt.axis('off')
    plt.show()
    
    return card_name

# Main execution
if __name__ == "__main__":
    image_path = 'card_images/Rainbow.jpg'
    
    # Extract just the card name
    card_name, _ = extract_card_name(image_path)
    
    if card_name:
        print(f"Card Name: {card_name}")
        
        # Clean the name for API usage (remove special characters, extra spaces)
        clean_name = ' '.join(card_name.split())
        print(f"Cleaned Name for API: {clean_name}")
        
        # Visualize the detection
        visualize_name_detection(image_path, show_all_text=True)
    else:
        print("Could not extract card name from the image")