(function () {
    const contenedor = document.getElementById('mapa-leaflet');
    if (!contenedor || typeof L === 'undefined') return;

    const ESTADOS = {
        pendiente: { texto: 'Pendiente', paso: 1 },
        proceso: { texto: 'En proceso', paso: 2 },
        resuelto: { texto: 'Resuelto', paso: 3 }
    };

    // Reportes de EJEMPLO. Las coordenadas son aproximadas, revísalas sobre el mapa.
    const reportes = [
        { lat: 10.4217, lng: -75.5472, grupo: 'Infraestructura', titulo: 'Hueco en la vía', barrio: 'Getsemaní', estado: 'proceso' },
        { lat: 10.4020, lng: -75.5570, grupo: 'Infraestructura', titulo: 'Alumbrado dañado', barrio: 'Bocagrande', estado: 'pendiente' },
        { lat: 10.4235, lng: -75.5400, grupo: 'Servicios públicos', titulo: 'Basura acumulada', barrio: 'La Popa', estado: 'pendiente' },
        { lat: 10.4160, lng: -75.5390, grupo: 'Servicios públicos', titulo: 'Fuga de agua', barrio: 'Manga', estado: 'resuelto' },
        { lat: 10.4430, lng: -75.5130, grupo: 'Infraestructura', titulo: 'Hueco en la vía', barrio: 'Crespo', estado: 'pendiente' }
    ];

    const mapa = L.map(contenedor, {
        attributionControl: false,
        scrollWheelZoom: false,
        dragging: !L.Browser.mobile,
        minZoom: 11,
        maxZoom: 17
    }).setView([10.422, -75.539], 13);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19
    }).addTo(mapa);

    L.control.attribution({ prefix: false })
                .addAttribution('&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors')
        .addTo(mapa);

    const marcadores = reportes.map((r) => {
        const e = ESTADOS[r.estado];
        const pasos = ['Pendiente', 'En proceso', 'Resuelto']
            .map((t, i) => '<li class="' + (i < e.paso ? 'on' : '') + '">' + t + '</li>').join('');
        const html = '<div class="fm"><span class="fm-grupo">' + r.grupo + '</span>' +
            '<strong>' + r.titulo + '</strong><span class="fm-barrio">' + r.barrio + '</span>' +
            '<ol class="fm-pasos mp-' + r.estado + '">' + pasos + '</ol></div>';
        const icono = L.divIcon({ className: 'mp-wrap', html: '<span class="mp mp-' + r.estado + '"></span>', iconSize: [20, 20] });
        return L.marker([r.lat, r.lng], { icon: icono })
            .bindPopup(html, { className: 'ficha-mapa', closeButton: false, autoPan: false, offset: [0, -6] })
            .addTo(mapa);
    });

    // Los reportes se van abriendo solos mientras el mapa está a la vista, hasta que el visitante toca algo
    let temporizador = null;
    let manual = false;
    let i = 0;
    const siguiente = () => { marcadores[i % marcadores.length].openPopup(); i++; };
    const iniciar = () => { if (temporizador || manual) return; siguiente(); temporizador = setInterval(siguiente, 4000); };
    const parar = () => { clearInterval(temporizador); temporizador = null; };
    const tocar = () => { manual = true; parar(); };

    marcadores.forEach((m) => m.on('click', tocar));
    mapa.on('dragstart zoomstart', tocar);

    const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (sinMovimiento || !('IntersectionObserver' in window)) {
        siguiente();
    } else {
        new IntersectionObserver((entradas) => {
            if (entradas[0].isIntersecting) iniciar(); else parar();
        }, { threshold: 0.3 }).observe(contenedor);
    }

    window.addEventListener('load', () => mapa.invalidateSize());
})();