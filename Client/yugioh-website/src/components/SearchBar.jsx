import React, { useState } from 'react';
import { Search, Loader2 } from 'lucide-react';
import { apiService } from '../services/api';

const SearchBar = ({ onSearch }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const handleSearch = async () => {
    if (!searchTerm.trim()) return;

    setIsSearching(true);
    try {
      const card = await apiService.searchCard(searchTerm);
      onSearch(card);
    } catch (err) {
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

  return (
    <div className="search-bar-container">
      <div className="search-bar-wrapper">
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Search for a card by name..."
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
    </div>
  );
};

export default SearchBar;
