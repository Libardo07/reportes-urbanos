/* ============================================================
   formulario-reporte.js
   Lógica del mapa, geolocalización y autocompletado de dirección
   ============================================================ */

(function () {

    var mapaInicializado = false;
    var mapa;
    var marcadorUsuario = null;  // marcador azul = posición del usuario
    var marcadorReporte = null;  // marcador rojo = punto seleccionado
    var marcadorBarrio  = null;  // marcador naranja = referencia del barrio seleccionado

    /* ----------------------------------------------------------
       inicializarMapa
       Crea el mapa centrado en las coordenadas dadas,
       restringido a los límites geográficos de Cartagena
    ---------------------------------------------------------- */
    function inicializarMapa(latInicial, lngInicial, zoom) {
        latInicial = latInicial || 10.3910;
        lngInicial = lngInicial || -75.4794;
        zoom       = zoom       || 13;

        // Límites geográficos de Cartagena
        var limites = L.latLngBounds(
            L.latLng(10.2500, -75.6200),  // esquina suroeste
            L.latLng(10.5200, -75.3800)   // esquina noreste
        );

        mapa = L.map('mapa', {
            maxBounds: limites,
            maxBoundsViscosity: 1.0,  // impide arrastrar fuera del área
            minZoom: 12               // no permite alejar demasiado
        }).setView([latInicial, lngInicial], zoom);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
        }).addTo(mapa);

        mapa.on('click', function (e) {
            var lat = e.latlng.lat;
            var lng = e.latlng.lng;

            // Validar que el clic esté dentro de Cartagena
            if (!limites.contains(e.latlng)) {
                mostrarBanner('denegado', 'Selecciona un punto dentro de Cartagena.');
                return;
            }

            if (marcadorReporte) mapa.removeLayer(marcadorReporte);
            marcadorReporte = L.marker([lat, lng]).addTo(mapa);
            obtenerDireccion(lat, lng);
        });

        mapaInicializado = true;
        setTimeout(function () { mapa.invalidateSize(); }, 100);
    }

    /* ----------------------------------------------------------
       obtenerDireccion
       Hace geocodificación inversa con Nominatim,
       rellena el campo dirección (sin barrio ni país)
       y auto-selecciona el barrio en el select
    ---------------------------------------------------------- */
    function obtenerDireccion(lat, lng) {
        fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng + '&accept-language=es')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (!data || !data.address) return;
                var a = data.address;

                // Valores que pertenecen al barrio: se excluyen del campo dirección
                var valoresBarrio = [a.suburb, a.neighbourhood, a.quarter, a.hamlet].filter(Boolean);

                // Dirección en orden: calle, número, distrito, ciudad, departamento, CP
                var partes = [
                    a.road,
                    a.house_number,
                    a.city_district,
                    a.municipality,
                    a.city || a.town || a.village,
                    a.state
                ].filter(function (v) {
                    if (!v) return false;
                    if (v.toLowerCase() === 'colombia') return false;
                    if (valoresBarrio.indexOf(v) !== -1) return false;
                    return true;
                });

                document.getElementById('direccion').value = partes.join(', ');

                // Auto-seleccionar barrio en el select si coincide con Nominatim
                var nombreBarrio = a.suburb || a.neighbourhood || a.quarter || null;
                if (nombreBarrio) {
                    var selectBarrio = document.getElementById('barrioId');
                    var opciones = selectBarrio.options;
                    for (var i = 0; i < opciones.length; i++) {
                        if (opciones[i].text.toLowerCase() === nombreBarrio.toLowerCase()) {
                            selectBarrio.value = opciones[i].value;
                            break;
                        }
                    }
                }
            })
            .catch(function () {
                // Si falla la petición no pisamos los campos
            });
    }

    /* ----------------------------------------------------------
       mostrarBanner
       Actualiza el banner de estado de geolocalización
       estados: 'localizando' | 'encontrado' | 'denegado'
    ---------------------------------------------------------- */
    function mostrarBanner(estado, texto) {
        var banner  = document.getElementById('geo-banner');
        var spinner = banner.querySelector('.geo-spinner');
        banner.className = '';
        banner.id = 'geo-banner';
        banner.classList.add(estado);
        document.getElementById('geo-texto').textContent = texto;
        spinner.style.display = estado === 'localizando' ? 'block' : 'none';
    }

    /* ----------------------------------------------------------
       pedirGeolocalizacion
       Solicita la ubicación del usuario al abrir el mapa.
       Si se concede, centra el mapa en su posición.
       Si se deniega o falla, carga centrado en Cartagena.
    ---------------------------------------------------------- */
    function pedirGeolocalizacion() {
        if (!navigator.geolocation) {
            inicializarMapa();
            return;
        }

        mostrarBanner('localizando', 'Obteniendo tu ubicación actual...');

        navigator.geolocation.getCurrentPosition(
            // Éxito
            function (pos) {
                var lat = pos.coords.latitude;
                var lng = pos.coords.longitude;

                inicializarMapa(lat, lng, 16);

                // Marcador azul pulsante para la posición del usuario
                var iconoAzul = L.divIcon({
                    className: '',
                    html: '<div style="width:16px;height:16px;background:#3498db;border:3px solid white;border-radius:50%;box-shadow:0 0 0 3px rgba(52,152,219,0.35);"></div>',
                    iconSize: [16, 16],
                    iconAnchor: [8, 8]
                });

                marcadorUsuario = L.marker([lat, lng], { icon: iconoAzul, zIndexOffset: -1 })
                    .addTo(mapa)
                    .bindPopup('📍 Estás aquí')
                    .openPopup();

                mostrarBanner('encontrado', 'Ubicación encontrada. Haz clic en el mapa para marcar el problema.');
            },
            // Error (denegado o timeout)
            function (err) {
                inicializarMapa();
                var msg = err.code === 1
                    ? 'Permiso denegado. Puedes marcar la ubicación manualmente.'
                    : 'No se pudo obtener la ubicación. Marca el punto en el mapa.';
                mostrarBanner('denegado', msg);
            },
            { timeout: 8000, maximumAge: 60000 }
        );
    }

    /* ----------------------------------------------------------
       buscarBarrioEnMapa
       Cuando el usuario elige un barrio en el select,
       busca sus coordenadas en Nominatim y centra el mapa ahí.
    ---------------------------------------------------------- */
    function buscarBarrioEnMapa(nombreBarrio) {
        var query = encodeURIComponent(nombreBarrio + ', Cartagena de Indias, Colombia');
        var url   = 'https://nominatim.openstreetmap.org/search?format=json&q=' + query + '&limit=1&accept-language=es';

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

                // Marcador naranja de referencia del barrio
                if (marcadorBarrio) mapa.removeLayer(marcadorBarrio);
                var iconoBarrio = L.divIcon({
                    className: '',
                    html: '<div style="width:14px;height:14px;background:#e67e22;border:3px solid white;border-radius:50%;box-shadow:0 0 0 3px rgba(230,126,34,0.35);"></div>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                });
                marcadorBarrio = L.marker([lat, lng], { icon: iconoBarrio, zIndexOffset: -2 })
                    .addTo(mapa)
                    .bindPopup('📌 ' + nombreBarrio)
                    .openPopup();

                mostrarBanner('encontrado', 'Barrio ' + nombreBarrio + ' encontrado. Haz clic para marcar el problema.');
            })
            .catch(function () {
                mostrarBanner('denegado', 'Error al buscar el barrio. Intenta de nuevo.');
            });
    }

    /* ----------------------------------------------------------
       Evento: abrir / cerrar el mapa
    ---------------------------------------------------------- */
    document.getElementById('btnMapa').addEventListener('click', function () {
        var container = document.getElementById('mapa-container');
        if (container.style.display === 'none' || container.style.display === '') {
            container.style.display = 'block';
            if (!mapaInicializado) {
                pedirGeolocalizacion();
            } else {
                setTimeout(function () { mapa.invalidateSize(); }, 100);
            }
        } else {
            container.style.display = 'none';
        }
    });

    /* ----------------------------------------------------------
       Evento: cambio de barrio en el select
       Si el mapa no está abierto lo abre, luego busca el barrio
    ---------------------------------------------------------- */
    document.getElementById('barrioId').addEventListener('change', function () {
        var nombreBarrio = this.options[this.selectedIndex].text;
        if (!nombreBarrio) return;

        var container = document.getElementById('mapa-container');

        if (!mapaInicializado) {
            container.style.display = 'block';
            inicializarMapa();
            setTimeout(function () { buscarBarrioEnMapa(nombreBarrio); }, 300);
        } else {
            if (container.style.display === 'none' || container.style.display === '') {
                container.style.display = 'block';
                setTimeout(function () { mapa.invalidateSize(); }, 100);
            }
            buscarBarrioEnMapa(nombreBarrio);
        }
    });

})();