'use strict';
history.replaceState(null, '', '/login');

if (window.opener && !window.opener.closed) {
    try { window.opener.location.replace('/login?verificacionExitosa'); } catch (_) {}
}

const countEl  = document.getElementById('countdown');
const btnLogin = document.getElementById('btn-login');
let seg = 3;

const interval = setInterval(() => {
    seg--;
    if (countEl) countEl.textContent = seg;
    if (seg <= 0) {
        clearInterval(interval);
        window.location.replace('/login?verificacionExitosa');
    }
}, 1000);

if (btnLogin) {
    btnLogin.addEventListener('click', (e) => {
        e.preventDefault();
        clearInterval(interval);
        window.location.replace('/login?verificacionExitosa');
    });
}