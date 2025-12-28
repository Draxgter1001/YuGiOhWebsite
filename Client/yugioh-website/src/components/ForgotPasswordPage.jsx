import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { apiService } from '../services/api';
import { Sparkles, Loader2, Mail, ArrowLeft, CheckCircle } from 'lucide-react';

const ForgotPasswordPage = () => {
    const [email, setEmail] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const result = await apiService.forgotPassword(email);

            if (result.success) {
                setSuccess(true);
            } else {
                setError(result.message || 'Failed to send reset email. Please try again.');
            }
        } catch (err) {
            setError('An error occurred. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    // Success state
    if (success) {
        return (
            <div className="auth-page">
                <div className="auth-container">
                    <div className="auth-header">
                        <Link to="/" className="auth-logo-link">
                            <div className="auth-logo success-logo">
                                <CheckCircle size={32} />
                            </div>
                        </Link>
                        <h1>Check Your Email</h1>
                        <p>We've sent a password reset link to your email address.</p>
                    </div>

                    <div className="success-info">
                        <p>If an account exists with <strong>{email}</strong>, you will receive an email with instructions to reset your password.</p>
                        <p className="info-note">The link will expire in 1 hour.</p>
                    </div>

                    <div className="auth-footer">
                        <p>
                            <Link to="/login">
                                <ArrowLeft size={16} style={{ verticalAlign: 'middle', marginRight: '0.25rem' }} />
                                Back to Login
                            </Link>
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="auth-page">
            <div className="auth-container">
                <div className="auth-header">
                    <Link to="/" className="auth-logo-link">
                        <div className="auth-logo">
                            <Sparkles size={32} />
                        </div>
                    </Link>
                    <h1>Forgot Password?</h1>
                    <p>Enter your email and we'll send you a reset link</p>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    {error && <div className="auth-error">{error}</div>}

                    <div className="form-group">
                        <label htmlFor="email">Email Address</label>
                        <div className="input-with-icon">
                            <Mail size={18} className="input-icon" />
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value);
                                    setError('');
                                }}
                                placeholder="Enter your email address"
                                required
                                disabled={isLoading}
                            />
                        </div>
                    </div>

                    <button type="submit" className="auth-submit-btn" disabled={isLoading}>
                        {isLoading ? (
                            <>
                                <Loader2 className="spin" size={20} />
                                Sending...
                            </>
                        ) : (
                            'Send Reset Link'
                        )}
                    </button>
                </form>

                <div className="auth-footer">
                    <p>
                        Remember your password? <Link to="/login">Sign in</Link>
                    </p>
                    <p>
                        <Link to="/forgot-username">Forgot your username?</Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default ForgotPasswordPage;