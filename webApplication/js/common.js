function loadNavbar(activePage) {
    const role = localStorage.getItem('role') || 'GUEST';
    
    const navHtml = `
    <nav class="bg-white shadow-lg">
        <div class="max-w-7xl mx-auto px-4">
            <div class="flex justify-between h-16">
                <div class="flex items-center">
                    <span class="text-xl font-bold text-gray-800 mr-8">Warehouse Manager</span>
                    <div class="hidden md:block">
                        <div class="ml-10 flex items-baseline space-x-4">
                            <a href="dashboard.html" class="${activePage === 'dashboard' ? 'bg-blue-600 text-white' : 'text-gray-700 hover:text-gray-900 hover:bg-gray-50'} px-3 py-2 rounded-md text-sm font-medium">Dashboard</a>
                            
                            <!-- Dropdown for Magazyn -->
                            <div class="relative group inline-block">
                                <button class="${activePage === 'inventory' || activePage === 'operations' ? 'bg-blue-600 text-white' : 'text-gray-700 hover:text-gray-900 hover:bg-gray-50'} px-3 py-2 rounded-md text-sm font-medium focus:outline-none">
                                    Magazyn
                                </button>
                                <div class="absolute z-10 hidden group-hover:block w-48 bg-white rounded-md shadow-lg py-1 ring-1 ring-black ring-opacity-5">
                                    <a href="inventory.html" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Stany Magazynowe</a>
                                    <a href="warehouse_operations.html" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Operacje i Historia</a>
                                </div>
                            </div>

                            <a href="quality_control.html" class="${activePage === 'quality' ? 'bg-blue-600 text-white' : 'text-gray-700 hover:text-gray-900 hover:bg-gray-50'} px-3 py-2 rounded-md text-sm font-medium">Kontrola Jakości</a>
                            <a href="shortages.html" class="${activePage === 'shortages' ? 'bg-blue-600 text-white' : 'text-gray-700 hover:text-gray-900 hover:bg-gray-50'} px-3 py-2 rounded-md text-sm font-medium">Zgłoszenia Braków</a>
                            <a href="messages.html" class="${activePage === 'messages' ? 'bg-blue-600 text-white' : 'text-gray-700 hover:text-gray-900 hover:bg-gray-50'} px-3 py-2 rounded-md text-sm font-medium">Wiadomości</a>
                        </div>
                    </div>
                </div>
                <div class="flex items-center">
                    <span class="text-sm text-gray-500 mr-4">${role}</span>
                    <button onclick="logout()" class="text-sm text-red-600 hover:text-red-800 font-medium">Wyloguj</button>
                </div>
            </div>
        </div>
        <!-- Mobile menu placeholder -->
    </nav>
    `;

    document.getElementById('navbar-container').innerHTML = navHtml;
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    window.location.href = 'index.html';
}

function checkAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'index.html';
    }
}
