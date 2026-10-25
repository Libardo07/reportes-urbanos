const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const conMouse = window.matchMedia('(hover: hover) and (pointer: fine)').matches;

const nav = document.querySelector('nav');

// La altura total se mide una vez y se vuelve a medir solo si la página cambia de tamaño.
// Antes se leía en cada evento de scroll, y eso obliga al navegador a recalcular todo.
let alturaTotal = 1;
const medir = () => {
    alturaTotal = document.documentElement.scrollHeight - window.innerHeight;
};
medir();

if ('ResizeObserver' in window) {
    new ResizeObserver(medir).observe(document.body);
} else {
    window.addEventListener('resize', medir, { passive: true });
    window.addEventListener('load', medir);
}

// Barra de progreso arriba (para quitarla, borra este bloque y el "if (barra)" de más abajo)
const barra = document.createElement('div');
barra.className = 'progreso';
document.body.appendChild(barra);

// Un solo manejador de scroll, que trabaja una vez por cuadro
let esperando = false;
const alBajar = () => {
    if (esperando) return;
    esperando = true;
    requestAnimationFrame(() => {
        esperando = false;
        const y = window.scrollY;
        nav.classList.toggle('scrolled', y > 10);
        if (barra) {
            barra.style.transform = 'scaleX(' + (alturaTotal > 0 ? y / alturaTotal : 0) + ')';
        }
    });
};
window.addEventListener('scroll', alBajar, { passive: true });
alBajar();

// Entrada suave de los bloques al llegar a la pantalla
if ('IntersectionObserver' in window && !sinMovimiento) {
    const grupos = [
        '.hero-content', '.section-title', '.steps-grid li', '.features-grid',
        '.mapa-texto', '.mapa-lienzo', '.caso', '.team-strip', '.role-card .vidrio', '.cta-panel'
    ];
    const observador = new IntersectionObserver((entradas) => {
        entradas.forEach((e) => {
            if (e.isIntersecting) {
                e.target.classList.add('visto');
                observador.unobserve(e.target);
            }
        });
    }, { threshold: 0.15 });

    grupos.forEach((selector) => {
        document.querySelectorAll(selector).forEach((el, i) => {
            el.classList.add('reveal');
            el.style.setProperty('--d', (i % 4) * 0.12 + 's');
            observador.observe(el);
        });
    });
}

// FONDO ÚNICO: una sola foto fija detrás de toda la página.
// Si el HTML no trae el elemento .fondo, se crea aquí.
let fondo = document.querySelector('.fondo');
if (!fondo) {
    fondo = document.createElement('div');
    fondo.className = 'fondo';
    fondo.setAttribute('aria-hidden', 'true');
    document.body.prepend(fondo);
}

// Cada escena define su encuadre en el CSS (--dx, --dy y --escala).
// La foto se desliza hacia el encuadre de la escena que está en el centro de la pantalla.
const encuadrar = (escena) => {
    const estilo = getComputedStyle(escena);
    ['--dx', '--dy', '--escala'].forEach((nombre) => {
        const valor = estilo.getPropertyValue(nombre).trim();
        if (valor) {
            fondo.style.setProperty(nombre, valor);
        } else {
            fondo.style.removeProperty(nombre);
        }
    });
};

if ('IntersectionObserver' in window) {
    const vigilante = new IntersectionObserver((entradas) => {
        entradas.forEach((e) => {
            if (e.isIntersecting) encuadrar(e.target);
        });
    }, { rootMargin: '-50% 0px -50% 0px' });

    document.querySelectorAll('.escena').forEach((el) => vigilante.observe(el));
}

// Brillo y movimiento con el mouse (solo en computador)
if (conMouse) {
    const VIDRIO = '.vidrio, .section-title, .steps-grid li, .features-grid, .team-strip, .caso-label';
    let cuadro = 0;
    let ultimo = null;

    window.addEventListener('mousemove', (e) => {
        ultimo = e;
        if (cuadro) return;
        cuadro = requestAnimationFrame(() => {
            cuadro = 0;

            // Brillo que sigue al mouse sobre los rectángulos de vidrio
            const el = ultimo.target.closest(VIDRIO);
            if (el) {
                const r = el.getBoundingClientRect();
                el.style.setProperty('--mx', (ultimo.clientX - r.left) + 'px');
                el.style.setProperty('--my', (ultimo.clientY - r.top) + 'px');
            }

            // La foto de fondo se mueve un poquito con el mouse
            if (!sinMovimiento) {
                fondo.style.setProperty('--px', ((ultimo.clientX / window.innerWidth - 0.5) * -28) + 'px');
                fondo.style.setProperty('--py', ((ultimo.clientY / window.innerHeight - 0.5) * -18) + 'px');
            }
        });
    }, { passive: true });
}