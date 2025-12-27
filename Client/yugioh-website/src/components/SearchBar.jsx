import React, { useState } from 'react';
import { Search, Loader2, AlertCircle } from 'lucide-react';
import { apiService } from '../services/api';

const SearchBar = ({ onSearch }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState(null);

  const handleSearch = async () => {
    if (!searchTerm.trim()) return;

    setError(null);
    setIsSearching(true);
    try {
      const card = await apiService.searchCard(searchTerm);

      if (card) {
        onSearch(card);
      } else {
        setError('Card not found. Please ensure the name is an exact match.');
      }
    } catch (err) {
      setError('Card not found. Please ensure the name is an exact match.');
      console.error('Search failed:', err);
    } finally {
      setIsSearching(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const handleChange = (e) => {
    setError(null);
    setSearchTerm(e.target.value);
  };

  return (
      <div className="search-bar-container">
        <div className="search-bar-wrapper">
          <input
              type="text"
              value={searchTerm}
              onChange={handleChange}
              onKeyPress={handleKeyPress}
              placeholder="Search for a card by exact name..."
              className="search-input"
              disabled={isSearching}
          />
          <button
              onClick={handleSearch}
              disabled={isSearching || !searchTerm.trim()}
              className="search-button"
          >
            {isSearching ? (
                <Loader2 className="search-spinner" />
            ) : (
                <Search className="search-icon" />
            )}
          </button>
        </div>

        {error && (
            <div className="search-error-message">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
        )}

        <p className="search-disclaimer">
          Disclaimer: The search requires the card's full and exact name to find a match.
        </p>
      </div>
  );
};

export default SearchBar;