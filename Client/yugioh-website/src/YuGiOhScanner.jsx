import React, { useState, useEffect, useRef } from 'react';
import {
    ChevronUp,
    Plus,
    Loader2,
    X,
    CheckCircle,
    HelpCircle,
    ExternalLink,
    Eye
} from 'lucide-react';
import { useAuth } from './context/AuthContext';
import { apiService } from './services/api';
import Header from './components/Header';
import CardUploader from './components/CardUploader';
import SearchBar from './components/SearchBar';

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

    // Card detail modal state
    const [showCardDetail, setShowCardDetail] = useState(false);
    const [detailCard, setDetailCard] = useState(null);

    const resultSectionRef = useRef(null);

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

    const closeAddToDeck = () => {
        setShowAddToDeck(false);
        setCardToAdd(null);
        setAddError('');
        setAddSuccess('');
    };

    // Card detail modal
    const openCardDetail = (card) => {
        setDetailCard(card);
        setShowCardDetail(true);
    };

    const closeCardDetail = () => {
        setShowCardDetail(false);
        setDetailCard(null);
    };

    // Scroll handlers
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
        if (currentCard && resultSectionRef.current) {
            resultSectionRef.current.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
            });
        }
    }, [currentCard]);

    const scrollToTop = () => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    // Format price
    const formatPrice = (price, currency = '$') => {
        if (!price || price === '0.00' || price === 0) return null;
        const num = typeof price === 'string' ? parseFloat(price) : price;
        return `${currency}${num.toFixed(2)}`;
    };

    // Get marketplace URL
    const getMarketplaceUrl = (cardName, marketplace) => {
        const encoded = encodeURIComponent(cardName);
        switch (marketplace) {
            case 'tcgplayer':
                return `https://www.tcgplayer.com/search/yugioh/product?productLineName=yugioh&q=${encoded}`;
            case 'cardmarket':
                return `https://www.cardmarket.com/en/YuGiOh/Products/Search?searchString=${encoded}`;
            case 'coolstuffinc':
                return `https://www.coolstuffinc.com/main_search.php?pa=searchOnName&page=1&resultsPerPage=25&q=${encoded}`;
            default:
                return '#';
        }
    };

    return (
        <div className="page-background">
            <Header />

            <main className="main-container">
                {/* Hero Section */}
                <section className="hero-section">
                    <h1 className="hero-title">Scan & Identify Your Cards</h1>
                    <p className="hero-subtitle">
                        Upload a card image or search by name to instantly identify Yu-Gi-Oh! cards and check market prices.
                    </p>
                </section>

                {/* How It Works Guide */}
                <section className="guide-section">
                    <h2 className="guide-title">
                        <HelpCircle size={18} />
                        How It Works
                    </h2>
                    <div className="guide-steps">
                        <div className="guide-step">
                            <div className="step-number">1</div>
                            <p className="step-text">Upload a card image or search by name</p>
                        </div>
                        <div className="guide-step">
                            <div className="step-number">2</div>
                            <p className="step-text">View card details and market prices</p>
                        </div>
                        <div className="guide-step">
                            <div className="step-number">3</div>
                            <p className="step-text">Add cards to your decks (login required)</p>
                        </div>
                    </div>
                </section>

                {/* Scanner Section */}
                <section className="scanner-section">
                    <h2 className="section-title">Upload Card Image</h2>
                    <CardUploader onCardFound={handleCardFound} />
                </section>

                {/* Divider */}
                <div className="divider">
                    <div className="divider-line" />
                    <span className="divider-text">OR</span>
                    <div className="divider-line" />
                </div>

                {/* Search Section */}
                <section className="search-section">
                    <h2 className="section-title">Search by Name</h2>
                    <SearchBar onSearch={handleCardFound} />
                </section>

                {/* Result Section */}
                {currentCard && (
                    <section ref={resultSectionRef} className="result-section">
                        <h2 className="section-title">Identified Card</h2>
                        <div className="card-result">
                            <div className="card-result-content">
                                {/* Card Image */}
                                <div className="card-image-wrapper">
                                    <img
                                        src={currentCard.imageUrl}
                                        alt={currentCard.name}
                                        className="card-image"
                                        onClick={() => openCardDetail(currentCard)}
                                    />
                                </div>

                                {/* Card Info */}
                                <div className="card-info">
                                    <h3 className="card-name">{currentCard.name}</h3>

                                    <div className="card-meta">
                                        {currentCard.type && <span className="card-tag">{currentCard.type}</span>}
                                        {currentCard.race && <span className="card-tag">{currentCard.race}</span>}
                                        {currentCard.attribute && <span className="card-tag">{currentCard.attribute}</span>}
                                    </div>

                                    {(currentCard.atk !== null || currentCard.def !== null) && (
                                        <div className="card-stats">
                                            {currentCard.atk !== null && (
                                                <span className="stat">
                          <span className="stat-label">ATK</span> {currentCard.atk}
                        </span>
                                            )}
                                            {currentCard.def !== null && (
                                                <span className="stat">
                          <span className="stat-label">DEF</span> {currentCard.def}
                        </span>
                                            )}
                                            {currentCard.level && (
                                                <span className="stat">
                          <span className="stat-label">Level</span> {currentCard.level}
                        </span>
                                            )}
                                        </div>
                                    )}

                                    {currentCard.desc && (
                                        <p className="card-description">{currentCard.desc}</p>
                                    )}

                                    {/* Prices */}
                                    {currentCard.prices && (
                                        <div className="price-section">
                                            <div className="price-title">Market Prices</div>
                                            <div className="price-badges">
                                                {formatPrice(currentCard.prices.tcgplayerPrice) && (
                                                    <a
                                                        href={getMarketplaceUrl(currentCard.name, 'tcgplayer')}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="price-badge"
                                                    >
                                                        <span className="marketplace">TCGPlayer</span>
                                                        <span className="price">{formatPrice(currentCard.prices.tcgplayerPrice)}</span>
                                                        <ExternalLink size={12} />
                                                    </a>
                                                )}
                                                {formatPrice(currentCard.prices.cardmarketPrice, '€') && (
                                                    <a
                                                        href={getMarketplaceUrl(currentCard.name, 'cardmarket')}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="price-badge"
                                                    >
                                                        <span className="marketplace">Cardmarket</span>
                                                        <span className="price">{formatPrice(currentCard.prices.cardmarketPrice, '€')}</span>
                                                        <ExternalLink size={12} />
                                                    </a>
                                                )}
                                                {formatPrice(currentCard.prices.coolstuffincPrice) && (
                                                    <a
                                                        href={getMarketplaceUrl(currentCard.name, 'coolstuffinc')}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="price-badge"
                                                    >
                                                        <span className="marketplace">CoolStuffInc</span>
                                                        <span className="price">{formatPrice(currentCard.prices.coolstuffincPrice)}</span>
                                                        <ExternalLink size={12} />
                                                    </a>
                                                )}
                                            </div>
                                        </div>
                                    )}

                                    {/* Actions */}
                                    <div className="card-actions">
                                        {isAuthenticated && (
                                            <button
                                                className="add-to-deck-btn"
                                                onClick={() => openAddToDeck(currentCard)}
                                            >
                                                <Plus size={18} />
                                                Add to Deck
                                            </button>
                                        )}
                                        <button
                                            className="view-details-btn"
                                            onClick={() => openCardDetail(currentCard)}
                                        >
                                            <Eye size={18} />
                                            View Details
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
                )}

                {/* Recent Scans */}
                {recentCards.length > 0 && (
                    <section className="recent-section">
                        <h2 className="section-title">Recent Scans</h2>
                        <div className="recent-grid">
                            {recentCards.map((card) => (
                                <div
                                    key={card.id}
                                    className="recent-card"
                                    onClick={() => openCardDetail(card)}
                                >
                                    <img src={card.imageUrl} alt={card.name} />
                                    <div className="recent-card-overlay">
                                        <span className="recent-card-name">{card.name}</span>
                                    </div>
                                    {isAuthenticated && (
                                        <button
                                            className="recent-add-btn"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                openAddToDeck(card);
                                            }}
                                            title="Add to Deck"
                                        >
                                            <Plus size={14} />
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    </section>
                )}

                {/* Footer */}
                <footer className="footer">
                    <p>
                        DuelDiskScan.gg is not affiliated with Konami.
                        Card data from <a href="https://ygoprodeck.com/" target="_blank" rel="noopener noreferrer">YGOPRODeck</a>.
                    </p>
                </footer>
            </main>

            {/* Back to Top Button */}
            <button
                className={`back-to-top-btn ${showBackToTop ? 'visible' : ''}`}
                onClick={scrollToTop}
                aria-label="Back to top"
            >
                <ChevronUp size={24} />
            </button>

            {/* Add to Deck Modal */}
            {showAddToDeck && (
                <div className="modal-overlay" onClick={closeAddToDeck}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close-btn" onClick={closeAddToDeck}>
                            <X size={20} />
                        </button>

                        <div className="modal-header">
                            <h3 className="modal-title">Add to Deck</h3>
                        </div>

                        <div className="modal-body">
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
                                    <a href="/decks">Create a deck first</a>
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
                </div>
            )}

            {/* Card Detail Modal */}
            {showCardDetail && detailCard && (
                <div className="modal-overlay detail-modal" onClick={closeCardDetail}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close-btn" onClick={closeCardDetail}>
                            <X size={24} />
                        </button>

                        <div className="card-detail-content">
                            {/* Card Image */}
                            <img
                                src={detailCard.imageUrl}
                                alt={detailCard.name}
                                className="card-detail-image"
                            />

                            {/* Card Info */}
                            <div className="card-detail-info">
                                <h2 className="card-detail-name">{detailCard.name}</h2>

                                <div className="card-detail-meta">
                                    {detailCard.type && <span className="card-tag">{detailCard.type}</span>}
                                    {detailCard.race && <span className="card-tag">{detailCard.race}</span>}
                                    {detailCard.attribute && <span className="card-tag">{detailCard.attribute}</span>}
                                </div>

                                {(detailCard.atk !== null || detailCard.def !== null || detailCard.level) && (
                                    <div className="card-detail-stats">
                                        {detailCard.atk !== null && (
                                            <div className="stat">
                                                <div className="stat-value">{detailCard.atk}</div>
                                                <div className="stat-label">ATK</div>
                                            </div>
                                        )}
                                        {detailCard.def !== null && (
                                            <div className="stat">
                                                <div className="stat-value">{detailCard.def}</div>
                                                <div className="stat-label">DEF</div>
                                            </div>
                                        )}
                                        {detailCard.level && (
                                            <div className="stat">
                                                <div className="stat-value">{detailCard.level}</div>
                                                <div className="stat-label">Level</div>
                                            </div>
                                        )}
                                    </div>
                                )}

                                {detailCard.desc && (
                                    <p className="card-detail-desc">{detailCard.desc}</p>
                                )}

                                {/* Prices */}
                                {detailCard.prices && (
                                    <div className="price-section">
                                        <div className="price-title">Market Prices</div>
                                        <div className="price-badges">
                                            {formatPrice(detailCard.prices.tcgplayerPrice) && (
                                                <a
                                                    href={getMarketplaceUrl(detailCard.name, 'tcgplayer')}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="price-badge"
                                                >
                                                    <span className="marketplace">TCGPlayer</span>
                                                    <span className="price">{formatPrice(detailCard.prices.tcgplayerPrice)}</span>
                                                    <ExternalLink size={12} />
                                                </a>
                                            )}
                                            {formatPrice(detailCard.prices.cardmarketPrice, '€') && (
                                                <a
                                                    href={getMarketplaceUrl(detailCard.name, 'cardmarket')}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="price-badge"
                                                >
                                                    <span className="marketplace">Cardmarket</span>
                                                    <span className="price">{formatPrice(detailCard.prices.cardmarketPrice, '€')}</span>
                                                    <ExternalLink size={12} />
                                                </a>
                                            )}
                                            {formatPrice(detailCard.prices.coolstuffincPrice) && (
                                                <a
                                                    href={getMarketplaceUrl(detailCard.name, 'coolstuffinc')}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="price-badge"
                                                >
                                                    <span className="marketplace">CoolStuffInc</span>
                                                    <span className="price">{formatPrice(detailCard.prices.coolstuffincPrice)}</span>
                                                    <ExternalLink size={12} />
                                                </a>
                                            )}
                                        </div>
                                    </div>
                                )}

                                {/* Actions */}
                                {isAuthenticated && (
                                    <div className="card-actions">
                                        <button
                                            className="add-to-deck-btn"
                                            onClick={() => {
                                                closeCardDetail();
                                                openAddToDeck(detailCard);
                                            }}
                                        >
                                            <Plus size={18} />
                                            Add to Deck
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default YuGiOhScanner;