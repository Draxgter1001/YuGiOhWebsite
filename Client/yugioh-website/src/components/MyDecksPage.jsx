import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import {
    Plus,
    Loader2,
    Trash2,
    Edit3,
    Eye,
    Lock,
    Globe,
    AlertCircle,
} from 'lucide-react';
import Header from '../components/Header';

const MyDecksPage = () => {
    const navigate = useNavigate();
    const [decks, setDecks] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    // Create deck modal state
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [newDeckName, setNewDeckName] = useState('');
    const [newDeckDescription, setNewDeckDescription] = useState('');
    const [isCreating, setIsCreating] = useState(false);

    // Delete confirmation state
    const [deckToDelete, setDeckToDelete] = useState(null);
    const [isDeleting, setIsDeleting] = useState(false);

    useEffect(() => {
        fetchDecks();
    }, []);

    const fetchDecks = async () => {
        try {
            setIsLoading(true);
            const data = await apiService.getMyDecks();
            setDecks(data);
        } catch (err) {
            setError('Failed to load decks');
        } finally {
            setIsLoading(false);
        }
    };

    const handleCreateDeck = async (e) => {
        e.preventDefault();
        if (!newDeckName.trim()) return;

        setIsCreating(true);
        try {
            const newDeck = await apiService.createDeck(newDeckName, newDeckDescription);
            setDecks([newDeck, ...decks]);
            setShowCreateModal(false);
            setNewDeckName('');
            setNewDeckDescription('');
            navigate(`/decks/${newDeck.id}`);
        } catch (err) {
            setError('Failed to create deck');
        } finally {
            setIsCreating(false);
        }
    };

    const handleDeleteDeck = async () => {
        if (!deckToDelete) return;

        setIsDeleting(true);
        try {
            await apiService.deleteDeck(deckToDelete.id);
            setDecks(decks.filter((d) => d.id !== deckToDelete.id));
            setDeckToDelete(null);
        } catch (err) {
            setError('Failed to delete deck');
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <div className="scanner-background">
            <div className="scanner-pattern" />
            <Header />

            <main className="main-container">
                <div className="decks-header">
                    <h2 className="section-title">My Decks</h2>
                    <button className="create-deck-btn" onClick={() => setShowCreateModal(true)}>
                        <Plus size={20} />
                        <span>New Deck</span>
                    </button>
                </div>

                {error && (
                    <div className="error-message">
                        <AlertCircle size={20} />
                        <span>{error}</span>
                    </div>
                )}

                {isLoading ? (
                    <div className="loading-container">
                        <Loader2 className="spin" size={40} />
                        <p>Loading your decks...</p>
                    </div>
                ) : decks.length === 0 ? (
                    <div className="empty-decks">
                        <p>You don't have any decks yet.</p>
                        <button className="create-deck-btn" onClick={() => setShowCreateModal(true)}>
                            <Plus size={20} />
                            <span>Create Your First Deck</span>
                        </button>
                    </div>
                ) : (
                    <div className="decks-grid">
                        {decks.map((deck) => (
                            <div key={deck.id} className="deck-card">
                                <div className="deck-card-header">
                                    <h3>{deck.name}</h3>
                                    {deck.isPublic ? (
                                        <Globe size={16} className="public-icon" title="Public" />
                                    ) : (
                                        <Lock size={16} className="private-icon" title="Private" />
                                    )}
                                </div>

                                {deck.description && (
                                    <p className="deck-description">{deck.description}</p>
                                )}

                                <div className="deck-stats">
                                    <span>Main: {deck.mainDeckCount || 0}</span>
                                    <span>Extra: {deck.extraDeckCount || 0}</span>
                                    <span>Side: {deck.sideDeckCount || 0}</span>
                                </div>

                                <div className="deck-card-actions">
                                    <button
                                        className="deck-action-btn view"
                                        onClick={() => navigate(`/decks/${deck.id}`)}
                                    >
                                        <Eye size={16} />
                                        <span>View</span>
                                    </button>
                                    <button
                                        className="deck-action-btn edit"
                                        onClick={() => navigate(`/decks/${deck.id}`)}
                                    >
                                        <Edit3 size={16} />
                                        <span>Edit</span>
                                    </button>
                                    <button
                                        className="deck-action-btn delete"
                                        onClick={() => setDeckToDelete(deck)}
                                    >
                                        <Trash2 size={16} />
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </main>

            {/* Create Deck Modal */}
            {showCreateModal && (
                <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h3>Create New Deck</h3>
                        <form onSubmit={handleCreateDeck}>
                            <div className="form-group">
                                <label htmlFor="deckName">Deck Name</label>
                                <input
                                    type="text"
                                    id="deckName"
                                    value={newDeckName}
                                    onChange={(e) => setNewDeckName(e.target.value)}
                                    placeholder="Enter deck name"
                                    required
                                    maxLength={50}
                                    disabled={isCreating}
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="deckDescription">Description (optional)</label>
                                <textarea
                                    id="deckDescription"
                                    value={newDeckDescription}
                                    onChange={(e) => setNewDeckDescription(e.target.value)}
                                    placeholder="Describe your deck strategy..."
                                    rows={3}
                                    maxLength={200}
                                    disabled={isCreating}
                                />
                            </div>
                            <div className="modal-actions">
                                <button
                                    type="button"
                                    className="modal-btn cancel"
                                    onClick={() => setShowCreateModal(false)}
                                    disabled={isCreating}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="modal-btn confirm"
                                    disabled={isCreating || !newDeckName.trim()}
                                >
                                    {isCreating ? <Loader2 className="spin" size={16} /> : 'Create'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Modal */}
            {deckToDelete && (
                <div className="modal-overlay" onClick={() => setDeckToDelete(null)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h3>Delete Deck</h3>
                        <p>Are you sure you want to delete "{deckToDelete.name}"?</p>
                        <p className="warning-text">This action cannot be undone.</p>
                        <div className="modal-actions">
                            <button
                                className="modal-btn cancel"
                                onClick={() => setDeckToDelete(null)}
                                disabled={isDeleting}
                            >
                                Cancel
                            </button>
                            <button
                                className="modal-btn delete"
                                onClick={handleDeleteDeck}
                                disabled={isDeleting}
                            >
                                {isDeleting ? <Loader2 className="spin" size={16} /> : 'Delete'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MyDecksPage;