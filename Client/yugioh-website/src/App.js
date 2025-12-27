import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

// Pages
import YuGiOhScanner from './YuGiOhScanner';
import LoginPage from './components/LoginPage';
import RegisterPage from './components/RegisterPage';
import MyDecksPage from './components/MyDecksPage';
import DeckBuilderPage from './components/DeckBuilderPage';

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