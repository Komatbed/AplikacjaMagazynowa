document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadHistory();

    document.getElementById('receiptForm').addEventListener('submit', handleReceipt);
    document.getElementById('issueForm').addEventListener('submit', handleIssue);
});

function switchTab(tabName) {
    // Hide all contents
    ['receipt', 'issue', 'history'].forEach(t => {
        document.getElementById(`content-${t}`).classList.add('hidden');
        document.getElementById(`tab-${t}`).classList.remove('border-blue-500', 'text-blue-600');
        document.getElementById(`tab-${t}`).classList.add('border-transparent', 'text-gray-500');
    });

    // Show selected
    document.getElementById(`content-${tabName}`).classList.remove('hidden');
    const activeTab = document.getElementById(`tab-${tabName}`);
    activeTab.classList.remove('border-transparent', 'text-gray-500');
    activeTab.classList.add('border-blue-500', 'text-blue-600');
}

async function handleReceipt(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());

    try {
        await api.postReceipt(data);
        alert('Przyjęcie towaru zarejestrowane');
        switchTab('history');
        loadHistory();
    } catch (error) {
        console.error(error);
        if (localStorage.getItem('token') === 'demo-token') {
             alert('Przyjęcie towaru zarejestrowane (Demo)');
             switchTab('history');
             loadHistory();
             return;
        }
        alert('Błąd podczas rejestracji przyjęcia');
    }
}

async function handleIssue(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());

    try {
        await api.postIssue(data);
        alert('Wydanie towaru zarejestrowane');
        switchTab('history');
        loadHistory();
    } catch (error) {
        console.error(error);
        if (localStorage.getItem('token') === 'demo-token') {
             alert('Wydanie towaru zarejestrowane (Demo)');
             switchTab('history');
             loadHistory();
             return;
        }
        alert('Błąd podczas rejestracji wydania');
    }
}

async function loadHistory() {
    const tableBody = document.getElementById('historyTableBody');
    tableBody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center">Ładowanie...</td></tr>';

    try {
        const history = await api.getHistory();
        renderHistoryTable(history);
    } catch (error) {
        console.error(error);
         if (localStorage.getItem('token') === 'demo-token') {
            // Mock Data
            const history = [
                { date: '2026-02-07 10:30', type: 'PZ', profile: 'P-1001', qty: 50, user: 'Jan Kowalski', doc: 'PZ/2026/02/01' },
                { date: '2026-02-07 11:15', type: 'WZ', profile: 'P-2005', qty: -12, user: 'Anna Nowak', doc: 'WZ/2026/02/05' },
                { date: '2026-02-06 14:20', type: 'PZ', profile: 'P-3000', qty: 100, user: 'Jan Kowalski', doc: 'PZ/2026/02/02' },
                { date: '2026-02-06 09:00', type: 'WZ', profile: 'P-1001', qty: -20, user: 'Marek Zając', doc: 'WZ/2026/02/04' },
            ];
            renderHistoryTable(history);
            return;
         }
         tableBody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-red-500">Błąd ładowania historii</td></tr>';
    }
}

function renderHistoryTable(history) {
    const tableBody = document.getElementById('historyTableBody');
    tableBody.innerHTML = history.map(item => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${item.date}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium ${item.type === 'PZ' ? 'text-green-600' : 'text-red-600'}">${item.type}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${item.profile}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${item.qty}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${item.user}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${item.doc}</td>
        </tr>
    `).join('');
}
