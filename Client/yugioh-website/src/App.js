import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

// Pages
import YuGiOhScanner from './YuGiOhScanner';
import LoginPage from './components/LoginPage';
import RegisterPage from './components/RegisterPage';
import ForgotPasswordPage from './components/ForgotPasswordPage';
import ResetPasswordPage from './components/ResetPasswordPage';
import ForgotUsernamePage from './components/ForgotUsernamePage';
import MyDecksPage from './components/MyDecksPage';
import DeckBuilderPage from './components/DeckBuilderPage';

// Styles - Import global.css for DuelDiskScan theme
import './styles/global.css';

function App() {
    return (
        <AuthProvider>
            <Router>
                <Routes>
                    {/* Public Routes */}
                    <Route path="/" element={<YuGiOhScanner />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                    <Route path="/reset-password" element={<ResetPasswordPage />} />
                    <Route path="/forgot-username" element={<ForgotUsernamePage />} />

                    {/* Protected Routes */}
                    <Route
                        path="/decks"
                        element={
                            <ProtectedRoute>
                                <MyDecksPage />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/decks/:deckId"
                        element={
                            <ProtectedRoute>
                                <DeckBuilderPage />
                            </ProtectedRoute>
                        }
                    />
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;