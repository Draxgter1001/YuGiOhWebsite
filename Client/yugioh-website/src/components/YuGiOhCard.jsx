import React, { useState } from 'react';
import CardDetailModal from './CardDetailModal';

const YuGiOhCard = ({ card, zoomable = false }) => {
  const [isZoomed, setIsZoomed] = useState(false);

  if (!card || !card.imageUrl) {
    return null;
  }

  const handleClick = () => {
    if (zoomable) {
      setIsZoomed(true);
    }
  };

  const handleClose = () => setIsZoomed(false);

  return (
      <>
        <div
            className="full-card-background"
            onClick={handleClick}
            style={{ backgroundImage: `url(${card.imageUrl})` }}
            title={zoomable ? 'Click to enlarge' : card.name}
        >
          <div className="card-overlay">
            <h2>{card.name}</h2>
          </div>
        </div>

        {zoomable && isZoomed && (
            <CardDetailModal card={card} onClose={handleClose} />
        )}
      </>
  );
};

export default YuGiOhCard;