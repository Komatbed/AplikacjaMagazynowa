document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadClaims();

    document.getElementById('decisionForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Mock ID logic - in real app would come from modal data attribute
        const id = 1; 
        const formData = new FormData(e.target);
        // ... collect form data

        try {
            await api.updateClaimDecision(id, data);
            alert('Decyzja zapisana');
            closeModal();
            loadClaims();
        } catch (error) {
            if (localStorage.getItem('token') === 'demo-token') {
                 alert('Decyzja zapisana (Demo)');
                 closeModal();
                 loadClaims();
                 return;
            }
            console.error(error);
        }
    });
});

async function loadClaims() {
    try {
        const claims = await api.getClaims();
        renderClaims(claims);
    } catch (error) {
        console.error(error);
        if (localStorage.getItem('token') === 'demo-token') {
             const mockClaims = [
                {
                    id: 1,
                    orderNo: 'ZL/2026/001',
                    deliveryDate: '2026-02-05',
                    status: 'NEW',
                    description: 'Uszkodzenie mechaniczne profili podczas transportu.',
                    situation: 'Kierowca zgłosił uszkodzenie palety przy rozładunku. Widoczne wgniecenia.',
                    items: [
                        { partNo: 'P-1001-WHITE', qty: 5 },
                        { partNo: 'P-2005-ANTH', qty: 2 }
                    ],
                    photos: [
                        'https://via.placeholder.com/150/FF0000/FFFFFF?text=Uszkodzenie+1',
                        'https://via.placeholder.com/150/FF0000/FFFFFF?text=Paleta'
                    ],
                    createdAt: '2026-02-06T14:30:00'
                },
                {
                    id: 2,
                    orderNo: 'ZL/2026/015',
                    deliveryDate: '2026-02-01',
                    status: 'IN_PROGRESS',
                    description: 'Błędny kolor profili.',
                    situation: 'Otrzymano RAL 7016 zamiast RAL 9016.',
                    items: [
                        { partNo: 'P-3000-GREY', qty: 50 }
                    ],
                    photos: [
                        'https://via.placeholder.com/150/0000FF/FFFFFF?text=Etykieta',
                        'https://via.placeholder.com/150/808080/FFFFFF?text=Profil'
                    ],
                    createdAt: '2026-02-07T09:00:00'
                }
            ];
            renderClaims(mockClaims);
            return;
        }
        document.getElementById('claimsList').innerHTML = '<li>Błąd ładowania danych</li>';
    }
}

function renderClaims(claims) {
    // Save for modal usage (simplified for this context)
    window.currentClaims = claims; 

    const list = document.getElementById('claimsList');
    list.innerHTML = claims.map(claim => {
        const statusColors = {
            'NEW': 'bg-red-100 text-red-800',
            'IN_PROGRESS': 'bg-yellow-100 text-yellow-800',
            'APPROVED': 'bg-green-100 text-green-800',
            'REJECTED': 'bg-gray-100 text-gray-800'
        };
        const statusLabel = {
            'NEW': 'Nowe',
            'IN_PROGRESS': 'W Analizie',
            'APPROVED': 'Uznana',
            'REJECTED': 'Odrzucona'
        };

        return `
        <li>
            <a href="#" onclick="openClaimDetails(${claim.id})" class="block hover:bg-gray-50">
                <div class="px-4 py-4 sm:px-6">
                    <div class="flex items-center justify-between">
                        <p class="text-sm font-medium text-blue-600 truncate">
                            Zlecenie #${claim.orderNo}
                        </p>
                        <div class="ml-2 flex-shrink-0 flex">
                            <p class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${statusColors[claim.status]}">
                                ${statusLabel[claim.status]}
                            </p>
                        </div>
                    </div>
                    <div class="mt-2 sm:flex sm:justify-between">
                        <div class="sm:flex">
                            <p class="flex items-center text-sm text-gray-500">
                                <svg class="flex-shrink-0 mr-1.5 h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                                </svg>
                                Zgłoszono: ${new Date(claim.createdAt).toLocaleString()}
                            </p>
                        </div>
                        <div class="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            <p>${claim.description}</p>
                        </div>
                    </div>
                </div>
            </a>
        </li>
        `;
    }).join('');
}

function openClaimDetails(id) {
    // Use cached data
    const claim = window.currentClaims?.find(c => c.id === id);
    if (!claim) return;

    document.getElementById('modalTitle').textContent = `Reklamacja #${id} (Zlecenie ${claim.orderNo})`;
    document.getElementById('modalOrderNo').textContent = claim.orderNo;
    document.getElementById('modalDate').textContent = claim.deliveryDate;
    document.getElementById('modalDescription').textContent = claim.description;
    document.getElementById('modalSituation').textContent = claim.situation;
    document.getElementById('modalStatus').value = claim.status;

    // Items
    const itemsHtml = claim.items.map(item => `
        <tr>
            <td class="px-3 py-2 text-sm text-gray-900">${item.partNo}</td>
            <td class="px-3 py-2 text-sm text-gray-500">${item.qty}</td>
        </tr>
    `).join('');
    document.getElementById('modalItems').innerHTML = itemsHtml;

    // Photos
    const photosHtml = claim.photos.map(url => `
        <a href="${url}" target="_blank" class="block flex-shrink-0 w-24 h-24 border rounded overflow-hidden">
            <img src="${url}" class="w-full h-full object-cover hover:opacity-75 transition" alt="Dowód">
        </a>
    `).join('');
    document.getElementById('modalPhotos').innerHTML = photosHtml;

    document.getElementById('claimModal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('claimModal').classList.add('hidden');
}
