document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadInventory();
    
    document.getElementById('filterForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const profile = document.getElementById('profile').value;
        const color = document.getElementById('color').value;
        loadInventory(profile, color);
    });
});

async function loadInventory(profile = '', color = '') {
    const tableBody = document.getElementById('inventoryTableBody');
    tableBody.innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500">Ładowanie...</td></tr>';

    try {
        const filters = {};
        if (profile) filters.profileCode = profile;
        if (color) filters.internalColor = color;

        const items = await api.getInventory(filters);
        renderTable(items);
    } catch (error) {
        console.error('Error loading inventory:', error);
        
        // Mock fallback
        if (localStorage.getItem('token') === 'demo-token') {
             const items = [
                { profileCode: 'P-1001', internalColor: 'RAL9016', location: 'A-01-01', length: 6000, status: 'AVAILABLE' },
                { profileCode: 'P-1001', internalColor: 'RAL9016', location: 'A-01-02', length: 3200, status: 'AVAILABLE' },
                { profileCode: 'P-2005', internalColor: 'RAL7016', location: 'B-02-01', length: 5800, status: 'RESERVED' },
                { profileCode: 'P-3000', internalColor: 'GOLD_OAK', location: 'C-05-04', length: 1200, status: 'WASTE' },
            ];
            renderTable(items);
            return;
        }

        tableBody.innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center text-red-500">Błąd ładowania danych</td></tr>';
    }
}

function renderTable(items) {
    const tableBody = document.getElementById('inventoryTableBody');
    tableBody.innerHTML = '';

    if (items.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500">Brak wyników</td></tr>';
        return;
    }

    items.forEach(item => {
        const row = document.createElement('tr');
        
        const statusColor = item.status === 'AVAILABLE' ? 'green' : (item.status === 'RESERVED' ? 'yellow' : 'red');
        const statusBadge = `<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-${statusColor}-100 text-${statusColor}-800">${item.status}</span>`;

        row.innerHTML = `
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${item.profileCode}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${item.internalColor}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${item.location}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${item.length}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${statusBadge}</td>
        `;
        tableBody.appendChild(row);
    });
}