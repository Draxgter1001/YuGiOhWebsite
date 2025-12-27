import React, { useState, useEffect, useRef } from 'react';
import { ChevronUp, Plus, Loader2, X, CheckCircle } from 'lucide-react';
import { useAuth } from './context/AuthContext';
import { apiService } from './services/api';
import Header from './components/Header';
import CardUploader from './components/CardUploader';
import SearchBar from './components/SearchBar';
import YuGiOhCard from './components/YuGiOhCard';

const YuGiOhScanner = () => {
    const { isAuthenticated } = useAuth();

    const [currentCard, setCurrentCard] = useState(null);
    const [recentCards, setRecentCards] = useState([]);
    const [showBackToTop, setShowBackToTop] = useState(false);

    // Add to deck modal state
    const [showAddToDeck, setShowAddToDeck] = useState(false);
    const [cardToAdd, setCardToAdd] = useState(null);
    const [userDecks, setUserDecks] = useState([]);
    const [selectedDeckId, setSelectedDeckId] = useState('');
    const [selectedDeckType, setSelectedDeckType] = useState('MAIN');
    const [isLoadingDecks, setIsLoadingDecks] = useState(false);
    const [isAddingCard, setIsAddingCard] = useState(false);
    const [addSuccess, setAddSuccess] = useState('');
    const [addError, setAddError] = useState('');

    const identifiedSectionRef = useRef(null);

    const handleCardFound = (card) => {
        setCurrentCard(card);
        setRecentCards((prev) => {
            const filtered = prev.filter((c) => c.id !== card.id);
            return [card, ...filtered].slice(0, 10);
        });
    };

    // Open add to deck modal
    const openAddToDeck = async (card) => {
        if (!isAuthenticated) return;

        setCardToAdd(card);
        setShowAddToDeck(true);
        setAddSuccess('');
        setAddError('');
        setSelectedDeckId('');
        setSelectedDeckType('MAIN');

        // Fetch user's decks
        setIsLoadingDecks(true);
        try {
            const decks = await apiService.getMyDecks();
            setUserDecks(decks);
            if (decks.length > 0) {
                setSelectedDeckId(decks[0].id.toString());
            }
        } catch (err) {
            setAddError('Failed to load decks');
        } finally {
            setIsLoadingDecks(false);
        }
    };

    // Handle adding card to deck
    const handleAddToDeck = async () => {
        if (!selectedDeckId || !cardToAdd) return;

        setIsAddingCard(true);
        setAddError('');

        try {
            await apiService.addCardToDeck(
                selectedDeckId,
                cardToAdd.id,
                1,
                selectedDeckType
            );

            const deckName = userDecks.find(d => d.id.toString() === selectedDeckId)?.name;
            setAddSuccess(`Added "${cardToAdd.name}" to ${deckName}!`);

            // Auto close after success
            setTimeout(() => {
                setShowAddToDeck(false);
                setAddSuccess('');
            }, 2000);
        } catch (err) {
            setAddError(err.message || 'Failed to add card to deck');
        } finally {
            setIsAddingCard(false);
        }
    };

    // Close modal
    const closeAddToDeck = () => {
        setShowAddToDeck(false);
        setCardToAdd(null);
        setAddError('');
        setAddSuccess('');
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
                            <YuGiOhCard card={currentCard} zoomable={true} />
                        </div>

                        {/* Add to Deck Button */}
                        {isAuthenticated && (
                            <div className="add-to-deck-action">
                                <button
                                    className="add-to-deck-btn"
                                    onClick={() => openAddToDeck(currentCard)}
                                >
                                    <Plus size={20} />
                                    <span>Add to Deck</span>
                                </button>
                            </div>
                        )}
                    </div>
                )}

                {recentCards.length > 0 && (
                    <div className="recent-section">
                        <h3 className="section-title">Recent Scans</h3>
                        <div
                            className={`recent-cards-container ${
                                recentCards.length > 4 ? 'vertical-layout' : ''
                            }`}
                        >
                            {recentCards.map((card) => (
                                <div key={card.id} className="recent-card-wrapper">
                                    <YuGiOhCard card={card} zoomable={true} />
                                    {/* Add to Deck for recent cards */}
                                    {isAuthenticated && (
                                        <button
                                            className="recent-add-btn"
                                            onClick={() => openAddToDeck(card)}
                                            title="Add to Deck"
                                        >
                                            <Plus size={16} />
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </main>

            <button
                className={`back-to-top-btn ${showBackToTop ? 'visible' : ''}`}
                onClick={scrollToTop}
            >
                <ChevronUp size={24} />
            </button>

            {/* Add to Deck Modal */}
            {showAddToDeck && (
                <div className="modal-overlay" onClick={closeAddToDeck}>
                    <div className="modal-content add-to-deck-modal" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close-btn" onClick={closeAddToDeck}>
                            <X size={20} />
                        </button>

                        <h3>Add to Deck</h3>

                        {/* Card Preview */}
                        {cardToAdd && (
                            <div className="modal-card-preview">
                                <img src={cardToAdd.imageUrl} alt={cardToAdd.name} />
                                <div className="modal-card-info">
                                    <h4>{cardToAdd.name}</h4>
                                    <p>{cardToAdd.type}</p>
                                </div>
                            </div>
                        )}

                        {/* Success Message */}
                        {addSuccess && (
                            <div className="modal-success">
                                <CheckCircle size={18} />
                                <span>{addSuccess}</span>
                            </div>
                        )}

                        {/* Error Message */}
                        {addError && (
                            <div className="modal-error">
                                <span>{addError}</span>
                            </div>
                        )}

                        {/* Loading Decks */}
                        {isLoadingDecks ? (
                            <div className="modal-loading">
                                <Loader2 className="spin" size={24} />
                                <span>Loading your decks...</span>
                            </div>
                        ) : userDecks.length === 0 ? (
                            <div className="modal-no-decks">
                                <p>You don't have any decks yet.</p>
                                <a href="/decks" className="create-deck-link">Create a deck first</a>
                            </div>
                        ) : !addSuccess && (
                            <>
                                {/* Deck Selection */}
                                <div className="form-group">
                                    <label htmlFor="deckSelect">Select Deck</label>
                                    <select
                                        id="deckSelect"
                                        value={selectedDeckId}
                                        onChange={(e) => setSelectedDeckId(e.target.value)}
                                        disabled={isAddingCard}
                                    >
                                        {userDecks.map((deck) => (
                                            <option key={deck.id} value={deck.id}>
                                                {deck.name} (Main: {deck.mainDeckCount || 0}, Extra: {deck.extraDeckCount || 0})
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {/* Deck Type Selection */}
                                <div className="form-group">
                                    <label htmlFor="deckType">Add to</label>
                                    <select
                                        id="deckType"
                                        value={selectedDeckType}
                                        onChange={(e) => setSelectedDeckType(e.target.value)}
                                        disabled={isAddingCard}
                                    >
                                        <option value="MAIN">Main Deck</option>
                                        <option value="EXTRA">Extra Deck</option>
                                        <option value="SIDE">Side Deck</option>
                                    </select>
                                </div>

                                {/* Add Button */}
                                <div className="modal-actions">
                                    <button
                                        className="modal-btn cancel"
                                        onClick={closeAddToDeck}
                                        disabled={isAddingCard}
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        className="modal-btn confirm"
                                        onClick={handleAddToDeck}
                                        disabled={isAddingCard || !selectedDeckId}
                                    >
                                        {isAddingCard ? (
                                            <>
                                                <Loader2 className="spin" size={16} />
                                                Adding...
                                            </>
                                        ) : (
                                            <>
                                                <Plus size={16} />
                                                Add Card
                                            </>
                                        )}
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default YuGiOhScanner;