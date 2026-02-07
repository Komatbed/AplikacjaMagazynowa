document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');

    try {
        const data = await api.login(username, password);
        
        localStorage.setItem('token', data.token);
        localStorage.setItem('role', data.role);
        
        // Redirect based on role
        if (data.role === 'ADMIN' || data.role === 'MANAGER') {
            window.location.href = 'dashboard.html';
        } else {
            window.location.href = 'inventory.html';
        }
    } catch (error) {
        console.error('Login error:', error);
        
        // Mock fallback for demo (Remove in strict prod)
        if (username === 'admin' && password === 'admin') {
            localStorage.setItem('token', 'demo-token');
            localStorage.setItem('role', 'MANAGER');
            window.location.href = 'dashboard.html';
            return;
        }

        errorMessage.classList.remove('hidden');
        errorMessage.textContent = error.message || 'Błąd logowania.';
    }
});

function checkAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'index.html';
    }
    return token;
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    window.location.href = 'index.html';
}