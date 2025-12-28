import React, { useState, useEffect } from 'react';
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
} from 'lucide-react';
import Header from './Header';

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
    const [addMode, setAddMode] = useState('search');
    const [searchTerm, setSearchTerm] = useState('');
    const [searchResult, setSearchResult] = useState(null);
    const [isSearching, setIsSearching] = useState(false);
    const [searchError, setSearchError] = useState('');
    const [selectedDeckType, setSelectedDeckType] = useState('MAIN');
    const [isAddingCard, setIsAddingCard] = useState(false);

    // Scanner state
    const [isScanning, setIsScanning] = useState(false);
    const [scanError, setScanError] = useState('');

    // Validation state
    const [validation, setValidation] = useState(null);

    useEffect(() => {
        fetchDeck();
    }, [deckId]);

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

    const handleSaveEdit = async () => {
        if (!editName.trim()) return;

        setIsSaving(true);
        try {
            const updated = await apiService.updateDeck(deckId, editName, editDescription, editIsPublic);
            setDeck(updated);
            setIsEditing(false);
            setSuccessMessage('Deck updated successfully!');
            setTimeout(() => setSuccessMessage(''), 3000);
        } catch (err) {
            setError('Failed to update deck');
        } finally {
            setIsSaving(false);
        }
    };

    const handleSearch = async () => {
        if (!searchTerm.trim()) return;

        setIsSearching(true);
        setSearchError('');
        setSearchResult(null);

        try {
            const result = await apiService.searchCard(searchTerm);
            if (result) {
                setSearchResult(result);
            } else {
                setSearchError('Card not found');
            }
        } catch (err) {
            setSearchError('Search failed. Please try again.');
        } finally {
            setIsSearching(false);
        }
    };

    const handleScan = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setIsScanning(true);
        setScanError('');
        setSearchResult(null);

        try {
            const result = await apiService.uploadCard(file);
            if (result) {
                setSearchResult(result);
            } else {
                setScanError('Could not identify the card');
            }
        } catch (err) {
            setScanError('Scan failed. Please try again.');
        } finally {
            setIsScanning(false);
            e.target.value = '';
        }
    };

    const handleAddCard = async () => {
        if (!searchResult) return;

        setIsAddingCard(true);
        try {
            const updated = await apiService.addCardToDeck(deckId, searchResult.id, 1, selectedDeckType);
            setDeck(updated);
            setSuccessMessage(`Added "${searchResult.name}" to ${selectedDeckType.toLowerCase()} deck!`);
            closeAddCardModal();
            setTimeout(() => setSuccessMessage(''), 3000);
        } catch (err) {
            setSearchError(err.message || 'Failed to add card');
        } finally {
            setIsAddingCard(false);
        }
    };

    const handleRemoveCard = async (cardId, deckType, quantity = null) => {
        try {
            const updated = await apiService.removeCardFromDeck(deckId, cardId, deckType, quantity);
            setDeck(updated);
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

    const openAddCardModal = () => {
        setShowAddCard(true);
        setAddMode('search');
        setSearchTerm('');
        setSearchResult(null);
        setSearchError('');
        setScanError('');
        setSelectedDeckType('MAIN');
    };

    const closeAddCardModal = () => {
        setShowAddCard(false);
        setSearchResult(null);
        setSearchTerm('');
        setSearchError('');
        setScanError('');
    };

    // Group cards by their info
    const groupCards = (cards) => {
        if (!cards) return [];
        const grouped = {};
        cards.forEach((card) => {
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
                    <button onClick={() => navigate('/decks')}>Back to Decks</button>
                </div>
            </div>
        );
    }

    return (
        <div className="scanner-background">
            <div className="scanner-pattern" />
            <Header />

            <main className="main-container deck-builder-page">
                {/* Header */}
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
                    <div className="error-message" onClick={() => setError('')}>
                        <AlertCircle size={18} />
                        <span>{error}</span>
                        <X size={16} className="close-icon" />
                    </div>
                )}

                {successMessage && (
                    <div className="success-message" onClick={() => setSuccessMessage('')}>
                        <CheckCircle size={18} />
                        <span>{successMessage}</span>
                        <X size={16} className="close-icon" />
                    </div>
                )}

                {/* Deck Info */}
                <div className="deck-info-section">
                    {isEditing ? (
                        <div className="deck-info-edit">
                            <input
                                type="text"
                                value={editName}
                                onChange={(e) => setEditName(e.target.value)}
                                className="deck-name-input"
                                placeholder="Deck name"
                            />
                            <textarea
                                value={editDescription}
                                onChange={(e) => setEditDescription(e.target.value)}
                                className="deck-desc-input"
                                placeholder="Description (optional)"
                                rows={2}
                            />
                            <label className="public-toggle">
                                <input
                                    type="checkbox"
                                    checked={editIsPublic}
                                    onChange={(e) => setEditIsPublic(e.target.checked)}
                                />
                                <span>Public deck</span>
                            </label>
                            <div className="edit-actions">
                                <button className="cancel-btn" onClick={() => setIsEditing(false)} disabled={isSaving}>
                                    Cancel
                                </button>
                                <button className="save-btn" onClick={handleSaveEdit} disabled={isSaving}>
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

                {/* Card Lists - FIXED: Using correct property names */}
                <div className="deck-sections">
                    <DeckSection
                        title="Main Deck"
                        cards={groupCards(deck.mainDeck)}
                        deckType="MAIN"
                        onRemove={handleRemoveCard}
                    />
                    <DeckSection
                        title="Extra Deck"
                        cards={groupCards(deck.extraDeck)}
                        deckType="EXTRA"
                        onRemove={handleRemoveCard}
                    />
                    <DeckSection
                        title="Side Deck"
                        cards={groupCards(deck.sideDeck)}
                        deckType="SIDE"
                        onRemove={handleRemoveCard}
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
                                <Upload size={18} />
                                <span>Scan</span>
                            </button>
                        </div>

                        {/* Search Mode */}
                        {addMode === 'search' && (
                            <div className="search-card-section">
                                <div className="modal-search-bar">
                                    <input
                                        type="text"
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                        onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                                        placeholder="Enter exact card name"
                                        disabled={isSearching}
                                    />
                                    <button onClick={handleSearch} disabled={isSearching || !searchTerm.trim()}>
                                        {isSearching ? <Loader2 className="spin" size={18} /> : <Search size={18} />}
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* Scan Mode */}
                        {addMode === 'scan' && (
                            <div className="scan-card-section">
                                <input
                                    type="file"
                                    accept="image/*"
                                    onChange={handleScan}
                                    id="scan-upload"
                                    className="scan-upload-input"
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

// Deck Section Component
const DeckSection = ({ title, cards, deckType, onRemove }) => {
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
                        <img src={card.imageUrl} alt={card.name} />
                        <div className="card-quantity">{card.quantity}x</div>
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
                        <div className="card-name-tooltip">{card.name}</div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default DeckBuilderPage;