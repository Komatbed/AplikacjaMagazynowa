const Auth = {
    user: null,
    token: null,
    listeners: [],

    init() {
        this.token = localStorage.getItem('token');
        const userStr = localStorage.getItem('user');
        
        if (this.token && userStr) {
            this.user = JSON.parse(userStr);
        }
        this.notify();
    },

    async login(username, password) {
        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) throw new Error('Błędne dane logowania');

            const data = await response.json();
            this.setSession(data);
            return true;
        } catch (e) {
            throw e;
        }
    },

    setSession(authData) {
        this.token = authData.token;
        this.user = {
            username: authData.username,
            role: authData.role,
            fullName: authData.fullName
        };
        
        localStorage.setItem('token', this.token);
        localStorage.setItem('user', JSON.stringify(this.user));
        this.notify();
    },

    logout() {
        this.token = null;
        this.user = null;
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        this.notify();
    },

    // Simple Observer pattern to notify Vue
    subscribe(callback) {
        this.listeners.push(callback);
    },

    notify() {
        this.listeners.forEach(cb => cb(this.user));
    },

    isAuthenticated() {
        return !!this.token;
    },

    isAdmin() {
        return this.user && this.user.role === 'ADMIN';
    }
};
