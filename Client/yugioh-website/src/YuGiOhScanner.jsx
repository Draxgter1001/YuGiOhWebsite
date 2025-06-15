import React, { useState, useEffect, useRef } from 'react';
import Header from './components/Header';
import CardUploader from './components/CardUploader';
import SearchBar from './components/SearchBar';
import YuGiOhCard from './components/YuGiOhCard';

const YuGiOhScanner = () => {
  const [currentCard, setCurrentCard] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [recentCards, setRecentCards] = useState([]);
  const [showBackToTop, setShowBackToTop] = useState(false);

  const identifiedSectionRef = useRef(null);

  const handleCardFound = (card) => {
    setCurrentCard(card);
    setRecentCards(prev => {
      const filtered = prev.filter(c => c.id !== card.id);
      return [card, ...filtered].slice(0, 10);
    });
  };

 useEffect(() => {
    const checkScrollTop = () => {
      if (!showBackToTop && window.scrollY > 400) {
        setShowBackToTop(true);
      } else if (showBackToTop && window.scrollY <= 400) {
        setShowBackToTop(false);
      }
    };
    window.addEventListener('scroll', checkScrollTop);
    return () => window.removeEventListener('scroll', checkScrollTop);
  }, [showBackToTop]);

  useEffect(() => {
    if (currentCard && identifiedSectionRef.current) {
      identifiedSectionRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    }
  }, [currentCard]); 

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="scanner-background">
      <div className="scanner-pattern" />
      <Header />
      <main className="main-container">
        <div className="scanner-heading">
          <h2 className="section-title">Scan Your Cards</h2>
          <p className="section-subtitle">
            Upload a card image to identify it instantly with our AI-powered scanner
          </p>
        </div>

        <div className="upload-section">
          <CardUploader onCardFound={handleCardFound} />
        </div>

        <div className="divider">
          <div className="divider-line" />
          <span className="divider-text">OR</span>
          <div className="divider-line" />
        </div>

        <div className="search-section">
          <SearchBar onSearch={handleCardFound} />
        </div>

        {currentCard && (
          <div ref={identifiedSectionRef} className="identified-section">
            <h3 className="section-title">Identified Card</h3>
            <div className="card-display">
              <YuGiOhCard card={currentCard} isLoading={isLoading} zoomable={true} />
            </div>
          </div>
        )}

        {recentCards.length > 0 && (
          <div className="recent-section">
            <h3 className="section-title">Recent Scans</h3>
            <div className={`recent-cards-container ${recentCards.length > 4 ? 'vertical-layout' : ''}`}>
              {recentCards.map((card) => (
                <div key={card.id} className="recent-card-wrapper">
                  <YuGiOhCard card={card} zoomable={true} />
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* Back to Top Button */}
      <button 
        className={`back-to-top-btn ${showBackToTop ? 'visible' : ''}`}
        onClick={scrollToTop}
        title="Back to top"
      >
        ↑
      </button>
    </div>
  );
};

export default YuGiOhScanner;