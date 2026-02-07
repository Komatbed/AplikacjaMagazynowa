document.addEventListener('DOMContentLoaded', async () => {
    checkAuth();
    
    // Mock Data loading - replace with actual API calls
    await loadDashboardStats();
    renderCharts();
});

async function loadDashboardStats() {
    try {
        const stats = await api.getDashboardStats();
        
        document.getElementById('totalItems').textContent = stats.totalItems;
        document.getElementById('lowStockItems').textContent = stats.lowStockItems;
        document.getElementById('openIssues').textContent = stats.openIssues;
        document.getElementById('activeShortages').textContent = stats.activeShortages;
        
    } catch (error) {
        console.error('Error loading stats:', error);
        // Keep mock values if API fails in demo
        if (localStorage.getItem('token') === 'demo-token') {
            document.getElementById('totalItems').textContent = '1,245';
            document.getElementById('lowStockItems').textContent = '12';
            document.getElementById('openIssues').textContent = '3';
            document.getElementById('activeShortages').textContent = '2';
        }
    }
}

function renderCharts() {
    // Usage Chart
    const usageCtx = document.getElementById('usageChart').getContext('2d');
    new Chart(usageCtx, {
        type: 'line',
        data: {
            labels: ['Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'So', 'Nd'],
            datasets: [{
                label: 'Zużyte metry',
                data: [120, 190, 300, 250, 200, 90, 40],
                borderColor: 'rgb(59, 130, 246)',
                tension: 0.1
            }]
        }
    });

    // Stock Chart
    const stockCtx = document.getElementById('stockChart').getContext('2d');
    new Chart(stockCtx, {
        type: 'doughnut',
        data: {
            labels: ['Profil A', 'Profil B', 'Profil C', 'Inne'],
            datasets: [{
                data: [300, 50, 100, 40],
                backgroundColor: [
                    'rgb(59, 130, 246)',
                    'rgb(239, 68, 68)',
                    'rgb(16, 185, 129)',
                    'rgb(209, 213, 219)'
                ]
            }]
        }
    });
}