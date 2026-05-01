'use strict';

let pollingInterval   = null;
let yaVerificado      = false;

// ── Arranque ────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    bloquearNavegacionAtras();
    window.addEventListener('beforeunload', advertenciaAlSalir);
    iniciarPolling();
});

// ══════════════════════════════════════════════════════════════════════════════
// POLLING — detecta la verificación automáticamente cada 3 s
// ══════════════════════════════════════════════════════════════════════════════
function iniciarPolling() {
    pollingInterval = setInterval(() => {
        fetch('/verificar-correo/estado')
            .then(r => r.json())
            .then(({ verificado, sinSesion }) => {
                if (sinSesion) { clearInterval(pollingInterval); return; }
                if (verificado) {
                    clearInterval(pollingInterval);
                    yaVerificado = true;
                    window.removeEventListener('beforeunload', advertenciaAlSalir);
                    mostrarAlerta('success',
                        '✅ ¡Tu correo fue verificado! Redirigiendo al inicio de sesión...');
                    setTimeout(() =>
                        window.location.replace('/login?verificacionExitosa'), 2500);
                }
            })
            .catch(() => {}); // silencioso — sigue intentando
    }, 3000);
}

// ══════════════════════════════════════════════════════════════════════════════
// NAVEGACIÓN — advertencias y bloqueo
// ══════════════════════════════════════════════════════════════════════════════
function advertenciaAlSalir(e) {
    e.preventDefault();
    e.returnValue = 'Si sales deberás esperar 1 minuto para registrarte de nuevo.';
    return e.returnValue;
}

function bloquearNavegacionAtras() {
    history.pushState(null, '', window.location.href);
    window.addEventListener('popstate', () => {
        if (!yaVerificado) mostrarModalSalir();
        history.pushState(null, '', window.location.href);
    });
}

// ── Modal: confirmar salida ─────────────────────────────────────────────────
function mostrarModalSalir() {
    document.getElementById('modal-salir').style.display = 'flex';
}

function cerrarModalSalir() {
    document.getElementById('modal-salir').style.display = 'none';
}

function confirmarSalida() {
    clearInterval(pollingInterval);
    window.removeEventListener('beforeunload', advertenciaAlSalir);
    cerrarModalSalir();
    window.location.replace('/registro');
}

// ══════════════════════════════════════════════════════════════════════════════
// REENVIAR ENLACE
// ══════════════════════════════════════════════════════════════════════════════
function reenviarEnlace() {
    const btn = document.getElementById('btn-reenviar');
    setLoadingBtn(btn, true, 'Enviando...');

    fetch('/reenviar-verificacion', { method: 'POST' })
        .then(r => r.json())
        .then(({ resultado }) => {
            if (resultado === 'OK') {
                mostrarAlerta('success', '✓ Enlace reenviado. Revisa tu bandeja de entrada.');
                iniciarCooldown(btn, 60);
            } else if (resultado === 'LIMITE') {
                mostrarAlerta('warn',
                    '⚠ Alcanzaste el límite de 3 reenvíos. ' +
                    'Si no recibes el correo, intenta crear una cuenta nueva.');
                btn.disabled = true;
                btn.textContent = 'Límite alcanzado';
            } else {
                mostrarAlerta('error', 'Error al reenviar. Intenta de nuevo.');
                restaurarBtn(btn);
            }
        })
        .catch(() => {
            mostrarAlerta('error', 'Error de conexión. Intenta de nuevo.');
            restaurarBtn(btn);
        });
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMBIAR CORREO
// ══════════════════════════════════════════════════════════════════════════════
function abrirModalCorreo() {
    document.getElementById('modal-cambiar-correo').style.display = 'flex';
    setTimeout(() => document.getElementById('nuevo-email').focus(), 80);
}

function cerrarModalCorreo() {
    document.getElementById('modal-cambiar-correo').style.display = 'none';
    document.getElementById('nuevo-email').value = '';
    ocultarModalError();
}

function cambiarCorreo() {
    const input      = document.getElementById('nuevo-email');
    const nuevoEmail = input.value.trim();
    const btnConf    = document.getElementById('btn-confirmar-correo');

    if (!nuevoEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(nuevoEmail)) {
        mostrarModalError('Ingresa un correo electrónico válido.');
        return;
    }

    ocultarModalError();
    setLoadingBtn(btnConf, true, 'Confirmando...');

    fetch('/cambiar-correo-verificacion', {
        method: 'POST',
        body: new URLSearchParams({ nuevoEmail })
    })
        .then(r => r.json())
        .then(({ resultado, emailOculto }) => {
            if (resultado === 'OK') {
                cerrarModalCorreo();
                const el = document.getElementById('emailOculto');
                if (el) el.textContent = emailOculto;
                mostrarAlerta('success',
                    '✓ Correo actualizado. Revisa tu nueva bandeja de entrada.');
            } else if (resultado === 'YA_EXISTE') {
                mostrarModalError('Este correo ya tiene una cuenta registrada.');
                restaurarBtn(btnConf);
            } else {
                mostrarModalError('Ocurrió un error. Intenta de nuevo.');
                restaurarBtn(btnConf);
            }
        })
        .catch(() => {
            mostrarModalError('Error de conexión. Intenta de nuevo.');
            restaurarBtn(btnConf);
        });
}

// ══════════════════════════════════════════════════════════════════════════════
// UTILIDADES
// ══════════════════════════════════════════════════════════════════════════════
function mostrarAlerta(tipo, mensaje) {
    const el = document.getElementById('ve-alerta');
    el.className = `ve-alerta ve-alerta--${tipo}`;
    el.textContent = mensaje;
    el.style.display = 'block';
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function mostrarModalError(msg) {
    const el = document.getElementById('modal-error');
    el.textContent = msg;
    el.style.display = 'block';
}

function ocultarModalError() {
    document.getElementById('modal-error').style.display = 'none';
}

function setLoadingBtn(btn, loading, texto = '') {
    if (loading) {
        btn.dataset.original = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = `<span class="spinner"></span>${texto}`;
    }
}

function restaurarBtn(btn) {
    btn.disabled = false;
    if (btn.dataset.original) btn.innerHTML = btn.dataset.original;
}

function iniciarCooldown(btn, segundos) {
    btn.disabled = true;
    let seg = segundos;
    btn.textContent = `Reenviar en ${seg}s`;
    const id = setInterval(() => {
        seg--;
        btn.textContent = `Reenviar en ${seg}s`;
        if (seg <= 0) {
            clearInterval(id);
            btn.disabled = false;
            btn.innerHTML = btn.dataset.original ||
                'Reenviar enlace de verificación';
        }
    }, 1000);
}

// Escape cierra modales
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') { cerrarModalCorreo(); cerrarModalSalir(); }
});