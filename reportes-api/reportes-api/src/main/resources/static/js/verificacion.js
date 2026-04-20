// ============================================================
// VERIFICACIÓN DE CORREO — verificacion.js
// ============================================================

let tiempoRestante = 300; // 5 minutos en segundos
let intervalo;
let reenviosHechos = 0;
let intentosUsados = 0;
const MAX_INTENTOS = 5;

const MENSAJES_AYUDA = [
    "Asegúrate de que el correo esté activo y sea accesible. Revisa tu bandeja de entrada y carpeta de spam.",
    "Los correos de Gmail suelen recibir nuestras notificaciones más rápido.",
    "Si usas Outlook, Hotmail o Yahoo es posible que el correo no llegue. Te recomendamos usar Gmail."
];

// Iniciar temporizador
function iniciarTemporizador() {
    clearInterval(intervalo);
    intervalo = setInterval(function() {
        tiempoRestante--;
        actualizarTemporizador();
        if (tiempoRestante <= 0) {
            clearInterval(intervalo);
            document.getElementById('temporizador').textContent = '00:00';
            document.getElementById('temporizador').classList.add('expirando');
            mostrarError('El código ha expirado. Por favor reenvía el código.');
            document.getElementById('btn-verificar').disabled = true;
        }
    }, 1000);
}

function actualizarTemporizador() {
    const minutos = Math.floor(tiempoRestante / 60);
    const segundos = tiempoRestante % 60;
    const texto = String(minutos).padStart(2, '0') + ':' + String(segundos).padStart(2, '0');
    const el = document.getElementById('temporizador');
    el.textContent = texto;
    if (tiempoRestante <= 60) {
        el.classList.add('expirando');
    }
}

// Manejo de inputs del código
document.addEventListener('DOMContentLoaded', function() {
    iniciarTemporizador();
    iniciarIntentosDots();

    const inputs = document.querySelectorAll('.codigo-input');
    inputs.forEach((input, index) => {
        input.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value && index < inputs.length - 1) {
                inputs[index + 1].focus();
            }
            if (getCodigo().length === 6) {
                verificarCodigo();
            }
        });

        input.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && !this.value && index > 0) {
                inputs[index - 1].focus();
            }
        });

        input.addEventListener('paste', function(e) {
            e.preventDefault();
            const pegado = (e.clipboardData || window.clipboardData).getData('text').replace(/[^0-9]/g, '');
            pegado.split('').forEach((char, i) => {
                if (inputs[index + i]) inputs[index + i].value = char;
            });
            if (getCodigo().length === 6) verificarCodigo();
        });
    });
});

function getCodigo() {
    return Array.from(document.querySelectorAll('.codigo-input'))
        .map(i => i.value).join('');
}

function iniciarIntentosDots() {
    const dots = document.getElementById('intentos-dots');
    dots.innerHTML = '';
    for (let i = 0; i < MAX_INTENTOS; i++) {
        const dot = document.createElement('div');
        dot.className = 'intento-dot';
        dot.id = 'dot-' + i;
        dots.appendChild(dot);
    }
}

function actualizarIntentosDots(intentosUsados) {
    const wrapper = document.getElementById('intentos-wrapper');
    wrapper.style.display = 'flex';
    for (let i = 0; i < MAX_INTENTOS; i++) {
        const dot = document.getElementById('dot-' + i);
        if (dot) {
            dot.classList.toggle('usado', i < intentosUsados);
        }
    }
    const restantes = MAX_INTENTOS - intentosUsados;
    document.getElementById('intentos-texto').textContent =
        restantes + ' intento' + (restantes === 1 ? '' : 's') + ' disponible' + (restantes === 1 ? '' : 's');
}

function mostrarError(msg) {
    const el = document.getElementById('error-codigo');
    el.textContent = msg;
    el.style.display = 'block';
}

function ocultarError() {
    document.getElementById('error-codigo').style.display = 'none';
}

function mostrarMensajeAyuda(reenvios) {
    const idx = Math.min(reenvios - 1, MENSAJES_AYUDA.length - 1);
    if (idx >= 0) {
        const el = document.getElementById('mensaje-ayuda');
        document.getElementById('texto-ayuda').textContent = MENSAJES_AYUDA[idx];
        el.style.display = 'flex';
    }
}

function marcarInputsIncorrecto() {
    document.querySelectorAll('.codigo-input').forEach(i => {
        i.classList.add('incorrecto');
        setTimeout(() => i.classList.remove('incorrecto'), 500);
    });
}

function limpiarInputs() {
    document.querySelectorAll('.codigo-input').forEach(i => i.value = '');
    document.querySelectorAll('.codigo-input')[0].focus();
}

// Verificar código
function verificarCodigo() {
    const codigo = getCodigo();
    if (codigo.length !== 6) {
        mostrarError('Ingresa los 6 dígitos del código.');
        return;
    }

    ocultarError();
    const loader = document.getElementById('loader');
    if (loader) loader.classList.add('show');

    const formData = new FormData();
    formData.append('codigo', codigo);

    fetch('/verificar-codigo', { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            if (loader) loader.classList.remove('show');
            if (data.resultado === 'OK') {
                document.querySelectorAll('.codigo-input').forEach(i => i.classList.add('correcto'));
                setTimeout(() => { window.location.href = '/login?verificacionExitosa'; }, 800);
            } else if (data.resultado === 'EXPIRADO') {
                mostrarError('El código ha expirado. Por favor reenvía el código.');
                document.getElementById('btn-verificar').disabled = true;
            } else if (data.resultado === 'BLOQUEADO') {
                mostrarError('Has agotado todos los intentos. Por favor reenvía el código.');
                document.getElementById('btn-verificar').disabled = true;
                actualizarIntentosDots(MAX_INTENTOS);
            } else if (data.resultado && data.resultado.startsWith('INCORRECTO:')) {
                const restantes = parseInt(data.resultado.split(':')[1]);
                intentosUsados = MAX_INTENTOS - restantes;
                marcarInputsIncorrecto();
                limpiarInputs();
                actualizarIntentosDots(intentosUsados);
                mostrarError('Código incorrecto. Te quedan ' + restantes + ' intento' + (restantes === 1 ? '' : 's') + '.');
            }
        })
        .catch(() => {
            if (loader) loader.classList.remove('show');
            mostrarError('Error al verificar. Intenta de nuevo.');
        });
}

// Reenviar código
function reenviarCodigo() {
    const btn = document.getElementById('btn-reenviar');
    btn.disabled = true;

    fetch('/reenviar-codigo', { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            if (data.resultado && data.resultado.startsWith('REENVIADO:')) {
                reenviosHechos = parseInt(data.resultado.split(':')[1]);
                tiempoRestante = 300;
                document.getElementById('temporizador').classList.remove('expirando');
                document.getElementById('btn-verificar').disabled = false;
                iniciarTemporizador();
                limpiarInputs();
                ocultarError();
                intentosUsados = 0;
                actualizarIntentosDots(0);
                mostrarMensajeAyuda(reenviosHechos);

                setTimeout(() => { btn.disabled = false; }, 30000);
            }
        })
        .catch(() => { btn.disabled = false; });
}

// Cambiar correo
function mostrarCambiarCorreo() {
    document.getElementById('modal-cambiar-correo').style.display = 'flex';
    document.getElementById('nuevo-email').focus();
}

function cerrarCambiarCorreo() {
    document.getElementById('modal-cambiar-correo').style.display = 'none';
    document.getElementById('nuevo-email').value = '';
    document.getElementById('error-cambiar').style.display = 'none';
}

function cambiarCorreo() {
    const nuevoEmail = document.getElementById('nuevo-email').value.trim();
    const errorEl = document.getElementById('error-cambiar');

    if (!nuevoEmail) {
        errorEl.textContent = 'Ingresa un correo válido.';
        errorEl.style.display = 'block';
        return;
    }

    const formData = new FormData();
    formData.append('nuevoEmail', nuevoEmail);

    fetch('/cambiar-correo-verificacion', { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            if (data.resultado === 'OK') {
                document.querySelector('.email-destino-valor').textContent = data.emailOculto;
                cerrarCambiarCorreo();
                tiempoRestante = 300;
                reenviosHechos = 0;
                document.getElementById('temporizador').classList.remove('expirando');
                document.getElementById('btn-verificar').disabled = false;
                document.getElementById('mensaje-ayuda').style.display = 'none';
                iniciarTemporizador();
                limpiarInputs();
                ocultarError();
            } else if (data.resultado === 'YA_EXISTE') {
                errorEl.textContent = 'Este correo ya está registrado.';
                errorEl.style.display = 'block';
            } else {
                errorEl.textContent = 'Error al cambiar el correo. Intenta de nuevo.';
                errorEl.style.display = 'block';
            }
        })
        .catch(() => {
            errorEl.textContent = 'Error al cambiar el correo. Intenta de nuevo.';
            errorEl.style.display = 'block';
        });
}