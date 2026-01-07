import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiService } from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Check if user is already logged in on mount
    useEffect(() => {
        const checkAuth = async () => {
            if (apiService.isLoggedIn()) {
                try {
                    const isValid = await apiService.validateToken();
                    if (isValid) {
                        const userData = await apiService.getCurrentUser();
                        setUser(userData);
                    } else {
                        apiService.logout();
                    }
                } catch (error) {
                    console.error('Auth check failed:', error);
                    apiService.logout();
                }
            }
            setLoading(false);
        };

        checkAuth();
    }, []);

    const login = async (usernameOrEmail, password) => {
        const response = await apiService.login(usernameOrEmail, password);

        if (response.success) {
            setUser(response.user);
            return { success: true };
        }

        return { success: false, message: response.message };
    };

    const register = async (username, email, password) => {
        const response = await apiService.register(username, email, password);

        if (response.success) {
            setUser(response.user);
            return { success: true };
        }

        // FIX: Pass the data field which contains field-specific validation errors
        return {
            success: false,
            message: response.message,
            data: response.data  // This contains { username: "error", password: "error" }
        };
    };

    const logout = () => {
        apiService.logout();
        setUser(null);
    };

    const value = {
        user,
        loading,
        isAuthenticated: !!user,
        login,
        register,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};