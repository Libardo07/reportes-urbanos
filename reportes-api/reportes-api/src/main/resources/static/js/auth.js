// auth.js: solo lo que necesitan login.html y registro.html.
// El panel sigue usando script.js; estas dos páginas ya no lo cargan.

const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

// Mostrar u ocultar la contraseña
document.querySelectorAll('.ver-clave').forEach((boton) => {
    const campo = boton.closest('.entrada').querySelector('input');
    boton.addEventListener('click', () => {
        const visible = campo.type === 'password';
        campo.type = visible ? 'text' : 'password';
        boton.setAttribute('aria-pressed', String(visible));
        boton.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
    });
});

// Pantalla de carga mientras el servidor responde (login y formularios con data-carga)
const loader = document.getElementById('loader');

if (loader) {
    document.querySelectorAll('#loginForm, form[data-carga]').forEach((formulario) => {
        formulario.addEventListener('submit', () => loader.classList.add('show'));
    });
}

// Si el usuario vuelve atrás con el botón del navegador, la pantalla de carga se apaga
window.addEventListener('pageshow', () => {
    if (loader) loader.classList.remove('show');
});

// Después de un error, se limpia el ?error de la dirección
if (location.pathname === '/login' && location.search.includes('error')) {
    history.replaceState(null, '', '/login');
}

// Validación en tiempo real del registro (las mismas reglas que tenía script.js)
if (document.getElementById('registroForm')) {
    const correo = document.getElementById('email');
    const clave = document.getElementById('password');

    if (correo) {
        correo.addEventListener('input', () => {
            const corto = correo.value.length > 0 && correo.value.length < 8;
            correo.setCustomValidity(corto ? 'El correo debe tener al menos 8 caracteres.' : '');
        });
    }

    if (clave) {
        clave.addEventListener('input', () => {
            const corta = clave.value.length > 0 && clave.value.length < 6;
            clave.setCustomValidity(corta ? 'La contraseña debe tener al menos 6 caracteres.' : '');
        });
    }
}

// La foto de fondo se mueve un poquito con el mouse (solo en computador)
if (window.matchMedia('(hover: hover) and (pointer: fine)').matches && !sinMovimiento) {
    let cuadro = 0;
    window.addEventListener('mousemove', (e) => {
        if (cuadro) return;
        cuadro = requestAnimationFrame(() => {
            cuadro = 0;
            const raiz = document.documentElement.style;
            raiz.setProperty('--px', ((e.clientX / window.innerWidth - 0.5) * -28) + 'px');
            raiz.setProperty('--py', ((e.clientY / window.innerHeight - 0.5) * -18) + 'px');
        });
    }, { passive: true });
}

// Cuando la foto ya está descargada y lista, se activa el fundido de entrada.
// Usa la misma foto del <link rel="preload"> del <head>, así no se descarga dos veces.
{
    const precarga = document.querySelector('link[rel="preload"][as="image"]');
    const mostrar = () => document.documentElement.classList.add('foto-lista');

    if (precarga) {
        const foto = new Image();
        foto.src = precarga.href;
        if (foto.decode) {
            foto.decode().then(mostrar, mostrar);
        } else {
            foto.onload = mostrar;
            foto.onerror = mostrar;
        }
    } else {
        mostrar();
    }
}