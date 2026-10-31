// panel-shell.js: menú lateral en celular (se abre con el botón de la barra superior).
// Es independiente de script.js, que sigue encargándose de cargar las vistas.
(() => {
    const cuerpo = document.body;
    const boton = document.querySelector('.menu-toggle');
    const fondo = document.querySelector('.sidebar-fondo');

    const alternar = (abrir) => {
        cuerpo.classList.toggle('menu-abierto', abrir);
        if (boton) {
            boton.setAttribute('aria-expanded', String(abrir));
            boton.setAttribute('aria-label', abrir ? 'Cerrar menú' : 'Abrir menú');
        }
    };

    if (boton) {
        boton.addEventListener('click', () => alternar(!cuerpo.classList.contains('menu-abierto')));
    }

    if (fondo) {
        fondo.addEventListener('click', () => alternar(false));
    }

    // Al escoger una opción del menú, se cierra
    document.querySelectorAll('.topnav .menu-item').forEach((item) => {
        item.addEventListener('click', () => alternar(false));
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            alternar(false);
        }
    });

        // La barra de arriba se oscurece al bajar, como en la landing
    const contenido = document.getElementById('content-area');
    const barra = document.querySelector('.topnav');
    if (contenido && barra) {
        contenido.addEventListener('scroll', () => {
            barra.classList.toggle('scrolled', contenido.scrollTop > 10);
        }, { passive: true });
    }

        // Nombre corto en la barra de arriba: "Libardo J. Villero C." (el completo queda al pasar el mouse)
    const nombreEl = document.querySelector('.topnav .nombre');
    if (nombreEl) {
        const completo = nombreEl.textContent.trim();
        const p = completo.split(/\s+/);
        let corto = completo;
        if (p.length === 3) corto = `${p[0]} ${p[1][0].toUpperCase()}. ${p[2]}`;
        if (p.length >= 4) corto = `${p[0]} ${p[1][0].toUpperCase()}. ${p[2]} ${p[3][0].toUpperCase()}.`;
        nombreEl.textContent = corto;
        nombreEl.title = completo;
    }

        // Ojo de la contraseña (el mismo del login)
    document.addEventListener('click', (e) => {
        const boton = e.target.closest('.ver-clave');
        if (!boton) return;
        const campo = boton.parentElement.querySelector('input');
        if (!campo) return;
        const visible = campo.type === 'password';
        campo.type = visible ? 'text' : 'password';
        boton.setAttribute('aria-pressed', String(visible));
        boton.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
    });
})();