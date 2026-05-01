'use strict';

// Reemplaza el historial para que "atrás" vaya al login
history.replaceState(null, '', '/login');

// Si hay una ventana original abierta, recárgala al login
if (window.opener && !window.opener.closed) {
    try { window.opener.location.replace('/login?verificacionExitosa'); } catch (_) {}
}

// Cuenta regresiva y auto-redirect
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

// El botón cancela la cuenta y va directo
if (btnLogin) {
    btnLogin.addEventListener('click', (e) => {
        e.preventDefault();
        clearInterval(interval);
        window.location.replace('/login?verificacionExitosa');
    });
}