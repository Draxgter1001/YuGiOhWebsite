import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Loader2 } from 'lucide-react';

const ProtectedRoute = ({ children }) => {
    const { isAuthenticated, loading } = useAuth();

    if (loading) {
        return (
            <div className="loading-page">
                <Loader2 className="spin" size={40} />
                <p>Loading...</p>
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    return children;
};

export default ProtectedRoute;