import React from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { CardPriceDisplay } from './PriceDisplay';

// Helper component to render level stars
const LevelStars = ({ level }) => {
  if (!level) return null;
  return (
      <div className="level-stars">
        {Array.from({ length: level }).map((_, i) => (
            <span key={i} className="star-icon">
          ★
        </span>
        ))}
      </div>
  );
};

// Helper component for attribute icons
const AttributeIcon = ({ attribute }) => {
  if (!attribute) return null;
  return (
      <div className={`attribute-icon ${attribute.toLowerCase()}`} title={attribute}>
        {attribute}
      </div>
  );
};

const CardDetailModal = ({ card, onClose }) => {
  const modalContent = (
      <div className="zoom-modal detail-modal" onClick={onClose}>
        <div className="card-detail-content" onClick={(e) => e.stopPropagation()}>
          <button className="zoom-close detail-close-btn" onClick={onClose}>
            <X size={24} />
          </button>

          {/* Left Side: Card Image */}
          <div className="card-detail-image-wrapper">
            <img src={card.imageUrl} alt={card.name} className="card-detail-image" />
          </div>

          {/* Right Side: Card Info */}
          <div className="card-detail-info-panel">
            <header className="info-header">
              <h2 className="info-card-name">{card.name}</h2>
              <AttributeIcon attribute={card.attribute} />
            </header>

            {card.level && (
                <div className="info-level-row">
                  <LevelStars level={card.level} />
                </div>
            )}

            {card.atk !== null && (
                <div className="info-stats-row">
                  <span className="stat">ATK / {card.atk}</span>
                  <span className="stat">DEF / {card.def}</span>
                </div>
            )}

            <div className="info-type-row">
              <p>
                [{card.race} / {card.type}]
              </p>
            </div>

            <div className="info-desc-box">
              <h3 className="desc-title">Effect</h3>
              <p className="desc-text">{card.desc}</p>
            </div>

            {/* Price Display */}
            {card.prices && (
                <div className="info-price-section">
                  <h3 className="price-section-title">Market Prices</h3>
                  <CardPriceDisplay prices={card.prices} cardName={card.name} />
                </div>
            )}
          </div>
        </div>
      </div>
  );

  return createPortal(modalContent, document.body);
};

export default CardDetailModal;
