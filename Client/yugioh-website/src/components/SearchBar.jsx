import React, { useState, useEffect, useRef } from 'react';
import { Search, Loader2, X } from 'lucide-react';
import { apiService } from '../services/api';

const SearchBar = ({ onSearch }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  
  const dropdownRef = useRef(null);
  const inputRef = useRef(null);
  const debounceTimer = useRef(null);

  // Fetch autocomplete suggestions when user types
  useEffect(() => {
    // Clear previous timer
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }

    // Don't search if less than 2 characters
    if (searchTerm.trim().length < 2) {
      setSuggestions([]);
      setShowDropdown(false);
      return;
    }

    // Debounce API calls (wait 300ms after user stops typing)
    debounceTimer.current = setTimeout(async () => {
      setIsLoading(true);
      try {
        const response = await apiService.autocompleteCards(searchTerm.trim(), 10);
        if (response.success && response.data) {
          setSuggestions(response.data);
          setShowDropdown(response.data.length > 0);
          setSelectedIndex(-1);
        }
      } catch (err) {
        console.error('Autocomplete error:', err);
        setSuggestions([]);
      } finally {
        setIsLoading(false);
      }
    }, 300);

    return () => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    };
  }, [searchTerm]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Handle keyboard navigation
  const handleKeyDown = (e) => {
    if (!showDropdown || suggestions.length === 0) {
      if (e.key === 'Enter') {
        handleManualSearch();
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex((prev) => 
          prev < suggestions.length - 1 ? prev + 1 : prev
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex((prev) => (prev > 0 ? prev - 1 : -1));
        break;
      case 'Enter':
        e.preventDefault();
        if (selectedIndex >= 0 && suggestions[selectedIndex]) {
          handleSelectCard(suggestions[selectedIndex]);
        } else if (suggestions.length > 0) {
          handleSelectCard(suggestions[0]);
        }
        break;
      case 'Escape':
        setShowDropdown(false);
        setSelectedIndex(-1);
        break;
      default:
        break;
    }
  };

  // Select a card from dropdown
  const handleSelectCard = (card) => {
    setSearchTerm(card.name);
    setShowDropdown(false);
    setSuggestions([]);
    setSelectedIndex(-1);
    onSearch(card);
  };

  // Manual search (pressing search button)
  const handleManualSearch = async () => {
    if (!searchTerm.trim()) return;
    
    // If there are suggestions and one is selected, use that
    if (selectedIndex >= 0 && suggestions[selectedIndex]) {
      handleSelectCard(suggestions[selectedIndex]);
      return;
    }

    // Otherwise, try to get the first suggestion or search directly
    if (suggestions.length > 0) {
      handleSelectCard(suggestions[0]);
    } else {
      // Fallback to exact search
      setIsLoading(true);
      try {
        const response = await apiService.searchCard(searchTerm.trim());
        if (response.success && response.data) {
          onSearch(response.data);
          setShowDropdown(false);
        }
      } catch (err) {
        console.error('Search error:', err);
      } finally {
        setIsLoading(false);
      }
    }
  };

  // Clear search input
  const handleClear = () => {
    setSearchTerm('');
    setSuggestions([]);
    setShowDropdown(false);
    setSelectedIndex(-1);
    inputRef.current?.focus();
  };

  // Handle input focus
  const handleFocus = () => {
    if (suggestions.length > 0) {
      setShowDropdown(true);
    }
  };

  return (
    <div className="search-bar-container" ref={dropdownRef}>
      <div className="search-bar-wrapper">
        <input
          ref={inputRef}
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={handleFocus}
          placeholder="Search for a card..."
          className="search-input"
          autoComplete="off"
        />
        
        {/* Clear button */}
        {searchTerm && (
          <button 
            className="search-clear-btn" 
            onClick={handleClear}
            type="button"
          >
            <X size={18} />
          </button>
        )}
        
        {/* Search button */}
        <button
          onClick={handleManualSearch}
          disabled={isLoading || !searchTerm.trim()}
          className="search-button"
        >
          {isLoading ? (
            <Loader2 className="search-spinner" />
          ) : (
            <Search className="search-icon" />
          )}
        </button>
      </div>

      {/* Autocomplete Dropdown */}
      {showDropdown && suggestions.length > 0 && (
        <div className="search-dropdown">
          {suggestions.map((card, index) => (
            <div
              key={card.id}
              className={`search-dropdown-item ${index === selectedIndex ? 'selected' : ''}`}
              onClick={() => handleSelectCard(card)}
              onMouseEnter={() => setSelectedIndex(index)}
            >
              <img 
                src={card.imageUrlSmall || card.imageUrl} 
                alt={card.name}
                className="dropdown-card-image"
                onError={(e) => {
                  e.target.style.display = 'none';
                }}
              />
              <div className="dropdown-card-info">
                <span className="dropdown-card-name">{card.name}</span>
                <span className="dropdown-card-type">{card.type}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Loading indicator in dropdown */}
      {isLoading && searchTerm.length >= 2 && (
        <div className="search-dropdown">
          <div className="search-dropdown-loading">
            <Loader2 className="search-spinner" size={20} />
            <span>Searching...</span>
          </div>
        </div>
      )}

      <p className="search-hint">
        Start typing to see suggestions (min. 2 characters)
      </p>
    </div>
  );
};

export default SearchBar;
