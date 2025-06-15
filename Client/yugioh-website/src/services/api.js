const API_BASE_URL = 'http://localhost:8080/api';

export const apiService = {
  uploadCard: async (file) => {
    const formData = new FormData();
    formData.append('image', file);
    
    const response = await fetch(`${API_BASE_URL}/cards/upload`, {
      method: 'POST',
      body: formData
    });
    
    if (!response.ok) {
      throw new Error('Upload failed');
    }
    
    return response.json();
  },
  
  searchCard: async (name) => {
    const response = await fetch(`${API_BASE_URL}/cards/search?name=${encodeURIComponent(name)}`);
    
    if (!response.ok) {
      throw new Error('Search failed');
    }
    
    return response.json();
  }
};