/* mapa-riesgo.js: superficie de riesgo de toda la ciudad (con curvas de nivel) y lista de barrios con más riesgo.
   Lee /api/riesgo (RiesgoController). El filtro por tipo se hace aquí, sin pedir nada más al servidor.
   script.js llama a inicializarMapaRiesgo() cada vez que se carga la vista. */
(function () {
    const TIPO = { '1': 'Infraestructura', '2': 'Servicios públicos' };

    // ---- Colores del mapa: riesgo esperado (0 a 1) -> color ----
    // Un barrio "Bajo" ronda 0.1-0.25 (azul), uno "Medio" 0.35-0.55 (amarillo a naranja)
    // y uno "Alto" 0.6 o más (rojo). Por eso el rojo llega desde 0.70.
    const PALETA = [
        [0.00, [31, 91, 255]],    // azul
        [0.18, [0, 200, 255]],    // celeste
        [0.30, [0, 220, 0]],      // verde
        [0.40, [255, 235, 0]],    // amarillo
        [0.52, [255, 140, 0]],    // naranja
        [0.70, [255, 26, 0]],     // rojo
        [1.00, [200, 0, 0]]       // rojo oscuro
    ];

    // ---- Ajustes de la superficie ----
    // Cartagena: los barrios con coordenadas fuera de esta caja se ignoran (coordenadas mal cargadas)
    const CIUDAD = { latMin: 10.25, latMax: 10.55, lngMin: -75.65, lngMax: -75.40 };
    const ANCHO_BARRIO = 450;   // metros: hasta dónde "se siente" el riesgo de cada barrio (en zonas sin barrios cercanos se amplía solo)
    const ALCANCE_PLENO = 1300; // metros: se pinta completo hasta esta distancia del barrio más cercano
    const ALCANCE_MAXIMO = 2600; // metros: más lejos que esto no se pinta (mar, bahía, zonas sin barrios)
    const CELDAS_X = 200;       // resolución de la cuadrícula de cálculo
    const ESCALA = 3;           // la imagen final es 3 veces más fina que la cuadrícula
    const NIVELES_LINEA = [0.15, 0.30, 0.45, 0.60, 0.75];

    let mapa = null;
    let superficie = null;
    let marcas = null;
    let datos = null;
    let tipo = '';
    let primera = true;

    function el(etiqueta, clase, contenido) {
        const e = document.createElement(etiqueta);
        if (clase) e.className = clase;
        if (contenido !== undefined) e.textContent = contenido;
        return e;
    }

    // Misma fórmula del servidor: probabilidad Alta completa + Media a la mitad
    function peso(t) { return (t.probMedio || 0) * 0.5 + (t.probAlto || 0); }

    function resumen(b) {
        const lista = (tipo ? [b.tipos[tipo]] : Object.values(b.tipos)).filter(Boolean);
        if (!lista.length) return null;
        const peor = lista.reduce((a, c) => (c.nivelNumero > a.nivelNumero ? c : a));
        const w = Math.max(...lista.map(peso));   // el peor de los tipos, igual que la etiqueta
        return { nivel: peor.nivel, peso: w };
    }

    function ventanita(b) {
        const caja = el('div', 'riesgo-pop');
        caja.appendChild(el('strong', '', b.nombre));
        Object.keys(b.tipos).sort().forEach(k => {
            const t = b.tipos[k];
            const fila = el('div', 'riesgo-pop-fila');
            fila.appendChild(el('span', '', TIPO[k] || ('Tipo ' + k)));
            fila.appendChild(el('span', 'riesgo-chip riesgo-' + t.nivel.toLowerCase(), t.nivel));
            caja.appendChild(fila);
            caja.appendChild(el('small', '', Math.round((t.probAlto || 0) * 100) + '% de probabilidad alta, ' + t.reportes + ' reportes el mes pasado'));
        });
        return caja;
    }

    function mesLegible(mes) {
        const [a, m] = String(mes).split('-').map(Number);
        return new Date(a, m - 1, 1).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
    }

    function enCiudad(b) {
        return b.lat >= CIUDAD.latMin && b.lat <= CIUDAD.latMax &&
               b.lng >= CIUDAD.lngMin && b.lng <= CIUDAD.lngMax;
    }

    // ---------- Superficie ----------

    function colorEn(v) {
        v = Math.max(0, Math.min(1, v));
        for (let i = 1; i < PALETA.length; i++) {
            if (v <= PALETA[i][0]) {
                const [v0, c0] = PALETA[i - 1];
                const [v1, c1] = PALETA[i];
                const t = (v - v0) / (v1 - v0);
                return [c0[0] + (c1[0] - c0[0]) * t, c0[1] + (c1[1] - c0[1]) * t, c0[2] + (c1[2] - c0[2]) * t];
            }
        }
        return PALETA[PALETA.length - 1][1];
    }

    // Promedio ponderado por cercanía: cada celda toma el riesgo de los barrios que tiene alrededor
    function calcularSuperficie(puntos) {
        const margenLat = ALCANCE_MAXIMO / 110540;
        const lat0 = puntos.reduce((s, p) => s + p.lat, 0) / puntos.length;
        const mx = 111320 * Math.cos(lat0 * Math.PI / 180);
        const my = 110540;
        const margenLng = ALCANCE_MAXIMO / mx;

        const latMin = Math.min(...puntos.map(p => p.lat)) - margenLat;
        const latMax = Math.max(...puntos.map(p => p.lat)) + margenLat;
        const lngMin = Math.min(...puntos.map(p => p.lng)) - margenLng;
        const lngMax = Math.max(...puntos.map(p => p.lng)) + margenLng;

        const anchoM = (lngMax - lngMin) * mx;
        const altoM = (latMax - latMin) * my;
        const nx = CELDAS_X;
        const ny = Math.max(2, Math.round(nx * altoM / anchoM));

        const px = puntos.map(p => (p.lng - lngMin) * mx);
        const py = puntos.map(p => (p.lat - latMin) * my);
        const pw = puntos.map(p => p.w);
        const H2 = ANCHO_BARRIO * ANCHO_BARRIO;

        const vals = new Float32Array(nx * ny);
        const alfa = new Float32Array(nx * ny);

        for (let j = 0; j < ny; j++) {
            const gy = (ny - 1 - j) / (ny - 1) * altoM;   // la fila 0 es el norte
            for (let i = 0; i < nx; i++) {
                const gx = i / (nx - 1) * anchoM;

                // 1.a pasada: barrio más cercano
                let minD2 = Infinity;
                for (let k = 0; k < px.length; k++) {
                    const dx = gx - px[k], dy = gy - py[k];
                    const d2 = dx * dx + dy * dy;
                    if (d2 < minD2) minD2 = d2;
                }

                // 2.a pasada: promedio ponderado por cercanía. Donde hay muchos barrios cerca el ancho es
                // pequeño (los picos de riesgo se conservan); donde hay pocos, se amplía para que el paso
                // de un barrio a otro sea suave. Se resta minD2 para que nunca se anule el cálculo.
                const dosH2 = 2 * Math.max(H2, 0.36 * minD2);
                let sumaK = 0, sumaKW = 0;
                for (let k = 0; k < px.length; k++) {
                    const dx = gx - px[k], dy = gy - py[k];
                    const kk = Math.exp(-((dx * dx + dy * dy) - minD2) / dosH2);
                    sumaK += kk;
                    sumaKW += kk * pw[k];
                }
                const idx = j * nx + i;
                vals[idx] = sumaKW / sumaK;

                const d = Math.sqrt(minD2);
                let a = 1;
                if (d >= ALCANCE_MAXIMO) a = 0;
                else if (d > ALCANCE_PLENO) {
                    const t = (d - ALCANCE_PLENO) / (ALCANCE_MAXIMO - ALCANCE_PLENO);
                    a = 1 - t * t * (3 - 2 * t);           // desvanece suave hacia afuera
                }
                alfa[idx] = a;
            }
        }
        return { nx, ny, vals, alfa, limites: [[latMin, lngMin], [latMax, lngMax]] };
    }

    function muestra(arr, nx, ny, gx, gy) {
        const x0 = Math.min(nx - 2, Math.floor(gx)), y0 = Math.min(ny - 2, Math.floor(gy));
        const fx = gx - x0, fy = gy - y0;
        const a = arr[y0 * nx + x0], b = arr[y0 * nx + x0 + 1];
        const c = arr[(y0 + 1) * nx + x0], d = arr[(y0 + 1) * nx + x0 + 1];
        return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
    }

    // Curvas de nivel (marching squares) sobre la cuadrícula
    function trazarCurvas(ctx, s) {
        const { nx, ny, vals, alfa } = s;
        const cruce = (x1, y1, v1, x2, y2, v2, nivel) => {
            const t = v2 === v1 ? 0.5 : (nivel - v1) / (v2 - v1);
            return [(x1 + (x2 - x1) * t) * ESCALA, (y1 + (y2 - y1) * t) * ESCALA];
        };
        ctx.lineWidth = 1.3;
        ctx.strokeStyle = 'rgba(255,255,255,0.8)';
        ctx.lineJoin = 'round';

        NIVELES_LINEA.forEach(nivel => {
            ctx.beginPath();
            for (let j = 0; j < ny - 1; j++) {
                for (let i = 0; i < nx - 1; i++) {
                    if (alfa[j * nx + i] < 0.75) continue;  // no se trazan líneas en los bordes desvanecidos
                    const vTL = vals[j * nx + i], vTR = vals[j * nx + i + 1];
                    const vBL = vals[(j + 1) * nx + i], vBR = vals[(j + 1) * nx + i + 1];
                    const caso = (vTL >= nivel ? 8 : 0) | (vTR >= nivel ? 4 : 0) | (vBR >= nivel ? 2 : 0) | (vBL >= nivel ? 1 : 0);
                    if (caso === 0 || caso === 15) continue;

                    const arriba = () => cruce(i, j, vTL, i + 1, j, vTR, nivel);
                    const derecha = () => cruce(i + 1, j, vTR, i + 1, j + 1, vBR, nivel);
                    const abajo = () => cruce(i, j + 1, vBL, i + 1, j + 1, vBR, nivel);
                    const izquierda = () => cruce(i, j, vTL, i, j + 1, vBL, nivel);
                    const seg = (p, q) => { ctx.moveTo(p[0], p[1]); ctx.lineTo(q[0], q[1]); };

                    switch (caso) {
                        case 1: case 14: seg(izquierda(), abajo()); break;
                        case 2: case 13: seg(abajo(), derecha()); break;
                        case 3: case 12: seg(izquierda(), derecha()); break;
                        case 4: case 11: seg(arriba(), derecha()); break;
                        case 5: seg(arriba(), izquierda()); seg(abajo(), derecha()); break;
                        case 6: case 9: seg(arriba(), abajo()); break;
                        case 7: case 8: seg(arriba(), izquierda()); break;
                        case 10: seg(arriba(), derecha()); seg(izquierda(), abajo()); break;
                    }
                }
            }
            ctx.stroke();
        });
    }

    function pintarSuperficie(puntos) {
        const s = calcularSuperficie(puntos);
        const W = (s.nx - 1) * ESCALA, H = (s.ny - 1) * ESCALA;
        const lienzo = document.createElement('canvas');
        lienzo.width = W;
        lienzo.height = H;
        const ctx = lienzo.getContext('2d');
        const img = ctx.createImageData(W, H);

        for (let y = 0; y < H; y++) {
            for (let x = 0; x < W; x++) {
                const gx = x / ESCALA, gy = y / ESCALA;
                const a = muestra(s.alfa, s.nx, s.ny, gx, gy);
                const o = (y * W + x) * 4;
                if (a <= 0.01) { img.data[o + 3] = 0; continue; }
                const c = colorEn(muestra(s.vals, s.nx, s.ny, gx, gy));
                img.data[o] = c[0];
                img.data[o + 1] = c[1];
                img.data[o + 2] = c[2];
                img.data[o + 3] = Math.round(255 * a * 0.88);
            }
        }
        ctx.putImageData(img, 0, 0);
        trazarCurvas(ctx, s);
        return L.imageOverlay(lienzo.toDataURL('image/png'), s.limites, { interactive: false });
    }

    function dibujar() {
        if (!datos || !mapa) return;
        if (superficie) mapa.removeLayer(superficie);
        if (marcas) mapa.removeLayer(marcas);

        const puntos = [];
        const filas = [];
        const descartados = [];
        marcas = L.layerGroup();

        datos.barrios.forEach(b => {
            if (!enCiudad(b)) { descartados.push(b.nombre); return; }
            const r = resumen(b);
            if (!r) return;
            puntos.push({ lat: b.lat, lng: b.lng, w: r.peso });
            filas.push({ b, r });
            L.circleMarker([b.lat, b.lng], {
                radius: 3.5, weight: 1.2, color: 'rgba(4,7,11,0.9)',
                fillColor: '#ffffff', fillOpacity: 0.95
            }).bindPopup(ventanita(b)).addTo(marcas);
        });

        if (descartados.length) {
            console.warn('Barrios con coordenadas fuera de Cartagena (no se dibujan): ' + descartados.join(', '));
        }
        if (!puntos.length) return;

        const ord = puntos.map(p => p.w).sort((a, b) => a - b);
        console.info('Riesgo (0 a 1): mínimo ' + ord[0].toFixed(2) + ', mediana ' + ord[Math.floor(ord.length / 2)].toFixed(2) +
                     ', percentil 90 ' + ord[Math.floor(ord.length * 0.9)].toFixed(2) + ', máximo ' + ord[ord.length - 1].toFixed(2));

        superficie = pintarSuperficie(puntos).addTo(mapa);
        marcas.addTo(mapa);

        if (primera) {
            mapa.fitBounds(L.latLngBounds(puntos.map(p => [p.lat, p.lng])), { padding: [30, 30] });
            primera = false;
        }

        const top = document.getElementById('riesgo-top');
        if (!top) return;
        top.innerHTML = '';
        filas.sort((a, b) => b.r.peso - a.r.peso).slice(0, 10).forEach(({ b, r }) => {
            const li = el('li', 'riesgo-fila');
            li.appendChild(el('span', 'riesgo-nombre', b.nombre));
            li.appendChild(el('span', 'riesgo-chip riesgo-' + r.nivel.toLowerCase(), r.nivel));
            li.addEventListener('click', () => {
                mapa.flyTo([b.lat, b.lng], 15);
                L.popup({ className: 'riesgo-popup' }).setLatLng([b.lat, b.lng]).setContent(ventanita(b)).openOn(mapa);
            });
            top.appendChild(li);
        });
    }

    function mensaje(texto) {
        const e = document.getElementById('riesgo-estado');
        if (!e) return;
        e.textContent = texto || '';
        e.style.display = texto ? 'block' : 'none';
    }

    window.inicializarMapaRiesgo = function () {
        const contenedor = document.getElementById('riesgo-mapa');
        if (!contenedor || typeof L === 'undefined') return;

        try { if (mapa) mapa.remove(); } catch (e) { /* el mapa anterior ya no estaba en pantalla */ }
        mapa = L.map(contenedor).setView([10.405, -75.5], 12);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap', maxZoom: 18 }).addTo(mapa);
        superficie = null;
        marcas = null;
        primera = true;
        tipo = '';

        document.querySelectorAll('.pill-tipo').forEach(boton => {
            boton.addEventListener('click', () => {
                tipo = boton.getAttribute('data-tipo') || '';
                document.querySelectorAll('.pill-tipo').forEach(b => b.classList.toggle('activa', b === boton));
                dibujar();
            });
        });

        setTimeout(() => mapa && mapa.invalidateSize(), 200);

        fetch('/api/riesgo')
            .then(r => {
                if (r.status === 404) throw new Error('vacio');
                if (!r.ok) throw new Error('error');
                return r.json();
            })
            .then(json => {
                datos = json;
                const mes = document.getElementById('riesgo-mes');
                if (mes) mes.textContent = mesLegible(json.mes);
                mensaje('');
                dibujar();
            })
            .catch(e => mensaje(e.message === 'vacio'
                ? 'Todavía no hay predicciones guardadas.'
                : 'No se pudieron cargar las predicciones.'));
    };
})();
