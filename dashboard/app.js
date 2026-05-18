const API_GATEWAY = 'http://localhost:8080/api';

// Application State
let token = localStorage.getItem('token');
let user = JSON.parse(localStorage.getItem('user'));
let accounts = [];
let autoRefreshInterval = null;

// Initializer
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    if (token && user) {
        showSection('dashboard-section');
        updateNavUser(true);
        loadDashboard();
        startAutoRefresh();
    } else {
        showSection('auth-section');
        updateNavUser(false);
        stopAutoRefresh();
    }
}

// Section Toggle Utilities
function showSection(sectionId) {
    document.getElementById('auth-section').classList.add('hidden');
    document.getElementById('dashboard-section').classList.add('hidden');
    document.getElementById(sectionId).classList.remove('hidden');
}

function updateNavUser(isLoggedIn) {
    const container = document.getElementById('nav-user-info');
    if (isLoggedIn && user) {
        container.innerHTML = `
            <div class="flex items-center gap-2.5">
                <div class="w-8 h-8 rounded-lg bg-violet-600/10 border border-violet-500/20 flex items-center justify-center">
                    <i class="fa-solid fa-user text-violet-400 text-xs"></i>
                </div>
                <div class="flex flex-col text-left">
                    <span class="text-xs font-bold text-white">${user.name}</span>
                    <span class="text-[10px] text-slate-500">ID: ${user.id}</span>
                </div>
            </div>
        `;
    } else {
        container.innerHTML = `
            <span class="text-xs font-semibold text-slate-500 flex items-center gap-1.5">
                <i class="fa-solid fa-lock"></i> Secured Session
            </span>
        `;
    }
}

// Authentication Tab Toggles
function switchAuthTab(tab) {
    const tabLogin = document.getElementById('tab-login');
    const tabRegister = document.getElementById('tab-register');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const alertBox = document.getElementById('auth-alert');

    alertBox.classList.add('hidden');

    if (tab === 'login') {
        tabLogin.className = 'flex-1 pb-3 text-white border-b-2 border-violet-500 transition-all';
        tabRegister.className = 'flex-1 pb-3 text-slate-400 hover:text-white transition-all';
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
    } else {
        tabRegister.className = 'flex-1 pb-3 text-white border-b-2 border-violet-500 transition-all';
        tabLogin.className = 'flex-1 pb-3 text-slate-400 hover:text-white transition-all';
        registerForm.classList.remove('hidden');
        loginForm.classList.add('hidden');
    }
}

// Login Handler
async function handleLogin(event) {
    event.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    const alertBox = document.getElementById('auth-alert');

    try {
        const response = await fetch(`${API_GATEWAY}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (response.ok) {
            token = data.token;
            user = { id: data.id, name: data.name, email: data.email };
            localStorage.setItem('token', token);
            localStorage.setItem('user', JSON.stringify(user));
            
            alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-green-500/10 border border-green-500/20 text-green-400';
            alertBox.innerHTML = '<i class="fa-solid fa-circle-check"></i> Sign In Successful!';
            alertBox.classList.remove('hidden');

            setTimeout(() => {
                initApp();
            }, 1000);
        } else {
            throw new Error(data.message || 'Login failed. Please check your credentials.');
        }
    } catch (error) {
        alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400';
        alertBox.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${error.message}`;
        alertBox.classList.remove('hidden');
    }
}

// Register Handler
async function handleRegister(event) {
    event.preventDefault();
    const name = document.getElementById('register-name').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    const alertBox = document.getElementById('auth-alert');

    try {
        const response = await fetch(`${API_GATEWAY}/users/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
        });

        const text = await response.text();

        if (response.ok) {
            alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-green-500/10 border border-green-500/20 text-green-400';
            alertBox.innerHTML = `<i class="fa-solid fa-circle-check"></i> ${text}! Switching to Sign In...`;
            alertBox.classList.remove('hidden');

            setTimeout(() => {
                switchAuthTab('login');
                document.getElementById('login-email').value = email;
            }, 1500);
        } else {
            throw new Error(text || 'Registration failed. Email might already be taken.');
        }
    } catch (error) {
        alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400';
        alertBox.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${error.message}`;
        alertBox.classList.remove('hidden');
    }
}

// Logout Handler
async function handleLogout() {
    try {
        await fetch(`${API_GATEWAY}/users/logout`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });
    } catch (e) {
        console.warn('Backend logout failed or token already blacklisted:', e.message);
    }

    localStorage.clear();
    token = null;
    user = null;
    initApp();
}

// Load Dashboard Components
async function loadDashboard() {
    if (!token || !user) return;

    try {
        // Update user profile info on dashboard cards
        document.getElementById('profile-name').innerText = user.name;
        document.getElementById('profile-email').innerText = user.email;

        // Fetch Accounts and Notifications
        await Promise.all([
            fetchAccounts(),
            fetchNotifications()
        ]);
    } catch (error) {
        console.error('Error loading dashboard:', error);
    }
}

// Fetch User Accounts
async function fetchAccounts() {
    try {
        const response = await fetch(`${API_GATEWAY}/accounts/${user.id}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (!response.ok) throw new Error('Failed to retrieve accounts');
        accounts = await response.json();
        
        renderAccounts();
        updateTransferDropdowns();
    } catch (error) {
        console.error(error.message);
    }
}

// Render Accounts Cards
function renderAccounts() {
    const container = document.getElementById('accounts-container');
    
    if (accounts.length === 0) {
        container.innerHTML = `
            <div class="h-full flex flex-col items-center justify-center text-center p-8 text-slate-500">
                <i class="fa-solid fa-piggy-bank text-4xl mb-3"></i>
                <span class="text-sm font-semibold">No accounts opened yet</span>
                <span class="text-xs mt-1">Open a savings or current account to start transacting.</span>
            </div>
        `;
        document.getElementById('total-balance').innerText = '0.00';
        return;
    }

    let netWorth = 0;
    let html = '';

    accounts.forEach(acc => {
        netWorth += acc.balance;
        
        html += `
            <div class="p-5 bg-white/5 border border-white/10 hover:border-violet-500/30 rounded-2xl flex items-center justify-between transition-all">
                <div class="flex items-center gap-4">
                    <div class="w-12 h-12 rounded-xl bg-violet-600/10 border border-violet-500/20 flex items-center justify-center text-violet-400">
                        <i class="fa-solid ${acc.accountType === 'SAVINGS' ? 'fa-piggy-bank' : 'fa-credit-card'} text-lg"></i>
                    </div>
                    <div>
                        <div class="flex items-center gap-2">
                            <span class="font-semibold text-white text-sm">${acc.accountType}</span>
                            <span class="px-2 py-0.5 rounded-full bg-violet-500/10 border border-violet-500/20 text-[10px] text-violet-400 font-bold">${acc.accountNumber}</span>
                        </div>
                        <span class="text-xs text-slate-500">Account ID: ${acc.id}</span>
                    </div>
                </div>
                <div class="flex items-center gap-6">
                    <div class="text-right">
                        <span class="text-xs text-slate-500 block">Current Balance</span>
                        <span class="font-['Outfit'] font-bold text-lg text-white">$${acc.balance.toFixed(2)}</span>
                    </div>
                    <button onclick="openFundModal(${acc.id})" class="px-3.5 py-2 bg-violet-600/10 hover:bg-violet-600/20 border border-violet-500/20 text-violet-400 rounded-xl text-xs font-bold transition-all">
                        <i class="fa-solid fa-plus"></i> Fund
                    </button>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
    document.getElementById('total-balance').innerText = netWorth.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Update From/To Dropdowns
function updateTransferDropdowns() {
    const fromSelect = document.getElementById('transfer-from');
    fromSelect.innerHTML = accounts.map(acc => `
        <option value="${acc.id}">${acc.accountType} (${acc.accountNumber}) - $${acc.balance.toFixed(2)}</option>
    `).join('');
}

// Fetch Live Notifications / Events
async function fetchNotifications() {
    try {
        const response = await fetch(`${API_GATEWAY}/notifications/${user.id}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Failed to retrieve notifications');
        const notifications = await response.json();

        renderNotifications(notifications);
    } catch (error) {
        console.error(error.message);
    }
}

// Render Notifications
function renderNotifications(notifications) {
    const container = document.getElementById('notifications-container');
    const badge = document.getElementById('alerts-count');

    badge.innerText = `${notifications.length} Alert${notifications.length === 1 ? '' : 's'}`;

    if (notifications.length === 0) {
        container.innerHTML = `
            <div class="h-full flex flex-col items-center justify-center text-center p-8 text-slate-500">
                <i class="fa-solid fa-clock-rotate-left text-3xl mb-3"></i>
                <span class="text-xs font-semibold">No Kafka alerts received</span>
                <span class="text-[10px] mt-1">Simulated notifications will stream here live on cash transfers.</span>
            </div>
        `;
        return;
    }

    container.innerHTML = notifications.map(notif => {
        const date = new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        
        return `
            <div class="p-4 bg-white/5 border border-white/10 hover:bg-white/10 rounded-2xl flex items-start gap-3 transition-all">
                <div class="w-8 h-8 shrink-0 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                    <i class="fa-solid fa-message text-xs"></i>
                </div>
                <div class="flex-1">
                    <div class="flex items-center justify-between">
                        <span class="text-[10px] text-slate-500">${date}</span>
                        <span class="w-1.5 h-1.5 rounded-full ${notif.readStatus ? 'bg-transparent' : 'bg-blue-400'}"></span>
                    </div>
                    <p class="text-xs text-slate-300 mt-1 leading-relaxed">${notif.message}</p>
                </div>
            </div>
        `;
    }).join('');
}

// Execute Secure Money Transfer
async function handleTransfer(event) {
    event.preventDefault();
    const fromAccountId = document.getElementById('transfer-from').value;
    const toAccountId = document.getElementById('transfer-to').value;
    const amount = parseFloat(document.getElementById('transfer-amount').value);
    const description = document.getElementById('transfer-description').value;
    const alertBox = document.getElementById('transfer-alert');

    try {
        const response = await fetch(`${API_GATEWAY}/transactions/transfer`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify({ fromAccountId, toAccountId, amount, description })
        });

        const data = await response.json();

        if (response.ok) {
            alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-green-500/10 border border-green-500/20 text-green-400';
            alertBox.innerHTML = '<i class="fa-solid fa-circle-check"></i> Transfer Completed! Kafka notification published.';
            alertBox.classList.remove('hidden');

            document.getElementById('transfer-amount').value = '';
            document.getElementById('transfer-description').value = '';
            document.getElementById('transfer-to').value = '';

            setTimeout(() => {
                alertBox.classList.add('hidden');
                loadDashboard();
            }, 2000);
        } else {
            throw new Error(data.message || 'Transfer failed. Check target account ID and source balance.');
        }
    } catch (error) {
        alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400';
        alertBox.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${error.message}`;
        alertBox.classList.remove('hidden');
    }
}

// Open Account Modal Handlers
function openOpenAccountModal() {
    document.getElementById('new-account-modal').classList.remove('hidden');
}

function closeOpenAccountModal() {
    document.getElementById('new-account-modal').classList.add('hidden');
    document.getElementById('modal-alert').classList.add('hidden');
}

async function handleCreateAccount(event) {
    event.preventDefault();
    const accountType = document.getElementById('modal-account-type').value;
    const alertBox = document.getElementById('modal-alert');

    try {
        const response = await fetch(`${API_GATEWAY}/accounts`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify({ userId: user.id, accountType })
        });

        const data = await response.json();

        if (response.ok) {
            alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-green-500/10 border border-green-500/20 text-green-400';
            alertBox.innerHTML = `<i class="fa-solid fa-circle-check"></i> Opened Account: ${data.accountNumber}`;
            alertBox.classList.remove('hidden');

            setTimeout(() => {
                closeOpenAccountModal();
                loadDashboard();
            }, 1500);
        } else {
            throw new Error(data.message || 'Failed to open account');
        }
    } catch (error) {
        alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400';
        alertBox.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${error.message}`;
        alertBox.classList.remove('hidden');
    }
}

// Fund Account Modal Handlers
function openFundModal(accountId) {
    document.getElementById('fund-account-id').value = accountId;
    document.getElementById('fund-account-modal').classList.remove('hidden');
}

function closeFundAccountModal() {
    document.getElementById('fund-account-modal').classList.add('hidden');
    document.getElementById('fund-modal-alert').classList.add('hidden');
    document.getElementById('fund-amount').value = '';
}

async function handleFundAccount(event) {
    event.preventDefault();
    const accountId = document.getElementById('fund-account-id').value;
    const balance = parseFloat(document.getElementById('fund-amount').value);
    const alertBox = document.getElementById('fund-modal-alert');

    try {
        const response = await fetch(`${API_GATEWAY}/accounts/balance/${accountId}`, {
            method: 'PUT',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify({ balance })
        });

        const data = await response.json();

        if (response.ok) {
            alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-green-500/10 border border-green-500/20 text-green-400';
            alertBox.innerHTML = `<i class="fa-solid fa-circle-check"></i> Funded Successfully! Balance updated.`;
            alertBox.classList.remove('hidden');

            setTimeout(() => {
                closeFundAccountModal();
                loadDashboard();
            }, 1500);
        } else {
            throw new Error(data.message || 'Failed to update balance');
        }
    } catch (error) {
        alertBox.className = 'mb-4 p-4 rounded-xl text-sm flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400';
        alertBox.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${error.message}`;
        alertBox.classList.remove('hidden');
    }
}

// Helper: Scroll to Transfer section
function scrollToTransfer() {
    document.getElementById('transfer-panel').scrollIntoView({ behavior: 'smooth' });
}

// Manual Refresh Trigger
function refreshAccounts() {
    fetchAccounts();
}

// Live Updates Poller
function startAutoRefresh() {
    if (autoRefreshInterval) clearInterval(autoRefreshInterval);
    // Poll notifications every 4 seconds to catch active transfers
    autoRefreshInterval = setInterval(() => {
        fetchNotifications();
    }, 4000);
}

function stopAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
    }
}
