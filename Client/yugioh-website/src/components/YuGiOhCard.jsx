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
                className="yugioh-card"
                onClick={handleClick}
                style={{ cursor: zoomable ? 'pointer' : 'default' }}
                title={zoomable ? 'Click to view details' : card.name}
            >
                <div className="card-image-wrapper">
                    <img
                        src={card.imageUrl}
                        alt={card.name}
                        className="card-image"
                        loading="lazy"
                    />
                    {zoomable && (
                        <div className="card-hover-overlay">
                            <span className="card-name">{card.name}</span>
                        </div>
                    )}
                </div>
            </div>

            {zoomable && isZoomed && (
                <CardDetailModal card={card} onClose={handleClose} />
            )}
        </>
    );
};

export default YuGiOhCard;