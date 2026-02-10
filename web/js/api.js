const API_URL = '/api/v1';

const api = {
    async get(endpoint) {
        const response = await fetch(`${API_URL}${endpoint}`);
        if (!response.ok) throw new Error(`API Error: ${response.statusText}`);
        return response.json();
    },

    async post(endpoint, data) {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) {
             const errorData = await response.json().catch(() => ({}));
             throw new Error(errorData.message || `API Error: ${response.statusText}`);
        }
        return response.json();
    },

    // Inventory
    getInventory: (page = 0, size = 20, filters = {}) => {
        const params = new URLSearchParams({
            page, 
            size,
            ...filters
        });
        // Remove empty filters
        for (const [key, value] of params.entries()) {
            if (!value) params.delete(key);
        }
        return api.get(`/inventory/items?${params.toString()}`);
    },
    getConfig: () => api.get('/inventory/config'),
    
    // Reservations
    reserveItem: (itemId, quantity, reservedBy, notes) => 
        api.post('/inventory/reserve', { itemId, quantity, reservedBy, notes }),
    
    cancelReservation: (itemId) => 
        api.post('/inventory/reserve/cancel', { itemId }),
        
    completeReservation: (itemId) =>
        api.post('/inventory/reserve/complete', { itemId }),
};
