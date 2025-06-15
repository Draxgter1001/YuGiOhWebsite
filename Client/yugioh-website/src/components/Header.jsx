import React from 'react';
import { Sparkles, Book } from 'lucide-react';

const Header = () => {
  return (
    <header className="header">
      <div className="header-container">
        <div className="header-logo">
          <div style={{
            width: '48px',
            height: '48px',
            background: 'linear-gradient(to bottom right, #facc15, #f59e0b)',
            borderRadius: '0.5rem',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
          }}>
            <Sparkles style={{ width: '28px', height: '28px', color: '#111827' }} />
          </div>
          <h1 className="header-title">Yu-Gi-Oh! Scanner</h1>
        </div>
        <button className="my-deck-btn">
          <Book style={{ width: '20px', height: '20px', color: '#9ca3af' }} />
          <span>My Deck</span>
        </button>
      </div>
    </header>
  );
};

export default Header;
