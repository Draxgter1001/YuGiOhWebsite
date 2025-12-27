const API_BASE_URL = 'http://localhost:8080/api';

// Token management
let accessToken = localStorage.getItem('accessToken');
let refreshToken = localStorage.getItem('refreshToken');

const setTokens = (access, refresh) => {
  accessToken = access;
  refreshToken = refresh;
  if (access) {
    localStorage.setItem('accessToken', access);
  } else {
    localStorage.removeItem('accessToken');
  }
  if (refresh) {
    localStorage.setItem('refreshToken', refresh);
  } else {
    localStorage.removeItem('refreshToken');
  }
};

const getAccessToken = () => accessToken;

const clearTokens = () => {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};

// Helper for authenticated requests
const authFetch = async (url, options = {}) => {
  const headers = {
    ...options.headers,
  };

  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, { ...options, headers });

  // If unauthorized, try to refresh token
  if (response.status === 401 && refreshToken) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      headers['Authorization'] = `Bearer ${accessToken}`;
      return fetch(url, { ...options, headers });
    }
  }

  return response;
};

const tryRefreshToken = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (response.ok) {
      const data = await response.json();
      setTokens(data.accessToken, data.refreshToken);
      return true;
    }
  } catch (error) {
    console.error('Token refresh failed:', error);
  }

  clearTokens();
  return false;
};

export const apiService = {
  // ============ AUTH ============
  register: async (username, email, password) => {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password }),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      setTokens(data.accessToken, data.refreshToken);
    }

    return data;
  },

  login: async (usernameOrEmail, password) => {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail, password }),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      setTokens(data.accessToken, data.refreshToken);
    }

    return data;
  },

  logout: () => {
    clearTokens();
  },

  getCurrentUser: async () => {
    const response = await authFetch(`${API_BASE_URL}/auth/me`);
    if (!response.ok) return null;
    const data = await response.json();
    return data.data;
  },

  validateToken: async () => {
    if (!accessToken) return false;
    try {
      const response = await authFetch(`${API_BASE_URL}/auth/validate`);
      if (!response.ok) return false;
      const data = await response.json();
      return data.data === true;
    } catch {
      return false;
    }
  },

  // ============ CARDS ============
  uploadCard: async (file) => {
    const formData = new FormData();
    formData.append('image', file);

    const response = await authFetch(`${API_BASE_URL}/cards/upload`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error('Upload failed');
    }

    return response.json();
  },

  searchCard: async (name) => {
    const response = await fetch(
        `${API_BASE_URL}/cards/search?name=${encodeURIComponent(name)}`
    );

    if (!response.ok) {
      throw new Error('Search failed');
    }

    return response.json();
  },

  // ============ DECKS ============
  getMyDecks: async () => {
    const response = await authFetch(`${API_BASE_URL}/decks`);
    if (!response.ok) throw new Error('Failed to fetch decks');
    const data = await response.json();
    return data.data || [];
  },

  getDeck: async (deckId) => {
    const response = await authFetch(`${API_BASE_URL}/decks/${deckId}`);
    if (!response.ok) throw new Error('Failed to fetch deck');
    const data = await response.json();
    return data.data;
  },

  createDeck: async (name, description = '', isPublic = false) => {
    const response = await authFetch(`${API_BASE_URL}/decks`, {
      method: 'POST',
      body: JSON.stringify({ name, description, isPublic }),
    });
    if (!response.ok) throw new Error('Failed to create deck');
    const data = await response.json();
    return data.data;
  },

  updateDeck: async (deckId, name, description, isPublic) => {
    const response = await authFetch(`${API_BASE_URL}/decks/${deckId}`, {
      method: 'PUT',
      body: JSON.stringify({ name, description, isPublic }),
    });
    if (!response.ok) throw new Error('Failed to update deck');
    const data = await response.json();
    return data.data;
  },

  deleteDeck: async (deckId) => {
    const response = await authFetch(`${API_BASE_URL}/decks/${deckId}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete deck');
    return true;
  },

  addCardToDeck: async (deckId, cardId, quantity = 1, deckType = 'MAIN') => {
    const response = await authFetch(`${API_BASE_URL}/decks/${deckId}/cards`, {
      method: 'POST',
      body: JSON.stringify({ cardId, quantity, deckType }),
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Failed to add card');
    }
    return data.data;
  },

  removeCardFromDeck: async (deckId, cardId, deckType = 'MAIN', quantity = null) => {
    let url = `${API_BASE_URL}/decks/${deckId}/cards/${cardId}?deckType=${deckType}`;
    if (quantity) {
      url += `&quantity=${quantity}`;
    }
    const response = await authFetch(url, { method: 'DELETE' });
    if (!response.ok) throw new Error('Failed to remove card');
    const data = await response.json();
    return data.data;
  },

  validateDeck: async (deckId) => {
    const response = await authFetch(`${API_BASE_URL}/decks/${deckId}/validate`);
    if (!response.ok) throw new Error('Failed to validate deck');
    const data = await response.json();
    return data.data;
  },

  getPublicDecks: async () => {
    const response = await fetch(`${API_BASE_URL}/decks/public`);
    if (!response.ok) throw new Error('Failed to fetch public decks');
    const data = await response.json();
    return data.data || [];
  },

  // Helper to check if user is logged in
  isLoggedIn: () => {
    return !!accessToken;
  },

  getAccessToken,
};