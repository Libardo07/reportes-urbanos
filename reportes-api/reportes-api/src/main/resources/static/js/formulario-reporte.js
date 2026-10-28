(function () {

    var mapa = null;
    var mapaListo = false;
    var pinReporte = null;
    var pinUsuario = null;
    var pinBarrio = null;
    var limites = null;
    var ignorarCambioBarrio = false;

    function pin(clase) {
        return L.divIcon({
            className: '',
            html: '<span class="pin-mapa ' + clase + '"></span>',
            iconSize: [20, 20],
            iconAnchor: [10, 10],
            popupAnchor: [0, -10]
        });
    }

    function inicializarMapa(lat, lng, zoom) {
        lat = lat || 10.3910;
        lng = lng || -75.4794;
        zoom = zoom || 13;

        limites = L.latLngBounds(L.latLng(10.2500, -75.6200), L.latLng(10.5200, -75.3800));

        mapa = L.map('mapa', { maxBounds: limites, maxBoundsViscosity: 1.0, minZoom: 12 }).setView([lat, lng], zoom);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
        }).addTo(mapa);

        mapa.on('click', function (e) {
            if (!limites.contains(e.latlng)) {
                mostrarBanner('denegado', 'Selecciona un punto dentro de Cartagena.');
                return;
            }
            colocarPinReporte(e.latlng);
            obtenerDireccion(e.latlng.lat, e.latlng.lng);
        });

        mapaListo = true;
        setTimeout(function () { mapa.invalidateSize(); }, 100);
    }

    // El pin del problema se puede arrastrar para ajustarlo
    function colocarPinReporte(latlng) {
        if (pinReporte) mapa.removeLayer(pinReporte);
        pinReporte = L.marker(latlng, { icon: pin('pin-reporte'), draggable: true }).addTo(mapa);
        pinReporte.on('dragend', function () {
            var pos = pinReporte.getLatLng();
            if (!limites.contains(pos)) {
                mostrarBanner('denegado', 'Selecciona un punto dentro de Cartagena.');
                return;
            }
            obtenerDireccion(pos.lat, pos.lng);
        });
    }

    function normalizar(texto) {
        return (texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
    }

    // Marca un campo por un momento para que se note que se llenó solo
    function resaltar(el) {
        if (!el) return;
        el.classList.remove('recien');
        void el.offsetWidth;
        el.classList.add('recien');
        setTimeout(function () { el.classList.remove('recien'); }, 1500);
    }

    // Busca el barrio en la lista del formulario y lo deja seleccionado. Devuelve el nombre o null.
    function seleccionarBarrio(nombre) {
        var buscado = normalizar(nombre);
        var items = document.querySelectorAll('#barrioLista li, #barrioLista option');
        var exacto = null;
        var parcial = null;

        for (var i = 0; i < items.length; i++) {
            var n = normalizar(items[i].textContent);
            if (!n) continue;
            if (n === buscado) { exacto = items[i]; break; }
            if (!parcial && (n.indexOf(buscado) !== -1 || buscado.indexOf(n) !== -1)) parcial = items[i];
        }

        var item = exacto || parcial;
        if (!item) return null;

        var nombreFinal = item.textContent.trim();
        ignorarCambioBarrio = true;

        if (item.tagName === 'LI') {
            item.click();
        } else {
            var oculto = document.getElementById('barrioId');
            oculto.value = item.value;
            oculto.dataset.nombre = nombreFinal;
            var vista = document.getElementById('barrioDisplay');
            if (vista) vista.textContent = nombreFinal;
        }

        setTimeout(function () { ignorarCambioBarrio = false; }, 80);
        resaltar(document.getElementById('barrioDisplay'));
        return nombreFinal;
    }

    function crearChip(texto, destacado) {
        var chip = document.createElement('span');
        chip.className = 'mapa-chip' + (destacado ? ' ok' : '');
        chip.textContent = texto;
        return chip;
    }

    // Ficha con la ubicación elegida, justo debajo del mapa
    function mostrarFicha(calle, ciudadEstado, barrio, barrioOsm, postal) {
        var ficha = document.getElementById('mapaFicha');
        if (!ficha) {
            ficha = document.createElement('div');
            ficha.id = 'mapaFicha';
            ficha.className = 'mapa-ficha';
            var info = document.querySelector('#mapa-container .mapa-info');
            info.parentNode.insertBefore(ficha, info);
        }
        ficha.hidden = false;
        ficha.innerHTML = '';

        var texto = document.createElement('div');
        texto.className = 'mapa-ficha-texto';

        var titulo = document.createElement('strong');
        titulo.textContent = calle || 'Ubicación seleccionada';

        var sub = document.createElement('span');
        sub.textContent = (ciudadEstado ? ciudadEstado + '. ' : '') + 'Ya la agregamos al formulario.';

        var chips = document.createElement('div');
        chips.className = 'mapa-ficha-chips';
        if (barrio) {
            chips.appendChild(crearChip('Barrio ' + barrio, true));
        } else {
            chips.appendChild(crearChip((barrioOsm ? 'Barrio ' + barrioOsm + ': ' : 'Barrio: ') + 'elígelo en la lista', false));
        }
        if (postal) chips.appendChild(crearChip('Código postal ' + postal, false));

        texto.appendChild(titulo);
        texto.appendChild(sub);
        texto.appendChild(chips);
        ficha.appendChild(texto);
    }


    function postalValido(codigo) {
        return /^13\d{4}$/.test((codigo || '').trim());
    }

    function obtenerDireccion(lat, lng) {
        fetch('https://nominatim.openstreetmap.org/reverse?format=json&addressdetails=1&zoom=18&lat=' + lat + '&lon=' + lng + '&accept-language=es')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (!data || !data.address) return;
                var a = data.address;

                var calle = a.road || a.pedestrian || a.footway || data.name || '';
                if (calle && a.house_number) calle += ' #' + a.house_number;
                var ciudad = a.city || a.town || a.village || a.municipality || '';
                var postal = postalValido(a.postcode) ? a.postcode.trim() : '';
                var ciudadEstado = [ciudad, a.state].filter(Boolean).join(', ');

                var campo = document.getElementById('direccion');
                campo.value = [calle, ciudad, a.state, postal].filter(Boolean).join(', ');
                resaltar(campo);

                var barrioOsm = a.suburb || a.neighbourhood || a.quarter || a.hamlet || '';
                var barrio = barrioOsm ? seleccionarBarrio(barrioOsm) : null;

                mostrarFicha(calle, ciudadEstado, barrio, barrioOsm, postal);
            })
            .catch(function () {});
    }

    function mostrarBanner(estado, texto) {
        var banner = document.getElementById('geo-banner');
        banner.className = estado;
        document.getElementById('geo-texto').textContent = texto;
        banner.querySelector('.geo-spinner').style.display = estado === 'localizando' ? 'block' : 'none';
    }

    function pedirGeolocalizacion() {
        if (!navigator.geolocation) {
            inicializarMapa();
            mostrarBanner('denegado', 'Tu navegador no soporta geolocalización. Puedes marcar el punto manualmente en el mapa.');
            return;
        }

        mostrarBanner('localizando', 'Obteniendo tu ubicación actual...');

        navigator.geolocation.getCurrentPosition(
            function (pos) {
                var lat = pos.coords.latitude;
                var lng = pos.coords.longitude;

                inicializarMapa(lat, lng, 16);

                pinUsuario = L.marker([lat, lng], { icon: pin('pin-usuario'), zIndexOffset: -1 })
                    .addTo(mapa)
                    .bindPopup('Estás aquí')
                    .openPopup();

                mostrarBanner('encontrado', 'Ubicación encontrada. Haz clic en el mapa para marcar el problema.');
            },
            function (err) {
                inicializarMapa();
                var msg;

                switch (err.code) {
                    case 1:
                        msg = 'Bloqueaste el acceso a tu ubicación. Para activarla, haz clic en el candado de la barra del navegador, entra a Permisos y permite la ubicación. También puedes marcar el punto manualmente.';
                        break;
                    case 2:
                        msg = 'No se pudo detectar tu ubicación. Revisa tu conexión a internet o marca el punto manualmente en el mapa.';
                        break;
                    case 3:
                        msg = 'Se agotó el tiempo para obtener tu ubicación. Puedes marcar el punto manualmente en el mapa.';
                        break;
                    default:
                        msg = 'No se pudo obtener tu ubicación. Puedes marcar el punto manualmente en el mapa.';
                }

                mostrarBanner('denegado', msg);
            },
            { timeout: 8000, maximumAge: 60000 }
        );
    }

    function buscarBarrioEnMapa(nombreBarrio) {
        var query = encodeURIComponent(nombreBarrio + ', Cartagena de Indias, Colombia');
        var url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + query + '&limit=1&accept-language=es';

        mostrarBanner('localizando', 'Buscando ' + nombreBarrio + ' en el mapa...');

        fetch(url)
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (!data || data.length === 0) {
                    mostrarBanner('denegado', 'No se encontró el barrio en el mapa.');
                    return;
                }

                var lat = parseFloat(data[0].lat);
                var lng = parseFloat(data[0].lon);

                mapa.setView([lat, lng], 15, { animate: true });
                if (pinBarrio) mapa.removeLayer(pinBarrio);
                pinBarrio = L.marker([lat, lng], { icon: pin('pin-barrio'), zIndexOffset: -2 })
                    .addTo(mapa)
                    .bindPopup(nombreBarrio)
                    .openPopup();

                mostrarBanner('encontrado', 'Barrio ' + nombreBarrio + ' encontrado. Haz clic para marcar el problema.');
            })
            .catch(function () {
                mostrarBanner('denegado', 'Error al buscar el barrio. Intenta de nuevo.');
            });
    }

    var btnMapa = document.getElementById('btnMapa');
    if (btnMapa) btnMapa.addEventListener('click', function () {
        var container = document.getElementById('mapa-container');
        var abrir = !container.classList.contains('abierto');

        container.classList.toggle('abierto', abrir);
        btnMapa.classList.toggle('activo', abrir);
        btnMapa.setAttribute('aria-expanded', String(abrir));
        if (!abrir) return;

        if (!mapaListo) pedirGeolocalizacion();

        // El mapa ya es visible: Leaflet recalcula su tamaño y se baja hasta él
        requestAnimationFrame(function () {
            if (mapa) mapa.invalidateSize();
            container.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    });


    var barrioIdEl = document.getElementById('barrioId');
    if (barrioIdEl) barrioIdEl.addEventListener('change', function () {
        if (ignorarCambioBarrio) return;
        var nombreBarrio = this.dataset.nombre || '';
        if (!nombreBarrio) return;
        var container = document.getElementById('mapa-container');
        if (mapaListo && container.classList.contains('abierto')) {
            buscarBarrioEnMapa(nombreBarrio);
        }
    });


    /* ---------- Fotos del reporte ---------- */
    window.imagenesSeleccionadas = [];
    var archivosSeleccionados = [];
    var MAX_IMAGENES = 3;
    var MAX_SIZE_MB = 5;

    var area = document.getElementById('imagenesArea');
    var placeholder = document.getElementById('imagenesPlaceholder');
    var preview = document.getElementById('imagenesPreview');
    var input = document.getElementById('imagenesInput');

    if (area && input) {
        area.addEventListener('click', function (e) {
            if (e.target.classList.contains('preview-remove')) return;
            if (e.target.classList.contains('preview-add')) return;
            if (archivosSeleccionados.length < MAX_IMAGENES) input.click();
        });

        area.addEventListener('dragover', function (e) {
            e.preventDefault();
            area.classList.add('arrastrando');
        });

        area.addEventListener('dragleave', function () {
            area.classList.remove('arrastrando');
        });

        area.addEventListener('drop', function (e) {
            e.preventDefault();
            area.classList.remove('arrastrando');
            procesarArchivos(Array.from(e.dataTransfer.files));
        });

        input.addEventListener('change', function () {
            procesarArchivos(Array.from(this.files));
            this.value = '';
        });
    }

    function procesarArchivos(archivos) {
        archivos.forEach(function (archivo) {
            if (window.imagenesSeleccionadas.length >= MAX_IMAGENES) return;
            if (!archivo.type.match(/image\/(jpeg|png|webp)/)) {
                alert('Solo se permiten imágenes JPG, PNG o WEBP.');
                return;
            }
            if (archivo.size > MAX_SIZE_MB * 1024 * 1024) {
                alert('La imagen "' + archivo.name + '" supera los 5MB.');
                return;
            }
            window.imagenesSeleccionadas.push(archivo);
        });
        archivosSeleccionados = window.imagenesSeleccionadas;
        actualizarPreview();
    }

    function actualizarPreview() {
        if (!preview || !placeholder) return;
        if (archivosSeleccionados.length === 0) {
            preview.style.display = 'none';
            placeholder.style.display = 'flex';
            return;
        }
        placeholder.style.display = 'none';
        preview.style.display = 'flex';
        preview.innerHTML = '';

        archivosSeleccionados.forEach(function (archivo, idx) {
            var item = document.createElement('div');
            item.className = 'preview-item';

            var img = document.createElement('img');
            img.src = URL.createObjectURL(archivo);

            var btn = document.createElement('button');
            btn.className = 'preview-remove';
            btn.type = 'button';
            btn.innerHTML = '&times;';
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                archivosSeleccionados.splice(idx, 1);
                actualizarPreview();
                sincronizarInput();
            });

            item.appendChild(img);
            item.appendChild(btn);
            preview.appendChild(item);
        });

        if (archivosSeleccionados.length < MAX_IMAGENES) {
            var addBtn = document.createElement('div');
            addBtn.className = 'preview-add';
            addBtn.innerHTML = '+';
            addBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                input.click();
            });
            preview.appendChild(addBtn);
        }
    }

    function sincronizarInput() {
        try {
            var dt = new DataTransfer();
            archivosSeleccionados.forEach(function (f) { dt.items.add(f); });
            input.files = dt.files;
        } catch (e) {
            console.error('Error sincronizando:', e);
        }
    }

})();