/**
 * API Client Module
 * Handles API requests, authentication, retries, and error handling.
 */

const CONFIG = {
    API_BASE_URL: window.env?.API_URL || 'http://localhost:8080/api/v1',
    TIMEOUT_MS: 10000,
    MAX_RETRIES: 3,
    RETRY_DELAY_MS: 1000
};

class ApiClient {
    constructor() {
        this.baseUrl = CONFIG.API_BASE_URL;
    }

    getToken() {
        return localStorage.getItem('token');
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers,
            signal: AbortSignal.timeout(CONFIG.TIMEOUT_MS)
        };

        return this._fetchWithRetry(url, config);
    }

    async _fetchWithRetry(url, config, retries = CONFIG.MAX_RETRIES) {
        try {
            const response = await fetch(url, config);
            
            if (response.status === 401) {
                // Token expired or invalid
                this.handleUnauthorized();
                throw new Error('Unauthorized');
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            // Return empty object for 204 No Content
            if (response.status === 204) return {};

            return await response.json();
        } catch (error) {
            if (retries > 0 && this._shouldRetry(error)) {
                await new Promise(resolve => setTimeout(resolve, CONFIG.RETRY_DELAY_MS));
                return this._fetchWithRetry(url, config, retries - 1);
            }
            throw error;
        }
    }

    _shouldRetry(error) {
        // Retry on network errors or 5xx server errors
        return !error.response || (error.response.status >= 500 && error.response.status < 600);
    }

    handleUnauthorized() {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        window.location.href = 'index.html?expired=true';
    }

    // Specific API methods
    async login(username, password) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
    }

    async getDashboardStats() {
        return this.request('/dashboard/stats');
    }

    async getInventory(filters = {}) {
        const query = new URLSearchParams(filters).toString();
        return this.request(`/inventory/items?${query}`);
    }

    async postReceipt(data) {
        return this.request('/inventory/receipt', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async postIssue(data) {
        return this.request('/inventory/issue', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async getHistory(filters = {}) {
        const query = new URLSearchParams(filters).toString();
        return this.request(`/inventory/history?${query}`);
    }

    async getClaims() {
        return this.request('/quality/claims');
    }

    async updateClaimDecision(id, decisionData) {
        return this.request(`/quality/claims/${id}/decision`, {
            method: 'POST',
            body: JSON.stringify(decisionData)
        });
    }

    async getShortages() {
        return this.request('/shortages');
    }

    async postShortage(data) {
        return this.request('/shortages', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async getUsers() {
        return this.request('/messages/users');
    }

    async getMessages(userId) {
        return this.request(`/messages/chat/${userId}`);
    }

    async sendMessage(userId, text) {
        return this.request(`/messages/chat/${userId}`, {
            method: 'POST',
            body: JSON.stringify({ text })
        });
    }

    // Mock Data Fallback (for demo/development if backend is down)
    // In production, this can be removed or disabled via flag
    useMocks() {
        console.warn('Using Mock Data Provider');
        // Implement mock overrides here if needed for specific testing
    }
}

const api = new ApiClient();
window.api = api; // Global access
