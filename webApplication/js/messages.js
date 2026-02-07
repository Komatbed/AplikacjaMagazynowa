document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadUsers();

    // Domyślnie załaduj wiadomości pierwszego użytkownika lub pusty stan
    // loadMessages(1); 

    document.getElementById('messageForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const input = document.getElementById('messageInput');
        const text = input.value.trim();
        if (!text) return;
        
        const activeUserId = document.body.dataset.activeUserId;
        if (!activeUserId) {
            alert('Wybierz użytkownika do rozmowy.');
            return;
        }

        try {
            await api.sendMessage(activeUserId, text);
            addMessage(text, 'me');
            input.value = '';
            
            // Reload messages to see if there's an immediate echo (optional, depending on backend)
            // loadMessages(activeUserId); 
        } catch (error) {
            console.error('Error sending message:', error);
            if (localStorage.getItem('token') === 'demo-token') {
                addMessage(text, 'me');
                input.value = '';
                // Mock reply
                setTimeout(() => {
                    addMessage('Okej, zajmę się tym (Demo).', 'other');
                }, 1000);
            }
        }
    });
});

async function loadUsers() {
    try {
        const users = await api.getUsers();
        renderUsers(users);
    } catch (error) {
        console.error('Error loading users:', error);
        if (localStorage.getItem('token') === 'demo-token') {
            const mockUsers = [
                { id: 1, name: 'Jan Kowalski', role: 'Kierownik', status: 'online' },
                { id: 2, name: 'Anna Nowak', role: 'Magazynier', status: 'offline' },
                { id: 3, name: 'Marek Zając', role: 'Kontroler Jakości', status: 'busy' },
            ];
            renderUsers(mockUsers);
        }
    }
}

function renderUsers(users) {
    const list = document.getElementById('usersList');
    list.innerHTML = users.map(u => `
        <div class="flex items-center px-4 py-3 hover:bg-gray-100 cursor-pointer border-b border-gray-100" onclick="selectUser(${u.id}, '${u.name}')">
            <div class="h-10 w-10 rounded-full bg-gray-300 flex items-center justify-center text-gray-600 font-bold">
                ${u.name.split(' ').map(n => n[0]).join('')}
            </div>
            <div class="ml-3 flex-1">
                <div class="flex justify-between items-baseline">
                    <span class="text-sm font-medium text-gray-900">${u.name}</span>
                    <span class="text-xs text-gray-500">12:30</span>
                </div>
                <p class="text-xs text-gray-500 truncate">${u.role}</p>
            </div>
            <div class="ml-2 h-2.5 w-2.5 rounded-full ${u.status === 'online' ? 'bg-green-500' : (u.status === 'busy' ? 'bg-red-500' : 'bg-gray-300')}"></div>
        </div>
    `).join('');
}

async function selectUser(id, name) {
    document.getElementById('chatUserName').textContent = name;
    document.getElementById('chatAvatar').textContent = name.split(' ').map(n => n[0]).join('');
    document.body.dataset.activeUserId = id;
    
    await loadMessages(id);
}

async function loadMessages(userId) {
    const area = document.getElementById('messagesArea');
    area.innerHTML = '<div class="text-center text-gray-500 mt-4">Ładowanie...</div>';

    try {
        const messages = await api.getMessages(userId);
        area.innerHTML = '';
        messages.forEach(msg => addMessage(msg.text, msg.senderId === 'me' ? 'me' : 'other', msg.timestamp));
    } catch (error) {
        console.error('Error loading messages:', error);
        area.innerHTML = '';
        if (localStorage.getItem('token') === 'demo-token') {
             // Initial mock messages for demo
            addMessage('Cześć, czy przyjęcie PZ/2026/02/01 jest już w systemie?', 'me');
            addMessage('Tak, właśnie zatwierdziłem. Możesz sprawdzać.', 'other');
            addMessage('Dzięki!', 'me');
        }
    }
}

function addMessage(text, type, timestamp = new Date()) {
    const area = document.getElementById('messagesArea');
    const isMe = type === 'me';
    
    const timeStr = new Date(timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

    const bubble = `
        <div class="flex ${isMe ? 'justify-end' : 'justify-start'} mb-2">
            <div class="${isMe ? 'bg-blue-600 text-white' : 'bg-white text-gray-900 border border-gray-200'} rounded-lg px-4 py-2 max-w-xs shadow-sm">
                <p class="text-sm">${text}</p>
                <p class="text-xs ${isMe ? 'text-blue-100' : 'text-gray-400'} mt-1 text-right">${timeStr}</p>
            </div>
        </div>
    `;
    area.insertAdjacentHTML('beforeend', bubble);
    area.scrollTop = area.scrollHeight;
}
