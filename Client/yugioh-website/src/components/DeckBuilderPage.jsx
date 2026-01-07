import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import {
    ArrowLeft,
    Loader2,
    Save,
    Trash2,
    Plus,
    Minus,
    Search,
    AlertCircle,
    CheckCircle,
    Globe,
    Lock,
    X,
    Upload,
    Camera,
    DollarSign,
} from 'lucide-react';
import Header from '../components/Header';
import { DeckPriceDisplay, CompactPriceDisplay } from './PriceDisplay';

const DeckBuilderPage = () => {
    const { deckId } = useParams();
    const navigate = useNavigate();

    const [deck, setDeck] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    // Edit deck info state
    const [isEditing, setIsEditing] = useState(false);
    const [editName, setEditName] = useState('');
    const [editDescription, setEditDescription] = useState('');
    const [editIsPublic, setEditIsPublic] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    // Add card state
    const [showAddCard, setShowAddCard] = useState(false);
    const [addMode, setAddMode] = useState('search'); // 'search' or 'scan'
    const [searchTerm, setSearchTerm] = useState('');
    const [searchResult, setSearchResult] = useState(null);
    const [isSearching, setIsSearching] = useState(false);
    const [searchError, setSearchError] = useState('');
    const [selectedDeckType, setSelectedDeckType] = useState('MAIN');
    const [isAddingCard, setIsAddingCard] = useState(false);

    // Autocomplete state
    const [suggestions, setSuggestions] = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const debounceRef = useRef(null);
    const dropdownRef = useRef(null);
    const inputRef = useRef(null);

    // Scanner state
    const [isScanning, setIsScanning] = useState(false);
    const [scanError, setScanError] = useState('');

    // Validation state
    const [validation, setValidation] = useState(null);

    // Price display toggle
    const [showPrices, setShowPrices] = useState(true);

    useEffect(() => {
        fetchDeck();
    }, [deckId]);

    // Close dropdown on outside click
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (
                dropdownRef.current &&
                !dropdownRef.current.contains(e.target) &&
                inputRef.current &&
                !inputRef.current.contains(e.target)
            ) {
                setShowSuggestions(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const fetchDeck = async () => {
        try {
            setIsLoading(true);
            const data = await apiService.getDeck(deckId);
            setDeck(data);
            setEditName(data.name);
            setEditDescription(data.description || '');
            setEditIsPublic(data.isPublic);
        } catch (err) {
            setError('Failed to load deck');
        } finally {
            setIsLoading(false);
        }
    };

    const handleSaveInfo = async () => {
        if (!editName.trim()) return;

        setIsSaving(true);
        try {
            const updated = await apiService.updateDeck(
                deckId,
                editName,
                editDescription,
                editIsPublic
            );
            setDeck(updated);
            setIsEditing(false);
            showSuccess('Deck updated successfully');
        } catch (err) {
            setError('Failed to update deck');
        } finally {
            setIsSaving(false);
        }
    };

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

    // Handle search input change with debounce
    const handleSearchInputChange = (e) => {
        const value = e.target.value;
        setSearchTerm(value);
        setSearchError('');
        setSelectedIndex(-1);

        // Clear previous debounce
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }

        // Debounce autocomplete
        debounceRef.current = setTimeout(() => {
            fetchSuggestions(value);
        }, 300);
    };

    // Handle selecting a suggestion
    const handleSelectSuggestion = (suggestion) => {
        setSearchTerm(suggestion.name);
        setShowSuggestions(false);
        setSelectedIndex(-1);
        // Set the card as the search result
        setSearchResult(suggestion);
        setSuggestions([]);
    };

    // Handle keyboard navigation in autocomplete
    const handleSearchKeyDown = (e) => {
        if (!showSuggestions || suggestions.length === 0) {
            if (e.key === 'Enter') {
                handleSearchCard();
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
                    handleSearchCard();
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

    const handleSearchCard = async () => {
        if (!searchTerm.trim()) return;

        setIsSearching(true);
        setSearchError('');
        setSearchResult(null);
        setShowSuggestions(false);

        try {
            const card = await apiService.searchCard(searchTerm);
            if (card) {
                setSearchResult(card);
            } else {
                setSearchError('Card not found. Try typing to see suggestions.');
            }
        } catch (err) {
            setSearchError('Card not found. Try typing to see suggestions.');
        } finally {
            setIsSearching(false);
        }
    };

    // Handle file upload for scanning
    const handleFileSelect = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setIsScanning(true);
        setScanError('');
        setSearchResult(null);

        try {
            const response = await apiService.uploadCard(file);

            if (response.success && response.data) {
                setSearchResult(response.data);
            } else {
                setScanError(response.message || 'Failed to identify card');
            }
        } catch (err) {
            setScanError('Scan failed. Please try again.');
        } finally {
            setIsScanning(false);
            // Reset file input
            e.target.value = '';
        }
    };

    const handleAddCard = async () => {
        if (!searchResult) return;

        setIsAddingCard(true);
        try {
            const updated = await apiService.addCardToDeck(
                deckId,
                searchResult.id,
                1,
                selectedDeckType
            );
            setDeck(updated);
            setSearchResult(null);
            setSearchTerm('');
            closeAddCardModal();
            showSuccess('Card added to deck');
        } catch (err) {
            setError(err.message || 'Failed to add card');
        } finally {
            setIsAddingCard(false);
        }
    };

    const handleRemoveCard = async (cardId, deckType, quantity = 1) => {
        try {
            const updated = await apiService.removeCardFromDeck(deckId, cardId, deckType, quantity);
            setDeck(updated);
            showSuccess('Card removed');
        } catch (err) {
            setError('Failed to remove card');
        }
    };

    const handleValidate = async () => {
        try {
            const result = await apiService.validateDeck(deckId);
            setValidation(result);
        } catch (err) {
            setError('Failed to validate deck');
        }
    };

    const showSuccess = (message) => {
        setSuccessMessage(message);
        setTimeout(() => setSuccessMessage(''), 3000);
    };

    const clearError = () => setError('');

    const openAddCardModal = () => {
        setShowAddCard(true);
        setAddMode('search');
        setSearchTerm('');
        setSearchResult(null);
        setSearchError('');
        setScanError('');
        setSuggestions([]);
        setShowSuggestions(false);
        setSelectedDeckType('MAIN');
    };

    const closeAddCardModal = () => {
        setShowAddCard(false);
        setSearchResult(null);
        setSearchTerm('');
        setSearchError('');
        setScanError('');
        setSuggestions([]);
        setShowSuggestions(false);
        // Clear debounce
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }
    };

    // Group cards by their info
    const groupCards = (cards) => {
        const grouped = {};
        cards?.forEach((card) => {
            if (grouped[card.cardId]) {
                grouped[card.cardId].quantity += card.quantity;
            } else {
                grouped[card.cardId] = { ...card };
            }
        });
        return Object.values(grouped);
    };

    if (isLoading) {
        return (
            <div className="scanner-background">
                <div className="scanner-pattern" />
                <Header />
                <div className="loading-container">
                    <Loader2 className="spin" size={40} />
                    <p>Loading deck...</p>
                </div>
            </div>
        );
    }

    if (!deck) {
        return (
            <div className="scanner-background">
                <div className="scanner-pattern" />
                <Header />
                <div className="error-container">
                    <p>Deck not found</p>
                    <button onClick={() => navigate('/decks')}>Back to My Decks</button>
                </div>
            </div>
        );
    }

    return (
        <div className="scanner-background">
            <div className="scanner-pattern" />
            <Header />

            <main className="main-container deck-builder">
                {/* Back button and title */}
                <div className="deck-builder-header">
                    <button className="back-btn" onClick={() => navigate('/decks')}>
                        <ArrowLeft size={20} />
                        <span>Back to Decks</span>
                    </button>

                    <button className="add-card-btn" onClick={openAddCardModal}>
                        <Plus size={20} />
                        <span>Add Card</span>
                    </button>
                </div>

                {/* Messages */}
                {error && (
                    <div className="error-message" onClick={clearError}>
                        <AlertCircle size={20} />
                        <span>{error}</span>
                        <X size={16} className="close-icon" />
                    </div>
                )}

                {successMessage && (
                    <div className="success-message">
                        <CheckCircle size={20} />
                        <span>{successMessage}</span>
                    </div>
                )}

                {/* Deck Info Section */}
                <div className="deck-info-section">
                    {isEditing ? (
                        <div className="deck-edit-form">
                            <input
                                type="text"
                                value={editName}
                                onChange={(e) => setEditName(e.target.value)}
                                placeholder="Deck name"
                                className="deck-name-input"
                            />
                            <textarea
                                value={editDescription}
                                onChange={(e) => setEditDescription(e.target.value)}
                                placeholder="Description..."
                                rows={2}
                                className="deck-desc-input"
                            />
                            <label className="public-toggle">
                                <input
                                    type="checkbox"
                                    checked={editIsPublic}
                                    onChange={(e) => setEditIsPublic(e.target.checked)}
                                />
                                <span>Make deck public</span>
                            </label>
                            <div className="edit-actions">
                                <button className="cancel-btn" onClick={() => setIsEditing(false)}>
                                    Cancel
                                </button>
                                <button
                                    className="save-btn"
                                    onClick={handleSaveInfo}
                                    disabled={isSaving}
                                >
                                    {isSaving ? <Loader2 className="spin" size={16} /> : <Save size={16} />}
                                    <span>Save</span>
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div className="deck-info-display">
                            <div className="deck-title-row">
                                <h2>{deck.name}</h2>
                                {deck.isPublic ? (
                                    <Globe size={18} className="public-icon" />
                                ) : (
                                    <Lock size={18} className="private-icon" />
                                )}
                                <button className="edit-info-btn" onClick={() => setIsEditing(true)}>
                                    Edit
                                </button>
                            </div>
                            {deck.description && <p className="deck-desc">{deck.description}</p>}
                        </div>
                    )}

                    <div className="deck-counts">
                        <div className="count-item">
                            <span className="count-label">Main Deck</span>
                            <span className="count-value">{deck.mainDeckCount || 0}/60</span>
                        </div>
                        <div className="count-item">
                            <span className="count-label">Extra Deck</span>
                            <span className="count-value">{deck.extraDeckCount || 0}/15</span>
                        </div>
                        <div className="count-item">
                            <span className="count-label">Side Deck</span>
                            <span className="count-value">{deck.sideDeckCount || 0}/15</span>
                        </div>
                        <button className="validate-btn" onClick={handleValidate}>
                            Validate Deck
                        </button>
                    </div>

                    {/* Deck Total Prices */}
                    {deck.totalPrices && (
                        <div className="deck-prices-section">
                            <div className="deck-prices-header">
                                <DollarSign size={18} />
                                <span>Estimated Value</span>
                                <button
                                    className="toggle-prices-btn"
                                    onClick={() => setShowPrices(!showPrices)}
                                >
                                    {showPrices ? 'Hide' : 'Show'}
                                </button>
                            </div>
                            {showPrices && <DeckPriceDisplay totalPrices={deck.totalPrices} />}
                        </div>
                    )}
                </div>

                {/* Validation Results */}
                {validation && (
                    <div className={`validation-result ${validation.valid ? 'valid' : 'invalid'}`}>
                        <h4>
                            {validation.valid ? (
                                <>
                                    <CheckCircle size={20} /> Deck is Valid!
                                </>
                            ) : (
                                <>
                                    <AlertCircle size={20} /> Deck has issues
                                </>
                            )}
                        </h4>
                        {validation.validationErrors?.length > 0 && (
                            <ul>
                                {validation.validationErrors.map((err, i) => (
                                    <li key={i}>{err}</li>
                                ))}
                            </ul>
                        )}
                        <button className="close-validation" onClick={() => setValidation(null)}>
                            <X size={16} />
                        </button>
                    </div>
                )}

                {/* Card Lists */}
                <div className="deck-sections">
                    <DeckSection
                        title="Main Deck"
                        cards={groupCards(deck.mainDeck)}
                        deckType="MAIN"
                        onRemove={handleRemoveCard}
                        showPrices={showPrices}
                    />
                    <DeckSection
                        title="Extra Deck"
                        cards={groupCards(deck.extraDeck)}
                        deckType="EXTRA"
                        onRemove={handleRemoveCard}
                        showPrices={showPrices}
                    />
                    <DeckSection
                        title="Side Deck"
                        cards={groupCards(deck.sideDeck)}
                        deckType="SIDE"
                        onRemove={handleRemoveCard}
                        showPrices={showPrices}
                    />
                </div>
            </main>

            {/* Add Card Modal */}
            {showAddCard && (
                <div className="modal-overlay" onClick={closeAddCardModal}>
                    <div className="modal-content add-card-modal" onClick={(e) => e.stopPropagation()}>
                        <h3>Add Card to Deck</h3>

                        {/* Toggle between Search and Scan */}
                        <div className="add-mode-toggle">
                            <button
                                className={`mode-btn ${addMode === 'search' ? 'active' : ''}`}
                                onClick={() => setAddMode('search')}
                            >
                                <Search size={18} />
                                <span>Search</span>
                            </button>
                            <button
                                className={`mode-btn ${addMode === 'scan' ? 'active' : ''}`}
                                onClick={() => setAddMode('scan')}
                            >
                                <Camera size={18} />
                                <span>Scan</span>
                            </button>
                        </div>

                        {/* Search Mode with Autocomplete */}
                        {addMode === 'search' && (
                            <div className="search-row-wrapper">
                                <div className="search-row">
                                    <input
                                        ref={inputRef}
                                        type="text"
                                        value={searchTerm}
                                        onChange={handleSearchInputChange}
                                        onKeyDown={handleSearchKeyDown}
                                        onFocus={() => {
                                            if (suggestions.length > 0) {
                                                setShowSuggestions(true);
                                            }
                                        }}
                                        placeholder="Start typing card name..."
                                        disabled={isSearching}
                                        autoComplete="off"
                                    />
                                    <button onClick={handleSearchCard} disabled={isSearching || !searchTerm.trim()}>
                                        {isSearching ? <Loader2 className="spin" size={18} /> : <Search size={18} />}
                                    </button>
                                </div>

                                {/* Autocomplete Dropdown */}
                                {showSuggestions && (
                                    <div ref={dropdownRef} className="modal-autocomplete-dropdown">
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

                                <p className="search-hint">Type at least 2 characters to see suggestions</p>
                            </div>
                        )}

                        {/* Scan Mode */}
                        {addMode === 'scan' && (
                            <div className="scan-upload-area">
                                <input
                                    type="file"
                                    accept="image/*"
                                    onChange={handleFileSelect}
                                    className="scan-file-input"
                                    id="scan-upload"
                                    disabled={isScanning}
                                />
                                <label htmlFor="scan-upload" className="scan-upload-label">
                                    {isScanning ? (
                                        <>
                                            <Loader2 className="spin" size={32} />
                                            <span>Scanning card...</span>
                                        </>
                                    ) : (
                                        <>
                                            <Upload size={32} />
                                            <span>Click to upload card image</span>
                                            <small>or drag and drop</small>
                                        </>
                                    )}
                                </label>
                            </div>
                        )}

                        {/* Errors */}
                        {searchError && <p className="search-error">{searchError}</p>}
                        {scanError && <p className="search-error">{scanError}</p>}

                        {/* Card Result */}
                        {searchResult && (
                            <div className="search-result-card">
                                <img src={searchResult.imageUrl} alt={searchResult.name} />
                                <div className="card-info">
                                    <h4>{searchResult.name}</h4>
                                    <p>{searchResult.type}</p>
                                    {searchResult.prices && (
                                        <div className="result-card-price">
                                            {searchResult.prices.tcgplayerPrice && searchResult.prices.tcgplayerPrice !== '0' && (
                                                <span className="price-tag tcgplayer">
                                                    ${searchResult.prices.tcgplayerPrice}
                                                </span>
                                            )}
                                            {searchResult.prices.cardmarketPrice && searchResult.prices.cardmarketPrice !== '0' && (
                                                <span className="price-tag cardmarket">
                                                    €{searchResult.prices.cardmarketPrice}
                                                </span>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Add Options */}
                        {searchResult && (
                            <div className="add-options">
                                <div className="deck-type-select">
                                    <label>Add to:</label>
                                    <select
                                        value={selectedDeckType}
                                        onChange={(e) => setSelectedDeckType(e.target.value)}
                                    >
                                        <option value="MAIN">Main Deck</option>
                                        <option value="EXTRA">Extra Deck</option>
                                        <option value="SIDE">Side Deck</option>
                                    </select>
                                </div>
                                <button
                                    className="confirm-add-btn"
                                    onClick={handleAddCard}
                                    disabled={isAddingCard}
                                >
                                    {isAddingCard ? <Loader2 className="spin" size={16} /> : 'Add Card'}
                                </button>
                            </div>
                        )}

                        <button className="modal-close-btn" onClick={closeAddCardModal}>
                            <X size={20} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

// Deck Section Component with price display
const DeckSection = ({ title, cards, deckType, onRemove, showPrices }) => {
    if (!cards || cards.length === 0) {
        return (
            <div className="deck-section empty">
                <h3>{title}</h3>
                <p className="empty-section">No cards yet</p>
            </div>
        );
    }

    return (
        <div className="deck-section">
            <h3>{title} ({cards.reduce((sum, c) => sum + c.quantity, 0)})</h3>
            <div className="cards-grid">
                {cards.map((card) => (
                    <div key={card.cardId} className="deck-card-item">
                        <img src={card.imageUrl} alt={card.cardName || card.name} />
                        <div className="card-quantity">{card.quantity}x</div>
                        {showPrices && card.prices && (
                            <CompactPriceDisplay
                                prices={card.prices}
                                cardName={card.cardName || card.name}
                                quantity={card.quantity}
                            />
                        )}
                        <div className="card-actions">
                            <button
                                className="remove-one"
                                onClick={() => onRemove(card.cardId, deckType, 1)}
                                title="Remove one"
                            >
                                <Minus size={14} />
                            </button>
                            <button
                                className="remove-all"
                                onClick={() => onRemove(card.cardId, deckType)}
                                title="Remove all"
                            >
                                <Trash2 size={14} />
                            </button>
                        </div>
                        <div className="card-name-tooltip">{card.cardName || card.name}</div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default DeckBuilderPage;
