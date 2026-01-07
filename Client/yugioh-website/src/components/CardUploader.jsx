import React, { useState } from 'react';
import { Upload, Loader2, AlertCircle, Check } from 'lucide-react';
import { apiService } from '../services/api';

const CardUploader = ({ onCardFound }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);

    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) {
      handleUpload(file);
    } else {
      setError('Please upload an image file (JPG, PNG, or WebP)');
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      handleUpload(file);
    }
  };

  const handleUpload = async (file) => {
    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('File size must be under 5MB');
      return;
    }

    setIsUploading(true);
    setError(null);
    setSuccess(false);

    try {
      const response = await apiService.uploadCard(file);

      if (response.success && response.data) {
        setSuccess(true);
        onCardFound(response.data);
        setTimeout(() => setSuccess(false), 3000);
      } else {
        setError(response.message || 'Could not identify the card. Try a clearer image.');
      }
    } catch (err) {
      setError('Upload failed. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };

  return (
      <div className="card-uploader-container">
        <div
            className={`card-drop-area ${isDragging ? 'dragging' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
          <input
              type="file"
              accept="image/*"
              onChange={handleFileSelect}
              className="file-input"
              id="file-upload"
              disabled={isUploading}
          />

          <label htmlFor="file-upload" className="upload-label">
            <div className="upload-icon-area">
              {isUploading ? (
                  <Loader2 className="loading-spinner" size={48} />
              ) : success ? (
                  <Check className="success-icon" size={48} />
              ) : (
                  <Upload size={48} />
              )}
            </div>

            <h3 className="upload-title">
              {isUploading ? 'Scanning Card...' : success ? 'Card Found!' : 'Upload Card Image'}
            </h3>

            <p className="upload-description">
              {isUploading
                  ? 'Our AI is identifying your card'
                  : 'Drag and drop your card image here, or click to browse'
              }
            </p>

            {!isUploading && !success && (
                <div className="file-types">
                  <span>JPG</span>
                  <span>PNG</span>
                  <span>WEBP</span>
                </div>
            )}
          </label>

          {error && (
              <div className="error-banner">
                <AlertCircle className="error-icon" size={18} />
                <span>{error}</span>
              </div>
          )}
        </div>
      </div>
  );
};

export default CardUploader;