document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadShortages();

    document.getElementById('shortageForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        try {
            await api.postShortage(data);
            alert('Zgłoszenie wysłane do działu zaopatrzenia');
            loadShortages();
        } catch (error) {
            console.error(error);
            if (localStorage.getItem('token') === 'demo-token') {
                 alert('Zgłoszenie wysłane do działu zaopatrzenia (Demo)');
                 loadShortages();
                 return;
            }
            alert('Błąd wysyłania zgłoszenia');
        }
    });
});

async function loadShortages() {
    try {
        const shortages = await api.getShortages();
        renderShortages(shortages);
    } catch (error) {
        console.error(error);
        if (localStorage.getItem('token') === 'demo-token') {
             const shortages = [
                { id: 1, item: 'P-1001-WHITE', qty: 200, dept: 'ZAOPATRZENIE', priority: 'CRITICAL', status: 'OPEN', date: '2026-02-07 08:00' },
                { id: 2, item: 'USZCZELKA-U1', qty: 500, dept: 'ZAOPATRZENIE', priority: 'HIGH', status: 'IN_PROGRESS', date: '2026-02-06 14:00' },
                { id: 3, item: 'WKRETY-3.5x25', qty: 1000, dept: 'MAGAZYN', priority: 'NORMAL', status: 'OPEN', date: '2026-02-07 10:30' },
            ];
            renderShortages(shortages);
            return;
        }
        document.getElementById('shortagesList').innerHTML = '<li>Błąd ładowania danych</li>';
    }
}

function renderShortages(shortages) {
    const list = document.getElementById('shortagesList');
    list.innerHTML = shortages.map(s => {
        const priorityColor = s.priority === 'CRITICAL' ? 'text-red-600 font-bold' : (s.priority === 'HIGH' ? 'text-orange-600' : 'text-gray-500');
        
        return `
        <li class="px-4 py-4 sm:px-6 hover:bg-gray-50">
            <div class="flex items-center justify-between">
                <p class="text-sm font-medium text-blue-600 truncate">${s.item}</p>
                <div class="ml-2 flex-shrink-0 flex">
                    <p class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${s.status === 'OPEN' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}">
                        ${s.status}
                    </p>
                </div>
            </div>
            <div class="mt-2 sm:flex sm:justify-between">
                <div class="sm:flex">
                    <p class="flex items-center text-sm text-gray-500 mr-6">
                        Ilość: ${s.qty}
                    </p>
                    <p class="flex items-center text-sm ${priorityColor}">
                        Priorytet: ${s.priority}
                    </p>
                </div>
                <div class="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                    <p>Dział: ${s.dept}</p>
                </div>
            </div>
        </li>
        `;
    }).join('');
}
