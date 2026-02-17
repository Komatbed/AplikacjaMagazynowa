document.addEventListener('DOMContentLoaded', async () => {
    checkAuth();
    await loadCoreMap();
    setupHandlers();
});

let coreMap = {};
let originalCoreMap = {};

async function loadCoreMap() {
    try {
        const data = await api.getCoreMap();
        coreMap = { ...data };
        originalCoreMap = { ...data };
        renderTable();
        showAlert('Załadowano aktualną mapę rdzeni.', 'info');
    } catch (error) {
        console.error('Error loading core map:', error);
        showAlert('Nie udało się pobrać mapy rdzeni z serwera.', 'error');
    }
}

function setupHandlers() {
    document.getElementById('reloadButton').addEventListener('click', async () => {
        await loadCoreMap();
    });

    document.getElementById('saveButton').addEventListener('click', async () => {
        try {
            await api.updateCoreMap(coreMap);
            originalCoreMap = { ...coreMap };
            showAlert('Zapisano mapę rdzeni.', 'success');
        } catch (error) {
            console.error('Error saving core map:', error);
            showAlert('Błąd podczas zapisu mapy rdzeni.', 'error');
        }
    });

    document.getElementById('cancelButton').addEventListener('click', () => {
        coreMap = { ...originalCoreMap };
        renderTable();
        showAlert('Przywrócono ostatnio zapisaną wersję.', 'info');
    });

    document.getElementById('addRowButton').addEventListener('click', () => {
        coreMap[''] = '';
        renderTable();
    });

    document.getElementById('filterInput').addEventListener('input', (e) => {
        renderTable(e.target.value);
    });
}

function renderTable(filter = '') {
    const tbody = document.getElementById('coreMapTable');
    tbody.innerHTML = '';

    const entries = Object.entries(coreMap)
        .filter(([key]) => key.toLowerCase().includes(filter.toLowerCase()))
        .sort((a, b) => a[0].localeCompare(b[0], 'pl'));

    entries.forEach(([extName, coreColor]) => {
        const tr = document.createElement('tr');

        const nameTd = document.createElement('td');
        nameTd.className = 'px-4 py-2 whitespace-nowrap';
        const nameInput = document.createElement('input');
        nameInput.type = 'text';
        nameInput.value = extName;
        nameInput.className = 'mt-1 block w-full px-2 py-1 border border-gray-300 rounded-md text-sm';
        nameInput.addEventListener('change', (e) => {
            const newKey = e.target.value;
            if (newKey !== extName) {
                delete coreMap[extName];
            }
            coreMap[newKey] = coreColor;
            renderTable(filter);
        });
        nameTd.appendChild(nameInput);

        const coreTd = document.createElement('td');
        coreTd.className = 'px-4 py-2 whitespace-nowrap';
        const coreInput = document.createElement('input');
        coreInput.type = 'text';
        coreInput.value = coreColor;
        coreInput.placeholder = 'biały / brąz / karmel / szary / antracyt / czarny / kremowy';
        coreInput.className = 'mt-1 block w-full px-2 py-1 border border-gray-300 rounded-md text-sm';
        coreInput.addEventListener('change', (e) => {
            coreMap[extName] = e.target.value;
        });
        coreTd.appendChild(coreInput);

        const actionsTd = document.createElement('td');
        actionsTd.className = 'px-4 py-2 whitespace-nowrap text-right text-sm';
        const deleteBtn = document.createElement('button');
        deleteBtn.textContent = 'Usuń';
        deleteBtn.className = 'inline-flex items-center px-2 py-1 border border-transparent text-xs font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200';
        deleteBtn.addEventListener('click', () => {
            delete coreMap[extName];
            renderTable(filter);
        });
        actionsTd.appendChild(deleteBtn);

        tr.appendChild(nameTd);
        tr.appendChild(coreTd);
        tr.appendChild(actionsTd);
        tbody.appendChild(tr);
    });
}

function showAlert(message, type) {
    const alert = document.getElementById('alert');
    if (!alert) return;
    alert.textContent = message;
    alert.classList.remove('hidden', 'bg-green-100', 'text-green-800', 'bg-red-100', 'text-red-800', 'bg-blue-100', 'text-blue-800');
    if (type === 'success') {
        alert.classList.add('bg-green-100', 'text-green-800');
    } else if (type === 'error') {
        alert.classList.add('bg-red-100', 'text-red-800');
    } else {
        alert.classList.add('bg-blue-100', 'text-blue-800');
    }
    setTimeout(() => {
        alert.classList.add('hidden');
    }, 4000);
}

