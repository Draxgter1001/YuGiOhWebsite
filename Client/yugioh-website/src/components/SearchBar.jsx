import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Search, Loader2, AlertCircle } from 'lucide-react';
import { apiService } from '../services/api';

const SearchBar = ({ onSearch }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState(null);

  // Autocomplete state
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);

  const inputRef = useRef(null);
  const dropdownRef = useRef(null);
  const debounceRef = useRef(null);

  // Fetch autocomplete suggestions
  const fetchSuggestions = useCallback(async (query) => {
    if (query.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    setIsLoadingSuggestions(true);
    try {
      const results = await apiService.autocompleteCard(query);
      setSuggestions(results || []);
      setShowSuggestions(true);
    } catch (err) {
      setSuggestions([]);
    } finally {
      setIsLoadingSuggestions(false);
    }
  }, []);

  // Debounced search
  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(() => {
      fetchSuggestions(searchTerm);
    }, 300);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [searchTerm, fetchSuggestions]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (
          dropdownRef.current &&
          !dropdownRef.current.contains(e.target) &&
          !inputRef.current.contains(e.target)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearch = async (cardName = searchTerm) => {
    if (!cardName.trim()) return;

    setError(null);
    setIsSearching(true);
    setShowSuggestions(false);

    try {
      const card = await apiService.searchCard(cardName);
      if (card) {
        onSearch(card);
        setSearchTerm('');
      } else {
        setError('Card not found. Try a different name.');
      }
    } catch (err) {
      setError('Card not found. Try a different name.');
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectSuggestion = (suggestion) => {
    setSearchTerm(suggestion.name);
    setShowSuggestions(false);
    setSelectedIndex(-1);
    handleSearch(suggestion.name);
  };

  const handleKeyDown = (e) => {
    if (!showSuggestions || suggestions.length === 0) {
      if (e.key === 'Enter') {
        handleSearch();
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev =>
            prev < suggestions.length - 1 ? prev + 1 : prev
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev => prev > 0 ? prev - 1 : -1);
        break;
      case 'Enter':
        e.preventDefault();
        if (selectedIndex >= 0 && suggestions[selectedIndex]) {
          handleSelectSuggestion(suggestions[selectedIndex]);
        } else {
          handleSearch();
        }
        break;
      case 'Escape':
        setShowSuggestions(false);
        setSelectedIndex(-1);
        break;
      default:
        break;
    }
  };

  const handleChange = (e) => {
    setError(null);
    setSearchTerm(e.target.value);
    setSelectedIndex(-1);
  };

  return (
      <div className="search-bar-container">
        <div className="search-bar-wrapper">
          <input
              ref={inputRef}
              type="text"
              value={searchTerm}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              onFocus={() => {
                if (suggestions.length > 0) {
                  setShowSuggestions(true);
                }
              }}
              placeholder="Search for a card by name..."
              className="search-input"
              disabled={isSearching}
              autoComplete="off"
          />
          <button
              onClick={() => handleSearch()}
              disabled={isSearching || !searchTerm.trim()}
              className="search-button"
              aria-label="Search"
          >
            {isSearching ? (
                <Loader2 className="search-spinner" size={20} />
            ) : (
                <Search className="search-icon" size={20} />
            )}
          </button>

          {/* Autocomplete Dropdown */}
          {showSuggestions && (
              <div ref={dropdownRef} className="autocomplete-dropdown">
                {isLoadingSuggestions ? (
                    <div className="autocomplete-loading">
                      <Loader2 className="spin" size={16} />
                      <span>Searching...</span>
                    </div>
                ) : suggestions.length > 0 ? (
                    suggestions.map((suggestion, index) => (
                        <div
                            key={suggestion.id}
                            className={`autocomplete-item ${index === selectedIndex ? 'selected' : ''}`}
                            onClick={() => handleSelectSuggestion(suggestion)}
                            onMouseEnter={() => setSelectedIndex(index)}
                        >
                          {suggestion.imageUrlSmall && (
                              <img
                                  src={suggestion.imageUrlSmall}
                                  alt=""
                                  className="autocomplete-thumb"
                                  loading="lazy"
                              />
                          )}
                          <div className="autocomplete-info">
                            <div className="autocomplete-name">{suggestion.name}</div>
                            {suggestion.type && (
                                <div className="autocomplete-type">{suggestion.type}</div>
                            )}
                          </div>
                        </div>
                    ))
                ) : searchTerm.length >= 2 ? (
                    <div className="autocomplete-empty">No cards found</div>
                ) : null}
              </div>
          )}
        </div>

        {error && (
            <div className="search-error-message">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
        )}

        <p className="search-disclaimer">
          Start typing to see suggestions, or enter a card name and press Enter.
        </p>
      </div>
  );
};

export default SearchBar;