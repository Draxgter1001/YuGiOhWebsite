import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Disc, Loader2, Eye, EyeOff, Check, X, User, Mail, Lock } from 'lucide-react';

const RegisterPage = () => {
    const navigate = useNavigate();
    const { register } = useAuth();

    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const [isLoading, setIsLoading] = useState(false);

    // Password requirements
    const passwordChecks = {
        length: formData.password.length >= 8,
        uppercase: /[A-Z]/.test(formData.password),
        lowercase: /[a-z]/.test(formData.password),
        number: /[0-9]/.test(formData.password),
        special: /[!@#$%^&*(),.?":{}|<>_\-+=\[\]\\;'`~]/.test(formData.password),
    };

    const isPasswordValid = Object.values(passwordChecks).every(Boolean);
    const passwordsMatch = formData.password === formData.confirmPassword;

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setError('');
        setFieldErrors({});
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setFieldErrors({});

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
            const result = await register(
                formData.username,
                formData.email,
                formData.password
            );

            if (result.success) {
                navigate('/');
            } else {
                // Check if there are field-specific validation errors
                if (result.data && typeof result.data === 'object') {
                    setFieldErrors(result.data);
                    // Create a user-friendly error message
                    const errorMessages = Object.entries(result.data)
                        .map(([field, msg]) => `${field}: ${msg}`)
                        .join('\n');
                    setError(errorMessages || result.message || 'Registration failed');
                } else {
                    setError(result.message || 'Registration failed. Please try again.');
                }
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

    return (
        <div className="auth-page">
            <div className="auth-container">
                <div className="auth-header">
                    <Link to="/" className="auth-logo-link">
                        <div className="auth-logo">
                            <Disc size={32} />
                        </div>
                    </Link>
                    <h1>Create Account</h1>
                    <p>Join DuelDiskScan and build your decks</p>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    {error && (
                        <div className="auth-error">
                            {error.split('\n').map((line, i) => (
                                <div key={i}>{line}</div>
                            ))}
                        </div>
                    )}

                    <div className="form-group">
                        <label htmlFor="username">Username</label>
                        <div className="input-with-icon">
                            <User size={18} className="input-icon" />
                            <input
                                type="text"
                                id="username"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                placeholder="Choose a username"
                                required
                                disabled={isLoading}
                                minLength={3}
                                maxLength={20}
                            />
                        </div>
                        {fieldErrors.username && (
                            <span className="field-error">{fieldErrors.username}</span>
                        )}
                        <span className="field-hint">Letters, numbers, and underscores only</span>
                    </div>

                    <div className="form-group">
                        <label htmlFor="email">Email</label>
                        <div className="input-with-icon">
                            <Mail size={18} className="input-icon" />
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="Enter your email"
                                required
                                disabled={isLoading}
                            />
                        </div>
                        {fieldErrors.email && (
                            <span className="field-error">{fieldErrors.email}</span>
                        )}
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <div className="password-input-wrapper">
                            <div className="input-with-icon">
                                <Lock size={18} className="input-icon" />
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    id="password"
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    placeholder="Create a password"
                                    required
                                    disabled={isLoading}
                                />
                            </div>
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label={showPassword ? 'Hide password' : 'Show password'}
                            >
                                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                            </button>
                        </div>
                        {fieldErrors.password && (
                            <span className="field-error">{fieldErrors.password}</span>
                        )}

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
                        <label htmlFor="confirmPassword">Confirm Password</label>
                        <div className="input-with-icon">
                            <Lock size={18} className="input-icon" />
                            <input
                                type={showPassword ? 'text' : 'password'}
                                id="confirmPassword"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="Confirm your password"
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
                                Creating account...
                            </>
                        ) : (
                            'Create Account'
                        )}
                    </button>
                </form>

                <div className="auth-footer">
                    <p>
                        Already have an account? <Link to="/login">Sign in</Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;