// State
let inventoryItems = [];
let currentPage = 0;
let pageSize = 20;
let totalPages = 0;
let currentUser = localStorage.getItem('currentUser') || 'Kierownik';

// Navigation
function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(el => el.classList.remove('active'));
    document.getElementById(sectionId).classList.add('active');
    
    document.querySelectorAll('nav a').forEach(el => el.classList.remove('active'));
    const navLink = document.querySelector(`nav a[href="#${sectionId}"]`);
    if(navLink) navLink.classList.add('active');

    if (sectionId === 'warehouse-list') {
        currentPage = 0; // Reset page when switching
        loadInventory();
    }
    if (sectionId === 'reservations') loadReservations();
}

// Loading Indicator
function showLoading(show) {
    const loader = document.getElementById('loader');
    if(loader) loader.style.display = show ? 'block' : 'none';
}

// Inventory View
async function loadInventory(page = 0) {
    showLoading(true);
    try {
        const filters = {
            profileCode: document.getElementById('filter-profile')?.value || '',
            location: document.getElementById('filter-location')?.value || '',
            internalColor: document.getElementById('filter-color-int')?.value || ''
        };
        
        const response = await api.getInventory(page, pageSize, filters);
        inventoryItems = response.content;
        currentPage = response.number;
        totalPages = response.totalPages;
        renderInventoryTable(inventoryItems);
        renderPaginationControls();
    } catch (e) {
        console.error(e);
    } finally {
        showLoading(false);
    }
}

function clearFilters() {
    if(document.getElementById('filter-profile')) document.getElementById('filter-profile').value = '';
    if(document.getElementById('filter-location')) document.getElementById('filter-location').value = '';
    if(document.getElementById('filter-color-int')) document.getElementById('filter-color-int').value = '';
    loadInventory(0);
}

function renderPaginationControls() {
    const container = document.getElementById('pagination-controls');
    if (!container) return;

    container.innerHTML = `
        <button class="btn btn-primary" ${currentPage === 0 ? 'disabled' : ''} onclick="loadInventory(${currentPage - 1})">Poprzednia</button>
        <span style="margin: 0 10px;">Strona ${currentPage + 1} z ${totalPages}</span>
        <button class="btn btn-primary" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="loadInventory(${currentPage + 1})">Następna</button>
    `;
}

function renderInventoryTable(items) {
    const tbody = document.querySelector('#inventory-table tbody');
    tbody.innerHTML = '';
    
    // Filter out reserved items from general view or show them with status
    const availableItems = items.filter(i => i.status !== 'WASTE'); 

    availableItems.forEach(item => {
        const tr = document.createElement('tr');
        const isAvailable = item.status === 'AVAILABLE';
        
        tr.innerHTML = `
            <td>${item.location.label}</td>
            <td>${item.profileCode}</td>
            <td>${item.lengthMm} mm</td>
            <td>${item.quantity}</td>
            <td>
                <span style="color: ${isAvailable ? 'green' : 'orange'}; font-weight: bold;">
                    ${item.status}
                </span>
            </td>
            <td>
                ${isAvailable ? 
                `<button class="btn btn-primary" onclick="openReserveModal('${item.id}', ${item.quantity}, '${item.profileCode}')">Rezerwuj</button>` : 
                (item.reservedBy ? `Rezerwacja: ${item.reservedBy}` : '')}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Reservation View (Simplified - fetch all for now or separate endpoint later)
// Note: For reservations we might still want to filter on client side or create specific endpoint
// For now, let's fetch first page of ALL items and filter (Inefficient, but backend API for "getReserved" is missing)
// TODO: Create /inventory/reserved endpoint
async function loadReservations() {
    showLoading(true);
    try {
        // Fetch reservations using status filter
        // We fetch page 0 with size 100 for now. Pagination for reservations can be added later.
        const response = await api.getInventory(0, 100, { status: 'RESERVED' }); 
        const items = response.content;
        renderReservationTable(items);
    } catch (e) {
        console.error(e);
    } finally {
        showLoading(false);
    }
}

function renderReservationTable(items) {
    const tbody = document.querySelector('#reservation-table tbody');
    tbody.innerHTML = '';

    if (items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center">Brak aktywnych rezerwacji</td></tr>';
        return;
    }

    items.forEach(item => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${item.profileCode}</td>
            <td>${item.lengthMm} mm</td>
            <td>${item.quantity}</td>
            <td>${item.reservedBy}</td>
            <td>${new Date(item.reservationDate).toLocaleString()}</td>
            <td>
                <button class="btn btn-success" onclick="completeOrder('${item.id}')">Wydaj (Zrealizuj)</button>
                <button class="btn btn-danger" onclick="cancelOrder('${item.id}')">Anuluj</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Actions
let currentReserveId = null;

function openReserveModal(id, maxQty, profileCode) {
    currentReserveId = id;
    document.getElementById('reserve-modal').style.display = 'block';
    document.getElementById('reserve-title').innerText = `Rezerwacja: ${profileCode}`;
    const qtyInput = document.getElementById('reserve-qty');
    qtyInput.max = maxQty;
    qtyInput.value = maxQty;
    document.getElementById('reserve-user').value = currentUser;
}

function closeReserveModal() {
    document.getElementById('reserve-modal').style.display = 'none';
}

async function submitReservation() {
    const qty = parseInt(document.getElementById('reserve-qty').value);
    const notes = document.getElementById('reserve-notes').value;
    const user = document.getElementById('reserve-user').value || currentUser;

    if (!user) {
        alert('Podaj nazwisko zamawiającego');
        return;
    }

    try {
        await api.reserveItem(currentReserveId, qty, user, notes);
        closeReserveModal();
        alert('Zarezerwowano pomyślnie');
        loadInventory();
        localStorage.setItem('currentUser', user);
        currentUser = user;
    } catch (e) {
        alert(e.message);
    }
}

async function completeOrder(id) {
    if(!confirm('Czy na pewno chcesz wydać ten towar?')) return;
    try {
        await api.completeReservation(id);
        loadReservations();
    } catch (e) {
        alert(e.message);
    }
}

async function cancelOrder(id) {
    if(!confirm('Czy anulować rezerwację? Towar wróci do puli dostępnych.')) return;
    try {
        await api.cancelReservation(id);
        loadReservations();
    } catch (e) {
        alert(e.message);
    }
}

// Init
document.addEventListener('DOMContentLoaded', () => {
    // Navigation listeners
    document.querySelectorAll('nav a').forEach(a => {
        a.addEventListener('click', (e) => {
            e.preventDefault();
            const section = e.target.getAttribute('href').substring(1);
            showSection(section);
        });
    });

    // Default Section
    showSection('dashboard');

    // Close modal when clicking outside
    window.onclick = function(event) {
        const modal = document.getElementById('reserve-modal');
        if (event.target == modal) {
            closeReserveModal();
        }
    }
});
