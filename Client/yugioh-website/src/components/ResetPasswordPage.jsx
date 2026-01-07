import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { apiService } from '../services/api';
import { Disc, Loader2, Eye, EyeOff, Check, X, CheckCircle, AlertCircle, Lock } from 'lucide-react';

const ResetPasswordPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');

    const [formData, setFormData] = useState({
        password: '',
        confirmPassword: '',
    });
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [isValidating, setIsValidating] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);
    const [tokenValid, setTokenValid] = useState(false);

    // Password requirements
    const passwordChecks = {
        length: formData.password.length >= 8,
        uppercase: /[A-Z]/.test(formData.password),
        lowercase: /[a-z]/.test(formData.password),
        number: /[0-9]/.test(formData.password),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(formData.password),
    };

    const isPasswordValid = Object.values(passwordChecks).every(Boolean);
    const passwordsMatch = formData.password === formData.confirmPassword;

    // Validate token on mount
    useEffect(() => {
        const validateToken = async () => {
            if (!token) {
                setTokenValid(false);
                setIsValidating(false);
                return;
            }

            try {
                const result = await apiService.verifyResetToken(token);
                setTokenValid(result.success && result.data === true);
            } catch (err) {
                setTokenValid(false);
            } finally {
                setIsValidating(false);
            }
        };

        validateToken();
    }, [token]);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!isPasswordValid) {
            setError('Please meet all password requirements');
            return;
        }

        if (!passwordsMatch) {
            setError('Passwords do not match');
            return;
        }

        setIsLoading(true);

        try {
            const result = await apiService.resetPassword(token, formData.password);

            if (result.success) {
                setSuccess(true);
            } else {
                setError(result.message || 'Failed to reset password. Please try again.');
            }
        } catch (err) {
            setError('An error occurred. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    const PasswordCheck = ({ passed, label }) => (
        <div className={`password-check ${passed ? 'passed' : ''}`}>
            {passed ? <Check size={14} /> : <X size={14} />}
            <span>{label}</span>
        </div>
    );

    // Loading state while validating token
    if (isValidating) {
        return (
            <div className="auth-page">
                <div className="auth-container">
                    <div className="loading-container" style={{ minHeight: 'auto', padding: '2rem' }}>
                        <Loader2 className="spin" size={40} />
                        <p>Validating reset link...</p>
                    </div>
                </div>
            </div>
        );
    }

    // Invalid or missing token
    if (!tokenValid) {
        return (
            <div className="auth-page">
                <div className="auth-container">
                    <div className="auth-header">
                        <Link to="/" className="auth-logo-link">
                            <div className="auth-logo error-logo">
                                <AlertCircle size={32} />
                            </div>
                        </Link>
                        <h1>Invalid Reset Link</h1>
                        <p>This password reset link is invalid or has expired.</p>
                    </div>

                    <div className="error-info">
                        <p>Password reset links expire after 1 hour for security reasons.</p>
                        <p>Please request a new password reset link.</p>
                    </div>

                    <Link to="/forgot-password" className="auth-submit-btn" style={{ textDecoration: 'none', display: 'flex', justifyContent: 'center' }}>
                        Request New Reset Link
                    </Link>

                    <div className="auth-footer">
                        <p>
                            <Link to="/login">Back to Login</Link>
                        </p>
                    </div>
                </div>
            </div>
        );
    }

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
                        <h1>Password Reset!</h1>
                        <p>Your password has been successfully changed.</p>
                    </div>

                    <div className="success-info">
                        <p>You can now log in with your new password.</p>
                    </div>

                    <button
                        onClick={() => navigate('/login')}
                        className="auth-submit-btn"
                    >
                        Go to Login
                    </button>
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
                            <Disc size={32} />
                        </div>
                    </Link>
                    <h1>Reset Password</h1>
                    <p>Enter your new password below</p>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    {error && <div className="auth-error">{error}</div>}

                    <div className="form-group">
                        <label htmlFor="password">New Password</label>
                        <div className="input-with-icon password-input-wrapper">
                            <Lock size={18} className="input-icon" />
                            <input
                                type={showPassword ? 'text' : 'password'}
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder="Enter new password"
                                required
                                disabled={isLoading}
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label={showPassword ? 'Hide password' : 'Show password'}
                            >
                                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                            </button>
                        </div>

                        {formData.password && (
                            <div className="password-requirements">
                                <PasswordCheck passed={passwordChecks.length} label="At least 8 characters" />
                                <PasswordCheck passed={passwordChecks.uppercase} label="One uppercase letter" />
                                <PasswordCheck passed={passwordChecks.lowercase} label="One lowercase letter" />
                                <PasswordCheck passed={passwordChecks.number} label="One number" />
                                <PasswordCheck passed={passwordChecks.special} label="One special character" />
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label htmlFor="confirmPassword">Confirm New Password</label>
                        <div className="input-with-icon password-input-wrapper">
                            <Lock size={18} className="input-icon" />
                            <input
                                type={showPassword ? 'text' : 'password'}
                                id="confirmPassword"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="Confirm new password"
                                required
                                disabled={isLoading}
                            />
                        </div>
                        {formData.confirmPassword && !passwordsMatch && (
                            <span className="field-error">Passwords do not match</span>
                        )}
                    </div>

                    <button
                        type="submit"
                        className="auth-submit-btn"
                        disabled={isLoading || !isPasswordValid || !passwordsMatch}
                    >
                        {isLoading ? (
                            <>
                                <Loader2 className="spin" size={20} />
                                <span>Resetting...</span>
                            </>
                        ) : (
                            'Reset Password'
                        )}
                    </button>
                </form>

                <div className="auth-footer">
                    <p>
                        <Link to="/login">Back to Login</Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default ResetPasswordPage;