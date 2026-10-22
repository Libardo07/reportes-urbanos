const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const nav = document.querySelector('nav');
const alBajar = () => nav.classList.toggle('scrolled', window.scrollY > 10);
window.addEventListener('scroll', alBajar, { passive: true });
alBajar();

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


const VIDRIO = '.vidrio, .section-title, .steps-grid li, .features-grid, .team-strip, .caso-label';
document.addEventListener('mousemove', (e) => {
    const el = e.target.closest(VIDRIO);
    if (!el) return;
    const r = el.getBoundingClientRect();
    el.style.setProperty('--mx', (e.clientX - r.left) + 'px');
    el.style.setProperty('--my', (e.clientY - r.top) + 'px');
}, { passive: true });


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

const barra = document.createElement('div');
barra.className = 'progreso';
document.body.appendChild(barra);
const progreso = () => {
    const total = document.documentElement.scrollHeight - window.innerHeight;
    barra.style.transform = 'scaleX(' + (total > 0 ? window.scrollY / total : 0) + ')';
};
window.addEventListener('scroll', progreso, { passive: true });
progreso();

