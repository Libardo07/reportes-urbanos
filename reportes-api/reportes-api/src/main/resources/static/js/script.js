function setupRealtimeValidation() {
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');

    const validateEmail = () => {
        if (emailInput.value.length > 0 && emailInput.value.length < 8) {
            emailInput.setCustomValidity('El correo debe tener al menos 8 caracteres.');
        } else {
            emailInput.setCustomValidity('');
        }
    };

    const validatePassword = () => {
        if (passwordInput.value.length > 0 && passwordInput.value.length < 6) {
            passwordInput.setCustomValidity('La contraseña debe tener al menos 6 caracteres.');
        } else {
            passwordInput.setCustomValidity('');
        }
    };

    if (emailInput) emailInput.addEventListener('input', validateEmail);
    if (passwordInput) passwordInput.addEventListener('input', validatePassword);
}


document.addEventListener('DOMContentLoaded', function() {
    const lastView = sessionStorage.getItem('lastView');
    sessionStorage.removeItem('lastView');
    if (lastView) {
        setTimeout(() => {
            const menuItemToActivate = document.querySelector(`.menu-item[data-view="${lastView}"]`);
            if (menuItemToActivate) {
                menuItemToActivate.click();
            }
        }, 100);
    }

    const contentArea = document.getElementById('content-area');
    if (contentArea) {
        contentArea.addEventListener('click', function(event) {
            const deleteBtn = event.target.closest('.delete-reporte');
            if (deleteBtn) {
                event.preventDefault();
                const reporteId = deleteBtn.getAttribute('data-id');
                const deleteUrl = deleteBtn.getAttribute('data-url');
                const reloadViewUrl = deleteBtn.getAttribute('data-reload-view');
                confirmDeleteReporte(reporteId, deleteUrl, reloadViewUrl);
                return;
            }

            const editBtn = event.target.closest('.edit-reporte');
            if (editBtn) {
                event.preventDefault();
                const editUrl = editBtn.getAttribute('data-url');
                loadEditForm(editUrl);
                return;
            }

            const verDetallesBtn = event.target.closest('.ver-detalles-btn');
            if (verDetallesBtn) {
                const modalId = verDetallesBtn.getAttribute('data-modal-id');
                document.getElementById(modalId).style.display = 'flex';
                return;
            }

            const cerrarBtn = event.target.closest('.cerrar-modal');
            if (cerrarBtn) {
                const modalId = cerrarBtn.getAttribute('data-modal-id');
                document.getElementById(modalId).style.display = 'none';
                return;
            }

            if (event.target.classList.contains('modal-overlay')) {
                const modalId = event.target.getAttribute('data-modal-id');
                document.getElementById(modalId).style.display = 'none';
                return;
            }
        });

        contentArea.addEventListener('change', function(event) {
            if (event.target.classList.contains('estado-select')) {
                const reporteId = event.target.getAttribute('data-id');
                const nuevoEstado = event.target.value;
                if (nuevoEstado) {
                    confirmarCambioEstado(reporteId, nuevoEstado);
                }
            }
        });

        contentArea.addEventListener('submit', function(event) {
            const form = event.target.closest('form[data-ajax="true"]');
            if (form) {
                event.preventDefault();

                const formData = new FormData(form);
                formData.delete('imagenes');
                if (window.imagenesSeleccionadas && window.imagenesSeleccionadas.length > 0) {
                    window.imagenesSeleccionadas.forEach(function(file) {
                        formData.append('imagenes', file, file.name);
                    });
                }

                const url = form.getAttribute('action');
                const method = form.getAttribute('method') || 'POST';

                Swal.fire({
                    title: 'Procesando...',
                    html: 'Por favor, espera un momento.',
                    allowOutsideClick: false,
                    didOpen: () => Swal.showLoading()
                });

                fetch(url, { method: method, body: formData })
                    .then(async response => {
                        if (!response.ok) {
                            const errorData = await response.json().catch(() => null);
                            const mensajeEspecifico = errorData && (errorData.error || errorData.message)
                                ? errorData.error || errorData.message
                                : 'Error del servidor (' + response.status + ')';
                            throw new Error(mensajeEspecifico);
                        }
                        return response.json();
                    })
                    .then(data => {
                        Swal.close();
                        if (data.success) {
                            showSuccessMessage(data.message || 'Operación realizada con éxito');
                            const formType = form.getAttribute('data-form-type');

                            if (formType === 'create-admin') {
                                loadView('/admin/fragmento/lista-reportes');
                                return;
                            }

                            if (formType === 'create') {
                                form.reset();
                                const barrioIdInput = document.getElementById('barrioId');
                                if (barrioIdInput) barrioIdInput.value = '';
                                loadView('/usuario/fragmento/lista-reportes');
                            } else {
                                loadView('/usuario/fragmento/lista-reportes');
                            }
                        } else {
                            showErrorMessage(data.error || 'Ocurrió un error durante la operación');
                        }
                    })
                    .catch(error => {
                        Swal.close();
                        showErrorMessage('Error en la solicitud: ' + error.message);
                    });
            }
        });
    }

    setupMenuNavigation();
    setupLoginFeatures();
    setupRealtimeValidation();
    initBarrioFiltro();
    initIniciales();
});


function setupMenuNavigation() {
    const menuItems = document.querySelectorAll('.menu-item[data-view]');
    menuItems.forEach(item => {
        item.addEventListener('click', function() {
            menuItems.forEach(i => i.classList.remove('active'));
            this.classList.add('active');
            const view = this.getAttribute('data-view');
            sessionStorage.setItem('lastView', view);
            loadView(view);
        });
    });
}

function loadView(view) {
    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;

    contentArea.innerHTML = skeletonLoader();

    fetch(view)
        .then(response => {
            if (!response.ok) throw new Error('Error en la respuesta del servidor');
            return response.text();
        })
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const fragment = doc.querySelector('[th\\:fragment], div, table');
            contentArea.innerHTML = fragment ? fragment.outerHTML : html;

            if (view.includes('formulario-reporte') || view.includes('editar-reporte')) {
                setTimeout(function() { initBarrioFiltro(); }, 300);
                var scriptAnterior = document.querySelector('script[src*="formulario-reporte.js"]');
                if (scriptAnterior) scriptAnterior.remove();
                var script = document.createElement('script');
                script.src = '/js/formulario-reporte.js?t=' + Date.now();
                document.body.appendChild(script);
            }

            setupPasswordToggles();

            // Si es la vista de detalle, inicializar comentarios
            if (typeof inicializarComentarios === 'function') {
                inicializarComentarios();
            }
        })
        .catch(error => {
            console.error('Error al cargar la vista:', error);
            contentArea.innerHTML = '<div class="card error-message">Error al cargar el contenido.</div>';
        });
}


function setupLoginFeatures() {
    const passwordToggles = document.querySelectorAll('.password-toggle');
    passwordToggles.forEach(toggle => {
        const passwordInput = toggle.parentElement.querySelector('input[type="password"], input[type="text"]');
        if (!passwordInput) return;
        toggle.addEventListener('click', function() {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            this.src = type === 'password' ? '/images/icono-ojo-visible.png' : '/images/icono-ojo-oculto.png';
            this.alt = type === 'password' ? 'Mostrar contraseña' : 'Ocultar contraseña';
        });
    });

    const loginForm = document.querySelector('form[action="/login"]');
    const loader = document.getElementById('loader');
    if (loginForm && loader) {
        loginForm.addEventListener('submit', function(event) {
            event.preventDefault();
            loader.classList.add('show');
            setTimeout(() => { loginForm.submit(); }, 2000);
        });
    }
}

function setupPasswordToggles() {
    const toggles = document.querySelectorAll('.password-toggle-panel');
    toggles.forEach(toggle => {
        const passwordInput = toggle.closest('.password-wrapper').querySelector('input');
        if (!passwordInput) return;
        toggle.addEventListener('click', function() {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            this.src = type === 'password' ? '/images/icono-ojo-visible.png' : '/images/icono-ojo-oculto.png';
            this.alt = type === 'password' ? 'Mostrar contraseña' : 'Ocultar contraseña';
        });
    });
}

function loadEditForm(url) {
    const activeMenuItem = document.querySelector('.menu-item.active');
    if (activeMenuItem) {
        sessionStorage.setItem('returnToView', activeMenuItem.getAttribute('data-view'));
    }

    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;

    contentArea.innerHTML = skeletonLoader();

    fetch(url)
        .then(response => response.text())
        .then(html => {
            contentArea.innerHTML = html;
            initBarrioFiltro();
            setupPasswordToggles();

            var scriptAnterior = document.querySelector('script[src*="formulario-reporte.js"]');
            if (scriptAnterior) scriptAnterior.remove();
            var script = document.createElement('script');
            script.src = '/js/formulario-reporte.js?t=' + Date.now();
            document.body.appendChild(script);

            contentArea.scrollIntoView({ behavior: 'smooth', block: 'start' });
        })
        .catch(error => {
            console.error('Error al cargar el formulario de edición:', error);
            contentArea.innerHTML = '<div class="card error-message">Error al cargar el formulario.</div>';
        });
}

function confirmDeleteReporte(reporteId, url, reloadViewUrl) {
    showConfirmDialog(
        '¿Eliminar este reporte?',
        'Esta acción no se puede deshacer.',
        () => {
            fetch(url, { method: 'POST' })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        showSuccessMessage('Reporte eliminado correctamente');
                        loadView(reloadViewUrl);
                    } else {
                        showErrorMessage(data.error || 'Error al eliminar el reporte');
                    }
                })
                .catch(error => showErrorMessage('Error en la solicitud: ' + error.message));
        }
    );
}

function confirmarCambioEstado(reporteId, nuevoEstado) {
    showConfirmDialog(
        '¿Cambiar el estado del reporte?',
        `El reporte cambiará a estado: ${nuevoEstado.replace('_', ' ')}.`,
        () => {
            const formData = new FormData();
            formData.append('reporteId', reporteId);
            formData.append('nuevoEstado', nuevoEstado);

            fetch('/admin/cambiar-estado', { method: 'POST', body: formData })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        showSuccessMessage('Estado cambiado correctamente');
                        loadView('/admin/fragmento/lista-reportes');
                    } else {
                        showErrorMessage(data.error || 'Error al cambiar el estado');
                    }
                })
                .catch(error => showErrorMessage('Error en la solicitud: ' + error.message));
        }
    );
}

function showSuccessMessage(message) {
    Swal.fire({ icon: 'success', title: 'Éxito', text: message, timer: 3000, showConfirmButton: false });
}

function showErrorMessage(message) {
    Swal.fire({ icon: 'error', title: 'Error', text: message, confirmButtonText: 'OK' });
}

function showConfirmDialog(title, text, onConfirm) {
    Swal.fire({
        title: title, text: text, icon: 'warning',
        showCancelButton: true, confirmButtonText: 'Sí', cancelButtonText: 'No'
    }).then((result) => { if (result.isConfirmed) onConfirm(); });
}

function logout() {
    showConfirmDialog(
        '¿Cerrar sesión?',
        'Se cerrará tu sesión actual.',
        () => {
            sessionStorage.clear();
            const loader = document.getElementById('loader');
            if (loader) loader.classList.add('show');
            sessionStorage.removeItem('lastView');
            setTimeout(() => { window.location.href = '/logout'; }, 800);
        }
    );
}

function initBarrioFiltro() {
    const display  = document.getElementById('barrioDisplay');
    const dropdown = document.getElementById('barrioDropdown');
    const filtro   = document.getElementById('barrioFiltro');
    const lista    = document.getElementById('barrioLista');
    const hidden   = document.getElementById('barrioId');

    if (!display || !dropdown) return;

    const optionsSource = document.querySelectorAll('#barrioLista option');
    lista.innerHTML = '';

    optionsSource.forEach(opt => {
        const li = document.createElement('li');
        li.textContent = opt.textContent;
        li.dataset.id = opt.value;
        li.addEventListener('click', function() {
            hidden.value = this.dataset.id;
            hidden.dataset.nombre = this.textContent;
            display.textContent = this.textContent;
            display.classList.remove('abierto');
            dropdown.style.display = 'none';
            filtro.value = '';
            renderLista('');
            hidden.dispatchEvent(new Event('change'));
        });
        lista.appendChild(li);
    });

    display.addEventListener('click', function() {
        const abierto = dropdown.style.display === 'block';
        dropdown.style.display = abierto ? 'none' : 'block';
        display.classList.toggle('abierto', !abierto);
        if (!abierto) filtro.focus();
    });

    filtro.addEventListener('input', function() {
        renderLista(this.value.toLowerCase().trim());
    });

    document.addEventListener('click', function(e) {
        if (!display.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
            display.classList.remove('abierto');
        }
    });

    function renderLista(texto) {
        const items = lista.querySelectorAll('li');
        let visibles = 0;
        items.forEach(li => {
            const coincide = li.textContent.toLowerCase().includes(texto);
            li.style.display = coincide ? '' : 'none';
            if (coincide) visibles++;
        });
        const sinRes = lista.querySelector('.sin-resultados');
        if (sinRes) sinRes.remove();
        if (visibles === 0) {
            const li = document.createElement('li');
            li.className = 'sin-resultados';
            li.textContent = 'No se encontró ningún barrio';
            lista.appendChild(li);
        }
    }
}

if (window.location.search.includes('error')) {
    window.history.replaceState(null, '', '/login');
}

history.pushState(null, null, location.href);
window.onpopstate = function() { history.pushState(null, null, location.href); };

// ── Explorar reportes ───────────────────────────────────────────────────────
let searchTimeout = null;
let currentReporteId = null;

document.addEventListener('keydown', function(e) {
    if (e.target && e.target.id === 'exp-search' && e.key === 'Enter') {
        e.preventDefault();
        buscarReportes(e.target.value.trim(), 0);
    }
});

document.addEventListener('click', function(e) {
    if (e.target && (e.target.id === 'exp-search-btn' || e.target.closest('#exp-search-btn'))) {
        const input = document.getElementById('exp-search');
        if (input) buscarReportes(input.value.trim(), 0);
    }
});

function buscarReportes(q, page) {
    const url = `/reportes/fragmento/explorar?q=${encodeURIComponent(q)}&page=${page}`;
    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;
    fetch(url)
        .then(r => r.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const fragment = doc.querySelector('.card');
            if (fragment) contentArea.innerHTML = fragment.outerHTML;
        })
        .catch(console.error);
}

function limpiarBusqueda() {
    const input = document.getElementById('exp-search');
    if (input) { input.value = ''; input.focus(); }
    const clear = document.getElementById('exp-clear');
    if (clear) clear.style.display = 'none';
    buscarReportes('', 0);
}

function verMasReportes(btn) {
    const page = parseInt(btn.getAttribute('data-page'));
    const q    = btn.getAttribute('data-q') || '';
    const url  = `/reportes/fragmento/explorar?q=${encodeURIComponent(q)}&page=${page}`;

    btn.disabled    = true;
    btn.textContent = 'Cargando...';

    fetch(url)
        .then(r => r.text())
        .then(html => {
            const parser   = new DOMParser();
            const doc      = parser.parseFromString(html, 'text/html');
            const lista    = doc.querySelector('#exp-lista');
            const verMas   = doc.querySelector('.exp-ver-mas-wrap');
            const miLista  = document.getElementById('exp-lista');
            const miVerMas = document.querySelector('.exp-ver-mas-wrap');

            if (lista && miLista) {
                lista.querySelectorAll('.exp-card').forEach(card => miLista.appendChild(card));
            }
            if (miVerMas) miVerMas.remove();
            if (verMas) document.querySelector('.card').appendChild(verMas);
        })
        .catch(console.error);
}

// ── Ver detalle del reporte ─────────────────────────────────────────────────
function verDetalleReporte(id) {
    currentReporteId = id;
    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;

    contentArea.innerHTML = skeletonLoader();

    fetch(`/reportes/fragmento/detalle/${id}`)
        .then(r => r.text())
        .then(html => {
            const parser   = new DOMParser();
            const doc      = parser.parseFromString(html, 'text/html');
            const fragment = doc.querySelector('.card');
            if (fragment) contentArea.innerHTML = fragment.outerHTML;

            // Inicializar comentarios después de insertar el HTML
            if (typeof inicializarComentarios === 'function') {
                inicializarComentarios();
            }
        })
        .catch(console.error);
}

function volverAExplorar() {
    loadView('/reportes/fragmento/explorar');
}

function volverAExplorar() {
    loadView('/reportes/fragmento/explorar');
}

// ── Modal de foto ───────────────────────────────────────────────────────────
function abrirFotoModal(src) {
    const overlay = document.createElement('div');
    overlay.className = 'foto-modal-overlay';
    overlay.innerHTML = `
        <div class="foto-modal-inner">
            <img src="${src}" alt="Foto del reporte">
            <button class="foto-modal-close" onclick="cerrarFotoModal()">✕</button>
        </div>`;
    overlay.addEventListener('click', function(e) {
        if (e.target === overlay) cerrarFotoModal();
    });
    document.body.appendChild(overlay);
    document.addEventListener('keydown', cerrarFotoConEscape);
}

function cerrarFotoModal() {
    const overlay = document.querySelector('.foto-modal-overlay');
    if (overlay) overlay.remove();
    document.removeEventListener('keydown', cerrarFotoConEscape);
}

function cerrarFotoConEscape(e) {
    if (e.key === 'Escape') cerrarFotoModal();
}

function skeletonLoader() {
    return `<div class="card" style="padding:24px;">
        <div class="sk" style="height:22px;width:180px;margin-bottom:20px;border-radius:6px;"></div>
        <div style="display:flex;flex-direction:column;gap:14px;">
            <div style="display:flex;gap:12px;align-items:center;">
                <div class="sk" style="height:14px;flex:1;border-radius:6px;"></div>
                <div class="sk" style="height:14px;width:80px;border-radius:6px;"></div>
                <div class="sk" style="height:14px;width:60px;border-radius:6px;"></div>
                <div class="sk" style="height:28px;width:50px;border-radius:6px;"></div>
            </div>
            <div style="display:flex;gap:12px;align-items:center;">
                <div class="sk" style="height:14px;flex:1;border-radius:6px;"></div>
                <div class="sk" style="height:14px;width:80px;border-radius:6px;"></div>
                <div class="sk" style="height:14px;width:60px;border-radius:6px;"></div>
                <div class="sk" style="height:28px;width:50px;border-radius:6px;"></div>
            </div>
            <div style="display:flex;gap:12px;align-items:center;">
                <div class="sk" style="height:14px;flex:1;border-radius:6px;"></div>
                <div class="sk" style="height:14px;width:80px;border-radius:6px;"></div>
                <div class="sk" style="height:14px;width:60px;border-radius:6px;"></div>
                <div class="sk" style="height:28px;width:50px;border-radius:6px;"></div>
            </div>
        </div>
    </div>`;
}

// ── Filtro Admin ─────────────────────────────────────────────────────────────
let filtroBarriosData = [];

function toggleFiltroPanel() {
    const panel = document.getElementById('filtroPanel');
    const overlay = document.getElementById('filtroOverlay');
    if (!panel) return;
    const abriendo = !panel.classList.contains('abierto');
    panel.classList.toggle('abierto');
    if (overlay) {
        if (abriendo) {
            overlay.classList.add('activo');
        } else {
            overlay.classList.remove('activo');
        }
    }
    if (abriendo && filtroBarriosData.length === 0) {
        cargarBarriosFiltro();
    }
}

function cargarBarriosFiltro() {
    fetch('/admin/barrios-buscar?q=')
        .then(r => r.json())
        .then(data => {
            filtroBarriosData = data;
            renderFiltroBarrios('');
        })
        .catch(console.error);
}

function renderFiltroBarrios(texto) {
    const lista = document.getElementById('filtroBarrioLista');
    if (!lista) return;
    lista.innerHTML = '';
    const filtrados = filtroBarriosData.filter(b =>
        b.nombre.toLowerCase().includes(texto.toLowerCase()));
    if (filtrados.length === 0) {
        const li = document.createElement('li');
        li.className = 'sin-resultados';
        li.textContent = 'No se encontró ningún barrio';
        lista.appendChild(li);
        return;
    }
    filtrados.forEach(b => {
        const li = document.createElement('li');
        li.textContent = b.nombre;
        li.dataset.id = b.id;
        li.addEventListener('click', function() {
            document.getElementById('filtroBarrioId').value = b.id;
            document.getElementById('filtroBarrioDisplay').textContent = b.nombre;
            document.getElementById('filtroBarrioDropdown').style.display = 'none';
            document.getElementById('filtroBarrioDisplay').classList.remove('abierto');
        });
        lista.appendChild(li);
    });
}

document.addEventListener('click', function(e) {
    const display = document.getElementById('filtroBarrioDisplay');
    const dropdown = document.getElementById('filtroBarrioDropdown');
    if (!display || !dropdown) return;
    if (e.target === display) {
        const abierto = dropdown.style.display === 'block';
        dropdown.style.display = abierto ? 'none' : 'block';
        display.classList.toggle('abierto', !abierto);
        if (!abierto) {
            renderFiltroBarrios(document.getElementById('filtroBarrioFiltro').value);
            document.getElementById('filtroBarrioFiltro').focus();
        }
        return;
    }
    if (!dropdown.contains(e.target) && e.target !== display) {
        dropdown.style.display = 'none';
        display.classList.remove('abierto');
    }
});

document.addEventListener('input', function(e) {
    if (e.target && e.target.id === 'filtroBarrioFiltro') {
        renderFiltroBarrios(e.target.value);
    }
});

function limpiarFiltros() {
    document.getElementById('filtroEstadoSelect').value = '';
    document.getElementById('filtroTipoSelect').value = '';
    document.getElementById('filtroBarrioId').value = '';
    document.getElementById('filtroBarrioDisplay').textContent = 'Seleccione un barrio';
    document.getElementById('filtroBarrioFiltro').value = '';
    document.getElementById('filtroBarrioDropdown').style.display = 'none';
    document.getElementById('filtroFechaDesde').value = '';
    document.getElementById('filtroHoraDesde').value = '';
    document.getElementById('filtroFechaHasta').value = '';
    document.getElementById('filtroHoraHasta').value = '';
    renderFiltroBarrios('');

    // ← recargar tabla sin filtros
    const contentArea = document.getElementById('content-area');
    fetch('/admin/fragmento/lista-reportes')
        .then(r => r.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const fragment = doc.querySelector('.card');
            if (fragment) contentArea.innerHTML = fragment.outerHTML;
            const overlay = document.getElementById('filtroOverlay');
            if (overlay) overlay.classList.remove('activo');
        })
        .catch(console.error);
}

function aplicarFiltros() {
    const params = new URLSearchParams();
    const estadoId = document.getElementById('filtroEstadoSelect').value;
    const tipoId = document.getElementById('filtroTipoSelect').value;
    const barrioId = document.getElementById('filtroBarrioId').value;
    const barrioNombre = document.getElementById('filtroBarrioDisplay').textContent;
    const fechaDesde = document.getElementById('filtroFechaDesde').value;
    const horaDesde = document.getElementById('filtroHoraDesde').value;
    const fechaHasta = document.getElementById('filtroFechaHasta').value;
    const horaHasta = document.getElementById('filtroHoraHasta').value;

    if (estadoId) params.append('estadoId', estadoId);
    if (tipoId) params.append('tipoId', tipoId);
    if (barrioId) params.append('barrioId', barrioId);
    if (fechaDesde) params.append('fechaDesde', fechaDesde);
    if (horaDesde) params.append('horaDesde', horaDesde);
    if (fechaHasta) params.append('fechaHasta', fechaHasta);
    if (horaHasta) params.append('horaHasta', horaHasta);

    const contentArea = document.getElementById('content-area');
    contentArea.innerHTML = skeletonLoader();

    fetch(`/admin/fragmento/filtrar-reportes?${params.toString()}`)
        .then(r => r.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const fragment = doc.querySelector('.card');
            if (fragment) contentArea.innerHTML = fragment.outerHTML;


            // Restaurar valores
            document.getElementById('filtroEstadoSelect').value = estadoId;
            document.getElementById('filtroTipoSelect').value = tipoId;
            document.getElementById('filtroBarrioId').value = barrioId;
            document.getElementById('filtroFechaDesde').value = fechaDesde;
            document.getElementById('filtroHoraDesde').value = horaDesde;
            document.getElementById('filtroFechaHasta').value = fechaHasta;
            document.getElementById('filtroHoraHasta').value = horaHasta;
            if (barrioId) {
                document.getElementById('filtroBarrioDisplay').textContent = barrioNombre;
            }

            
            // Limpiar overlay
            const overlay = document.getElementById('filtroOverlay');
            if (overlay) overlay.classList.remove('activo');


        })
        .catch(console.error);
}

function initIniciales() {
        const el = document.querySelector('.usuario-inicial');
        const nombreEl = document.querySelector('.nombre');
        if (!el || !nombreEl) return;
        
        const nombre = nombreEl.textContent.trim();
        const partes = nombre.split(' ').filter(p => p.length > 0);
        const iniciales = partes.length >= 2
            ? (partes[0][0] + partes[1][0]).toUpperCase()
            : partes[0][0].toUpperCase();
        
        el.textContent = iniciales;
    }