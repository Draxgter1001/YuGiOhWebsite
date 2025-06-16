// src/components/SearchBar.jsx

import React, { useState } from 'react';
import { Search, Loader2, AlertCircle } from 'lucide-react'; // Import AlertCircle for the error icon
import { apiService } from '../services/api';

const SearchBar = ({ onSearch }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState(null); // State for the error message

  const handleSearch = async () => {
    if (!searchTerm.trim()) return;

    setError(null); // Clear previous errors
    setIsSearching(true);
    try {
      const card = await apiService.searchCard(searchTerm);
      
      if (card) {
        onSearch(card);
      } else {
        // Handle cases where the API returns a successful response but no card data
        setError('Card not found. Please ensure the name is an exact match.');
      }

    } catch (err) {
      // Handle network errors or 404s from the API
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
    setError(null); // Clear error when user types again
    setSearchTerm(e.target.value);
  }

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

      {/* Display the error message if one exists */}
      {error && (
        <div className="search-error-message">
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {/* Add the disclaimer */}
      <p className="search-disclaimer">
        Disclaimer: The search requires the card's full and exact name to find a match.
      </p>
    </div>
  );
};

export default SearchBar;