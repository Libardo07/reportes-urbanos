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

    // ----- Nombre del usuario abreviado en la tabla del administrador -----
// "Libardo José Villero Paternina" -> "Libardo J. Villero P." (el nombre completo sale al pasar el mouse)
function abreviarNombre(nombre) {
    const mayuscula = (p) => p.charAt(0).toLocaleUpperCase('es') + p.slice(1);
    const partes = (nombre || '').trim().split(/\s+/).filter(Boolean);

    if (partes.length <= 2) return partes.map(mayuscula).join(' ');

    const primero     = partes[0];
    const intermedios = partes.slice(1, partes.length - 2);
    const apellido1   = partes[partes.length - 2];
    const apellido2   = partes[partes.length - 1];

    return [
        mayuscula(primero),
        ...intermedios.map(p => mayuscula(p).charAt(0) + '.'),
        mayuscula(apellido1),
        mayuscula(apellido2).charAt(0) + '.'
    ].join(' ');
}

function abreviarUsuariosEnTablas() {
    document.querySelectorAll('#content-area table').forEach(tabla => {
        const cabecera = tabla.querySelector('thead th');
        if (!cabecera || cabecera.textContent.trim().toLowerCase() !== 'usuario') return;

        tabla.querySelectorAll('tbody tr').forEach(fila => {
            const celda = fila.cells[0];
            if (!celda || celda.dataset.abreviado || celda.children.length > 0) return;

            const completo = celda.textContent.trim();
            celda.dataset.abreviado = '1';
            celda.title = completo;
            celda.textContent = abreviarNombre(completo);
        });
    });
}

// Se aplica sola cada vez que el panel pinta una lista nueva (cambio de página, filtros, etc.)
    const contenidoPanel = document.getElementById('content-area');
    if (contenidoPanel) {
        new MutationObserver(abreviarUsuariosEnTablas).observe(contenidoPanel, { childList: true, subtree: true });
        abreviarUsuariosEnTablas();
    }
})();