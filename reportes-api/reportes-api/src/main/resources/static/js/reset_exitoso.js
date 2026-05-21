'use strict';

history.replaceState(null, '', '/login');
const canal = new BroadcastChannel('password_reset');
canal.postMessage('restablecida');
canal.close();

if (window.opener && !window.opener.closed) {
    try { window.opener.location.replace('/login?passwordRestablecida'); } catch (_) {}
}

// Cuenta regresiva en esta pestaña
const countEl  = document.getElementById('countdown');
const btnLogin = document.getElementById('btn-login');
let seg = 3;

const interval = setInterval(() => {
    seg--;
    if (countEl) countEl.textContent = seg;
    if (seg <= 0) {
        clearInterval(interval);
        window.location.replace('/login?passwordRestablecida');
    }
}, 1000);

if (btnLogin) {
    btnLogin.addEventListener('click', e => {
        e.preventDefault();
        clearInterval(interval);
        window.location.replace('/login?passwordRestablecida');
    });
}