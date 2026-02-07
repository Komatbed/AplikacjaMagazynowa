const { createApp, ref, computed, onMounted, watch } = Vue;

const app = createApp({
    setup() {
        const loading = ref(false);
        const loginLoading = ref(false);
        const loginError = ref('');
        const loginForm = ref({ username: '', password: '' });
        
        const user = ref(Auth.user);
        const currentView = ref('dashboard');
        
        const notifications = ref([]);
        
        // Data
        const inventoryItems = ref([]);
        const inventoryCount = ref(0);
        const filters = ref({ profile: '', color: '' });
        
        const profiles = ref([]);
        const colors = ref([]);
        const newProfile = ref({ code: '', name: '' });
        const newColor = ref({ code: '', name: '' });

        const newUser = ref({ username: '', password: '', fullName: '', role: 'PRACOWNIK' });
        const adminMsg = ref('');
        const adminError = ref(false);

        // Auth state sync
        Auth.subscribe((u) => {
            user.value = u;
            if (!u) currentView.value = 'dashboard'; // Reset view on logout
        });

        const isAuthenticated = computed(() => !!user.value);
        const isAdmin = computed(() => user.value && user.value.role === 'ADMIN');

        // Methods
        const login = async () => {
            loginLoading.value = true;
            loginError.value = '';
            try {
                await Auth.login(loginForm.value.username, loginForm.value.password);
                loginForm.value = { username: '', password: '' };
            } catch (e) {
                loginError.value = e.message;
            } finally {
                loginLoading.value = false;
            }
        };

        const logout = () => {
            Auth.logout();
        };

        const removeNotification = (id) => {
            notifications.value = notifications.value.filter(n => n.id !== id);
        };

        // API Calls
        const fetchInventory = async () => {
            if (!isAuthenticated.value) return;
            loading.value = true;
            try {
                let query = '/v1/inventory/items?';
                if (filters.value.profile) query += `profileCode=${filters.value.profile}&`;
                if (filters.value.color) query += `internalColor=${filters.value.color}&`; // Simplified filter
                
                const data = await Api.get(query);
                inventoryItems.value = data || [];
                inventoryCount.value = inventoryItems.value.length;
            } catch (e) {
                console.error(e);
            } finally {
                loading.value = false;
            }
        };

        const fetchConfig = async () => {
            if (!isAuthenticated.value) return;
            try {
                profiles.value = await Api.get('/v1/config/profiles');
                colors.value = await Api.get('/v1/config/colors');
            } catch (e) {
                console.error(e);
            }
        };

        const addProfile = async () => {
            try {
                await Api.post('/v1/config/profiles', newProfile.value);
                newProfile.value = { code: '', name: '' };
                fetchConfig();
            } catch (e) { alert(e.message); }
        };

        const deleteProfile = async (id) => {
            if(!confirm('Czy na pewno usunąć?')) return;
            try {
                await Api.delete(`/v1/config/profiles/${id}`);
                fetchConfig();
            } catch (e) { alert(e.message); }
        };

        const addColor = async () => {
            try {
                await Api.post('/v1/config/colors', newColor.value);
                newColor.value = { code: '', name: '' };
                fetchConfig();
            } catch (e) { alert(e.message); }
        };

        const deleteColor = async (id) => {
            if(!confirm('Czy na pewno usunąć?')) return;
            try {
                await Api.delete(`/v1/config/colors/${id}`);
                fetchConfig();
            } catch (e) { alert(e.message); }
        };

        const registerUser = async () => {
            adminMsg.value = '';
            adminError.value = false;
            try {
                await Api.post('/auth/register', newUser.value);
                adminMsg.value = 'Użytkownik utworzony pomyślnie';
                newUser.value = { username: '', password: '', fullName: '', role: 'PRACOWNIK' };
            } catch (e) {
                adminError.value = true;
                adminMsg.value = 'Błąd: ' + e.message;
            }
        };

        // WebSocket
        const initWebSocket = () => {
            const socket = new WebSocket('ws://' + window.location.host + '/ws-warehouse');
            const stompClient = Stomp.over(socket);
            stompClient.debug = null;
            stompClient.connect({}, () => {
                stompClient.subscribe('/topic/notifications', (msg) => {
                    const data = JSON.parse(msg.body);
                    notifications.value.push({
                        id: Date.now(),
                        title: data.title,
                        message: data.message
                    });
                    // Auto remove
                    setTimeout(() => removeNotification(notifications.value[notifications.value.length-1]?.id), 5000);
                });
            }, (err) => console.log('WS Error:', err));
        };

        // Watchers
        watch(currentView, (newView) => {
            if (newView === 'inventory') fetchInventory();
            if (newView === 'settings') fetchConfig();
        });

        // Init
        onMounted(() => {
            Auth.init();
            initWebSocket();
            if (isAuthenticated.value) {
                fetchInventory(); // Load stats for dashboard
            }
        });

        return {
            loading, loginLoading, loginError, loginForm,
            user, currentView, notifications, isAuthenticated, isAdmin,
            login, logout, removeNotification,
            inventoryItems, inventoryCount, filters, fetchInventory,
            profiles, colors, newProfile, newColor, addProfile, deleteProfile, addColor, deleteColor,
            newUser, adminMsg, adminError, registerUser
        };
    }
});

app.mount('#app');
