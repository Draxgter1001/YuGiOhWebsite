import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Sparkles, Book, User, LogOut, LogIn, Menu, X } from 'lucide-react';

const Header = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
    setShowUserMenu(false);
  };

  return (
      <header className="header">
        <div className="header-container">
          {/* Logo */}
          <Link to="/" className="header-logo">
            <div className="logo-icon">
              <Sparkles style={{ width: '28px', height: '28px', color: '#111827' }} />
            </div>
            <h1 className="header-title">Yu-Gi-Oh! Scanner</h1>
          </Link>

          {/* Desktop Navigation */}
          <nav className="header-nav desktop-nav">
            <Link to="/" className="nav-link">
              Scanner
            </Link>
            {isAuthenticated && (
                <Link to="/decks" className="nav-link">
                  <Book size={18} />
                  <span>My Decks</span>
                </Link>
            )}
          </nav>

          {/* Auth Section */}
          <div className="header-auth">
            {isAuthenticated ? (
                <div className="user-menu-container">
                  <button
                      className="user-menu-btn"
                      onClick={() => setShowUserMenu(!showUserMenu)}
                  >
                    <User size={20} />
                    <span className="username">{user?.username}</span>
                  </button>

                  {showUserMenu && (
                      <div className="user-dropdown">
                        <Link to="/decks" className="dropdown-item" onClick={() => setShowUserMenu(false)}>
                          <Book size={16} />
                          <span>My Decks</span>
                        </Link>
                        <button className="dropdown-item logout" onClick={handleLogout}>
                          <LogOut size={16} />
                          <span>Logout</span>
                        </button>
                      </div>
                  )}
                </div>
            ) : (
                <div className="auth-buttons">
                  <Link to="/login" className="login-btn">
                    <LogIn size={18} />
                    <span>Login</span>
                  </Link>
                  <Link to="/register" className="register-btn">
                    Sign Up
                  </Link>
                </div>
            )}

            {/* Mobile Menu Toggle */}
            <button
                className="mobile-menu-toggle"
                onClick={() => setShowMobileMenu(!showMobileMenu)}
            >
              {showMobileMenu ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {showMobileMenu && (
            <div className="mobile-menu">
              <Link to="/" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>
                Scanner
              </Link>
              {isAuthenticated ? (
                  <>
                    <Link to="/decks" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>
                      My Decks
                    </Link>
                    <button className="mobile-nav-link logout" onClick={handleLogout}>
                      Logout
                    </button>
                  </>
              ) : (
                  <>
                    <Link to="/login" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>
                      Login
                    </Link>
                    <Link to="/register" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>
                      Sign Up
                    </Link>
                  </>
              )}
            </div>
        )}
      </header>
  );
};

export default Header;