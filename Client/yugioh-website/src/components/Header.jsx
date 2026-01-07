import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Disc, Book, User, LogOut, LogIn, Menu, X } from 'lucide-react';

const Header = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
    setShowUserMenu(false);
    setShowMobileMenu(false);
  };

  const closeMobileMenu = () => setShowMobileMenu(false);
  const closeUserMenu = () => setShowUserMenu(false);

  return (
      <header className="header">
        <div className="header-container">
          {/* Logo */}
          <Link to="/" className="header-logo" onClick={closeMobileMenu}>
            <div className="logo-icon">
              <Disc size={28} />
            </div>
            <h1 className="header-title">
              DuelDiskScan<span className="gg">.gg</span>
            </h1>
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
                      aria-expanded={showUserMenu}
                      aria-haspopup="true"
                  >
                    <User size={20} />
                    <span className="username">{user?.username}</span>
                  </button>

                  {showUserMenu && (
                      <div className="user-dropdown">
                        <Link
                            to="/decks"
                            className="dropdown-item"
                            onClick={closeUserMenu}
                        >
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
                aria-expanded={showMobileMenu}
                aria-label="Toggle menu"
            >
              {showMobileMenu ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {showMobileMenu && (
            <nav className="mobile-menu">
              <Link to="/" className="mobile-nav-link" onClick={closeMobileMenu}>
                Scanner
              </Link>
              {isAuthenticated ? (
                  <>
                    <Link to="/decks" className="mobile-nav-link" onClick={closeMobileMenu}>
                      My Decks
                    </Link>
                    <button className="mobile-nav-link logout" onClick={handleLogout}>
                      Logout
                    </button>
                  </>
              ) : (
                  <>
                    <Link to="/login" className="mobile-nav-link" onClick={closeMobileMenu}>
                      Login
                    </Link>
                    <Link to="/register" className="mobile-nav-link" onClick={closeMobileMenu}>
                      Sign Up
                    </Link>
                  </>
              )}
            </nav>
        )}
      </header>
  );
};

export default Header;