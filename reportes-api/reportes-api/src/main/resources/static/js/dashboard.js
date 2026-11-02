(function () {
    // Limpia lo que haya quedado de una visita anterior al dashboard
    const prev = window.__dash;
    if (prev) {
        clearInterval(prev.timer);
        Object.values(prev.charts).forEach(c => c && c.dispose());
        window.removeEventListener('resize', prev.onResize);
    }
    const E = window.__dash = { charts: {}, datos: null, tipo: null, modo: 'total', timer: null };

    // Paleta: cada color es un degradado [claro, oscuro]
    const ORDEN_ESTADO = ['pendiente', 'en proceso', 'resuelto'];
    const PAL_ESTADO = {
        'pendiente':  ['#F6CF7A', '#D9A441'],   // dorado de la marca
        'en proceso': ['#7FD6E0', '#2FA3B8'],   // turquesa caribe
        'resuelto':   ['#8EE3B8', '#35B07F']    // menta
    };
    const PAL_TIPO = [['#FFB08F', '#E8734F'], ['#CFE6F2', '#7FB4CC'], ['#8EE3B8', '#35B07F'], ['#F6CF7A', '#D9A441']];
    const PAL_BARRIO = { total: ['#CFE6F2', '#7FB4CC'], pendiente: PAL_ESTADO['pendiente'], resuelto: PAL_ESTADO['resuelto'] };
    const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
    const SUAVE = '#a9b8c0';
    const TOOLTIP = { backgroundColor: 'rgba(10,14,20,0.96)', borderColor: 'rgba(255,255,255,0.14)', textStyle: { color: '#fff' } };

    const css = p => 'linear-gradient(90deg,' + p[0] + ',' + p[1] + ')';
    const egH = p => new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: p[0] }, { offset: 1, color: p[1] }]);
    const egD = p => new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: p[0] }, { offset: 1, color: p[1] }]);
    const egV = p => new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: p[0] }, { offset: 1, color: p[1] }]);
    const TT_BARRAS = Object.assign({ trigger: 'axis', axisPointer: { type: 'shadow' } }, TOOLTIP);

    const $ = id => document.getElementById(id);
    const poner = (id, v) => { const el = $(id); if (el) el.textContent = v; };
    const norm = t => (t || '').toLowerCase().replace(/_/g, ' ').trim();
    const bonito = t => { const s = norm(t).replace('publicos', 'públicos'); return s.charAt(0).toUpperCase() + s.slice(1); };
    const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
    // Mes (año*12 + mes) en hora de Bogotá (UTC-5, sin horario de verano)
    const claveMes = seg => { const d = new Date((seg - 5 * 3600) * 1000); return d.getUTCFullYear() * 12 + d.getUTCMonth(); };

    // ECharts se carga solo cuando se abre el dashboard
    function cargarEcharts() {
        if (window.echarts) return Promise.resolve();
        return new Promise((ok, fallo) => {
            const s = document.createElement('script');
            s.src = 'https://cdn.jsdelivr.net/npm/echarts@5.5.1/dist/echarts.min.js';
            s.onload = ok;
            s.onerror = fallo;
            document.head.appendChild(s);
        });
    }

    function grafica(id) {
        const el = $(id);
        if (!el) return null;
        if (!E.charts[id]) {
            const c = echarts.init(el);
            const original = c.setOption.bind(c);
            c.setOption = (o, sinMezclar) => original(Object.assign({ animationDuration: 450, animationDurationUpdate: 200 }, o), sinMezclar);
            E.charts[id] = c;
        }
        return E.charts[id];
    }

    // Solo se usan los estados que existen en el sistema (cualquier otro se ignora)
    function preparar(d) {
        const claveEstado = {};
        d.estados.forEach(e => { const k = norm(e.nombre); if (ORDEN_ESTADO.includes(k)) claveEstado[e.id] = k; });
        const barrio = {};
        d.barrios.forEach(b => { barrio[b.id] = b.nombre; });
        const reportes = d.reportes
            .filter(r => claveEstado[r.estadoId])
            .map(r => ({ estado: claveEstado[r.estadoId], tipo: r.tipoId, barrio: r.barrioId, creado: r.creado, mes: claveMes(r.creado) }));
        return { reportes, tipos: d.tipos, barrio };
    }

    const chip = (id, texto, n) =>
        '<button type="button" class="dash-pill' + (E.tipo === id ? ' activa' : '') + '" data-tipo="' + (id == null ? '' : id) + '">' + esc(texto) + ' · ' + n + '</button>';

    function pintar() {
        const D = E.datos;
        if (!D || !$('dash-raiz')) return;
        const todos = D.reportes;
        const rs = E.tipo == null ? todos : todos.filter(r => r.tipo === E.tipo);
        const ahora = Math.floor(Date.now() / 1000);
        const mesActual = claveMes(ahora);

        // Chips de tipo
        $('dash-chips').innerHTML = chip(null, 'Todos', todos.length)
            + D.tipos.map(t => chip(t.id, bonito(t.nombre), todos.filter(r => r.tipo === t.id).length)).join('');

        // Indicadores
        const resueltos = rs.filter(r => r.estado === 'resuelto').length;
        const esteMes = rs.filter(r => r.mes === mesActual).length;
        const mesPrev = rs.filter(r => r.mes === mesActual - 1).length;
        poner('k-total', rs.length);
        poner('k-mes', esteMes);
        poner('k-tasa', rs.length ? Math.round(resueltos * 100 / rs.length) + '%' : '0%');
        poner('k-viejos', rs.filter(r => r.estado === 'pendiente' && r.creado < ahora - 30 * 86400).length);
        const delta = $('k-delta');
        if (mesPrev > 0) {
            const p = Math.round((esteMes - mesPrev) * 100 / mesPrev);
            delta.textContent = (p > 0 ? '+' : '') + p + '% vs. mes anterior';
            delta.className = 'dash-delta ' + (p >= 0 ? 'sube' : 'baja');
            delta.hidden = false;
        } else {
            delta.hidden = true;
        }

        // 01 Reportes en el tiempo (últimos 12 meses, una línea por tipo)
        const claves = Array.from({ length: 12 }, (_, i) => mesActual - 11 + i);
        const series = D.tipos
            .filter(t => E.tipo == null || t.id === E.tipo)
            .map(t => {
                const p = PAL_TIPO[D.tipos.indexOf(t) % PAL_TIPO.length];
                return {
                    name: bonito(t.nombre), type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, showSymbol: false,
                    lineStyle: { width: 3, color: egH(p), shadowBlur: 14, shadowColor: p[1] + '99' },
                    itemStyle: { color: p[0], borderColor: '#0b1118', borderWidth: 2 },
                    areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: p[1] + '55' }, { offset: 1, color: p[1] + '00' }]) },
                    data: claves.map(c => todos.filter(r => r.tipo === t.id && r.mes === c).length)
                };
            });
        const g1 = grafica('g-tiempo');
        if (g1) g1.setOption({
            backgroundColor: 'transparent',
            grid: { left: 34, right: 12, top: 34, bottom: 24 },
            legend: { top: 0, left: 0, icon: 'circle', itemWidth: 10, textStyle: { color: SUAVE } },
            tooltip: Object.assign({ trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: 'rgba(255,255,255,0.25)' } } }, TOOLTIP),
            xAxis: { type: 'category', data: claves.map(c => MESES[c % 12]), boundaryGap: false, axisTick: { show: false }, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } }, axisLabel: { color: SUAVE } },
            yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.07)', type: 'dashed' } }, axisLabel: { color: SUAVE } },
            series
        }, true);

        // 02 Estado (dona + lista)
        const cuentas = ORDEN_ESTADO.map(k => rs.filter(r => r.estado === k).length);
        const g2 = grafica('g-estado');
        if (g2) g2.setOption({
            backgroundColor: 'transparent',
            tooltip: Object.assign({ trigger: 'item' }, TOOLTIP),
            title: { text: String(rs.length), subtext: 'reportes', left: 'center', top: 'center', itemGap: 0, textStyle: { color: '#fff', fontSize: 28, fontWeight: 800 }, subtextStyle: { color: SUAVE, fontSize: 12 } },
            series: [{
                type: 'pie', radius: ['66%', '88%'], label: { show: false },
                itemStyle: { borderColor: 'rgba(10,14,20,0.9)', borderWidth: 3, borderRadius: 8 },
                data: ORDEN_ESTADO.map((k, i) => ({ name: bonito(k), value: cuentas[i], itemStyle: { color: egD(PAL_ESTADO[k]) } }))
            }]
        }, true);
        $('l-estado').innerHTML = ORDEN_ESTADO.map((k, i) =>
            '<li><i style="background:' + css(PAL_ESTADO[k]) + '"></i>' + bonito(k) + '<b>' + cuentas[i] + '</b><span>'
            + (rs.length ? Math.round(cuentas[i] * 100 / rs.length) : 0) + '%</span></li>').join('');

        // 03 Tipo de reporte y estado (siempre con todos los reportes)
        $('g-tipos').innerHTML = D.tipos.map(t => {
            const mios = todos.filter(r => r.tipo === t.id);
            const n = mios.length;
            const c = ORDEN_ESTADO.map(k => mios.filter(r => r.estado === k).length);
            const pct = x => (n ? x * 100 / n : 0);
            return '<div class="dash-tipo"><div class="dash-tipo-cab"><span>' + esc(bonito(t.nombre)) + '</span><span>' + n + ' · '
                + Math.round(pct(c[2])) + '% resueltos</span></div><div class="dash-barra">'
                + ORDEN_ESTADO.map((k, i) => '<i style="width:' + pct(c[i]) + '%;background:' + css(PAL_ESTADO[k]) + '"></i>').join('')
                + '</div></div>';
        }).join('') || '<p class="dash-vacio">Sin datos</p>';

        // 04 Barrios con más reportes (el color de las barras sigue al estado elegido)
        document.querySelectorAll('#barrio-modo .dash-pill').forEach(b => b.classList.toggle('activa', b.dataset.modo === E.modo));
        const base = E.modo === 'total' ? rs : rs.filter(r => r.estado === E.modo);
        const conteo = {};
        base.forEach(r => { conteo[r.barrio] = (conteo[r.barrio] || 0) + 1; });
        const top = Object.entries(conteo).sort((a, b) => b[1] - a[1]).slice(0, 6);
        const max = top.length ? top[0][1] : 1;
        const colorBarra = css(PAL_BARRIO[E.modo] || PAL_BARRIO.total);
        $('g-barrios').innerHTML = top.length
            ? top.map(([id, n]) => {
                const nombre = esc(D.barrio[id] || 'Sin barrio');
                return '<div class="dash-barrio"><span title="' + nombre + '">' + nombre + '</span><em style="width:' + (n * 100 / max) + '%;background:' + colorBarra + '"></em><span>' + n + '</span></div>';
            }).join('')
            : '<p class="dash-vacio">Sin reportes</p>';

        // ===== Atención y tendencias =====
        const base2 = { backgroundColor: 'transparent', grid: { left: 34, right: 12, top: 30, bottom: 24 }, tooltip: TT_BARRAS };
        const ejeX = data => ({ type: 'category', data, axisTick: { show: false }, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } }, axisLabel: { color: SUAVE } });
        const ejeY = { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.07)', type: 'dashed' } }, axisLabel: { color: SUAVE } };
        const barra = (p, valor) => ({ value: valor, itemStyle: { color: egV(p), borderRadius: [8, 8, 0, 0] } });

        // 05 Antigüedad de los pendientes
        const rangos = [
            ['0-7 días', 0, 7, PAL_BARRIO.total],
            ['8-30 días', 8, 30, PAL_ESTADO['pendiente']],
            ['31-90 días', 31, 90, ['#FFC48F', '#F08A4B']],
            ['Más de 90', 91, Infinity, PAL_TIPO[0]]
        ];
        const edades = rs.filter(r => r.estado === 'pendiente').map(r => Math.max(0, Math.floor((ahora - r.creado) / 86400)));
        const g3 = grafica('g-edad');
        if (g3) g3.setOption(Object.assign({}, base2, {
            xAxis: ejeX(rangos.map(r => r[0])), yAxis: ejeY,
            series: [{ type: 'bar', barWidth: '46%', label: { show: true, position: 'top', color: '#fff' },
                data: rangos.map(r => barra(r[3], edades.filter(d => d >= r[1] && d <= r[2]).length)) }]
        }), true);

        // 06 Resolución por barrio (los que peor resuelven; solo barrios con 3 reportes o más)
        const porBarrio = {};
        rs.forEach(r => {
            const b = porBarrio[r.barrio] || (porBarrio[r.barrio] = { n: 0, ok: 0 });
            b.n++;
            if (r.estado === 'resuelto') b.ok++;
        });
        const peores = Object.entries(porBarrio)
            .filter(([, b]) => b.n >= 3)
            .map(([id, b]) => [id, b, Math.round(b.ok * 100 / b.n)])
            .sort((a, b) => a[2] - b[2] || b[1].n - a[1].n)
            .slice(0, 6);
        $('g-resol').innerHTML = peores.length
            ? peores.map(([id, b, pct]) => {
                const nombre = esc(D.barrio[id] || 'Sin barrio');
                return '<div class="dash-barrio ancho"><span title="' + nombre + '">' + nombre + '</span><em style="width:' + Math.max(pct, 2) + '%;background:' + css(PAL_ESTADO['resuelto']) + '"></em><span>' + pct + '% · ' + b.ok + '/' + b.n + '</span></div>';
            }).join('')
            : '<p class="dash-vacio">Aún no hay barrios con 3 reportes o más</p>';

        // 07 Reportes por día de la semana (hora de Bogotá)
        const dias = [0, 0, 0, 0, 0, 0, 0];
        rs.forEach(r => { const d = new Date((r.creado - 5 * 3600) * 1000).getUTCDay(); dias[(d + 6) % 7]++; });
        const maxDia = Math.max(...dias);
        const g4 = grafica('g-dia');
        if (g4) g4.setOption(Object.assign({}, base2, {
            xAxis: ejeX(['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']), yAxis: ejeY,
            series: [{ type: 'bar', barWidth: '46%', data: dias.map(v => barra(v === maxDia && v > 0 ? PAL_ESTADO['pendiente'] : PAL_BARRIO.total, v)) }]
        }), true);

        // 08 Este mes contra el mismo mes del año pasado, por tipo
        const tiposVis = D.tipos.filter(t => E.tipo == null || t.id === E.tipo);
        const nombreMes = MESES[mesActual % 12];
        const anioActual = Math.floor(mesActual / 12);
        const contar = (t, clave) => todos.filter(r => r.tipo === t.id && r.mes === clave).length;
        const g5 = grafica('g-anio');
        if (g5) g5.setOption(Object.assign({}, base2, {
            grid: { left: 34, right: 12, top: 34, bottom: 24 },
            legend: { top: 0, left: 0, icon: 'circle', itemWidth: 10, textStyle: { color: SUAVE } },
            xAxis: ejeX(tiposVis.map(t => bonito(t.nombre))), yAxis: ejeY,
            series: [
                { name: nombreMes + ' ' + anioActual, type: 'bar', barWidth: '26%', itemStyle: { color: egV(PAL_ESTADO['pendiente']), borderRadius: [8, 8, 0, 0] }, data: tiposVis.map(t => contar(t, mesActual)) },
                { name: nombreMes + ' ' + (anioActual - 1), type: 'bar', barWidth: '26%', itemStyle: { color: egV(PAL_BARRIO.total), borderRadius: [8, 8, 0, 0] }, data: tiposVis.map(t => contar(t, mesActual - 12)) }
            ]
        }), true);
    }

    async function traerTexto() {
        try {
            const r = await fetch('/admin/api/dashboard');
            return r.ok ? await r.text() : null;
        } catch (e) { return null; }
    }

    // Si los datos son iguales a los anteriores no se vuelve a dibujar nada
    function aplicar(texto) {
        if (texto === E.texto) return;
        E.texto = texto;
        E.datos = preparar(JSON.parse(texto));
        pintar();
    }

    async function cargar() {
        // Si ya no estamos en el dashboard, se detiene el refresco
        if (!$('dash-raiz')) { clearInterval(E.timer); return; }
        if (document.hidden && E.datos) return;   // pestaña en segundo plano: no gasta peticiones
        const texto = await traerTexto();
        if (texto) aplicar(texto);
    }

    // Filtros (los datos no se vuelven a pedir: todo se recalcula en el navegador)
    $('dash-chips').addEventListener('click', e => {
        const b = e.target.closest('[data-tipo]');
        if (!b) return;
        E.tipo = b.dataset.tipo === '' ? null : Number(b.dataset.tipo);
        pintar();
    });
    $('barrio-modo').addEventListener('click', e => {
        const b = e.target.closest('[data-modo]');
        if (!b) return;
        E.modo = b.dataset.modo;
        pintar();
    });

    E.onResize = () => Object.values(E.charts).forEach(c => c && c.resize());
    window.addEventListener('resize', E.onResize);

    // La librería y los datos se piden al mismo tiempo, no uno después del otro
    Promise.all([cargarEcharts(), traerTexto()])
        .then(([, texto]) => {
            if (texto) aplicar(texto);
            E.timer = setInterval(cargar, 30000);
        })
        .catch(() => console.error('No se pudo cargar el dashboard'));
})();