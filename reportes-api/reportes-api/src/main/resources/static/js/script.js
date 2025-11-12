// static/js/script.js - TODO EL JS CENTRALIZADO

document.addEventListener('DOMContentLoaded', function () {

    // ==================================================================
    // 1. TOGGLE CONTRASEÑA (login y registro)
    // ==================================================================
    const passwordToggles = document.querySelectorAll('.password-toggle');
    passwordToggles.forEach(toggle => {
        const passwordInput = toggle.parentElement.querySelector('input[type="password"], input[type="text"]');
        if (passwordInput) {
            toggle.addEventListener('click', function () {
                const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                passwordInput.setAttribute('type', type);
                this.src = type === 'password' ? '/images/icono-ojo-visible.png' : '/images/icono-ojo-oculto.png';
                this.alt = type === 'password' ? 'Mostrar contraseña' : 'Ocultar contraseña';
            });
        }
    });

    // ==================================================================
    // 2. BARRIO AUTOCOMPLETE (usuario_inicio y editar_reporte)
    // ==================================================================
    const barrioInput = document.getElementById('barrioNombre');
    const barrioIdInput = document.getElementById('barrioId');
    if (barrioInput && barrioIdInput) {
        barrioInput.addEventListener('input', function () {
            const input = this.value.trim();
            const options = document.querySelectorAll('#listaBarrios option');
            let selectedId = '';
            options.forEach(option => {
                if (option.value === input) {
                    selectedId = option.dataset.id;
                }
            });
            barrioIdInput.value = selectedId;
        });
    }

    // ==================================================================
    // 3. ADMIN: REGISTRAR ADMIN (AJAX)
    // ==================================================================
    const formRegistroAdmin = document.getElementById('formRegistroAdmin');
    if (formRegistroAdmin) {
        formRegistroAdmin.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData(this);

            fetch('/admin/registrar-admin', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (!response.ok) throw new Error('Error en la respuesta');
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    Swal.fire('Éxito', data.success, 'success').then(() => {
                        this.reset();
                        document.getElementById('dropdownContent').style.display = 'none';
                        location.reload();
                    });
                } else {
                    Swal.fire('Error', data.error || 'Error al registrar', 'error');
                }
            })
            .catch(err => {
                Swal.fire('Error', 'Error en la solicitud: ' + err.message, 'error');
            });
        });
    }

    // ==================================================================
    // 4. ADMIN: TOGGLE DROPDOWN
    // ==================================================================
    window.toggleDropdown = function () {
        const content = document.getElementById('dropdownContent');
        if (content) {
            content.style.display = content.style.display === 'block' ? 'none' : 'block';
        }
    };

    // Cerrar al hacer clic fuera
    document.addEventListener('click', function (e) {
        const dropdown = document.querySelector('.dropdown');
        const content = document.getElementById('dropdownContent');
        if (dropdown && content && !dropdown.contains(e.target)) {
            content.style.display = 'none';
        }
    });

    // ==================================================================
    // 5. ADMIN: CAMBIO DE ESTADO
    // ==================================================================
    window.mostrarConfirmacionCambioEstado = function (select) {
        const id = select.dataset.id;
        const nuevoEstado = select.value;
        if (!nuevoEstado) return;

        const estadoActual = select.closest('tr').querySelector('td:nth-child(6)').textContent.trim();
        if (estadoActual === 'RESUELTO') {
            Swal.fire('Error', 'No se puede cambiar un reporte ya resuelto.', 'error').then(() => {
                select.value = '';
            });
            return;
        }

        Swal.fire({
            title: '¿Cambiar estado?',
            text: `Cambiar a ${nuevoEstado.replace('_', ' ')}`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Sí',
            cancelButtonText: 'No'
        }).then(result => {
            if (result.isConfirmed) {
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = '/admin/cambiar-estado';
                form.innerHTML = `
                    <input type="hidden" name="reporteId" value="${id}">
                    <input type="hidden" name="nuevoEstado" value="${nuevoEstado}">
                `;
                document.body.appendChild(form);
                form.submit();
            } else {
                select.value = '';
            }
        });
    };

    // ==================================================================
    // 6. ADMIN: ELIMINAR REPORTE
    // ==================================================================
    window.confirmarEliminar = function (link) {
        event.preventDefault();
        const id = link.dataset.id;
        Swal.fire({
            title: '¿Eliminar reporte?',
            text: 'Esta acción no se puede deshacer.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'No'
        }).then(result => {
            if (result.isConfirmed) {
                window.location.href = `/admin/eliminar-reporte/${id}`;
            }
        });
    };

    // ==================================================================
    // 7. CIUDADANO: ENVIAR REPORTE
    // ==================================================================
    const formReporte = document.getElementById('formReporte');
    if (formReporte) {
        formReporte.addEventListener('submit', function (e) {
            e.preventDefault();
            Swal.fire({
                title: '¿Enviar reporte?',
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sí',
                cancelButtonText: 'No'
            }).then(result => {
                if (result.isConfirmed) {
                    this.submit();
                }
            });
        });
    }

    // ==================================================================
    // 8. CIUDADANO: EDITAR REPORTE
    // ==================================================================
    const formEditar = document.getElementById('formEditar');
    if (formEditar) {
        formEditar.addEventListener('submit', function (e) {
            e.preventDefault();
            Swal.fire({
                title: '¿Guardar cambios?',
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sí',
                cancelButtonText: 'No'
            }).then(result => {
                if (result.isConfirmed) {
                    Swal.fire({
                        title: 'Guardando...',
                        allowOutsideClick: false,
                        didOpen: () => Swal.showLoading()
                    });
                    setTimeout(() => this.submit(), 1000);
                }
            });
        });
    }

    // ==================================================================
    // 9. CIUDADANO: CONFIRMAR EDITAR
    // ==================================================================
    window.confirmarEditar = function (link) {
        event.preventDefault();
        const url = link.dataset.url;
        Swal.fire({
            title: '¿Editar reporte?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Sí',
            cancelButtonText: 'No'
        }).then(result => {
            if (result.isConfirmed) {
                window.location.href = url;
            }
        });
    };

    // ==================================================================
    // 10. CIUDADANO: MODAL REPORTES
    // ==================================================================
    window.abrirModal = function () {
        document.getElementById('fondoModal').style.display = 'block';
        document.getElementById('modalReportes').style.display = 'block';
    };

    window.cerrarModal = function () {
        document.getElementById('fondoModal').style.display = 'none';
        document.getElementById('modalReportes').style.display = 'none';
    };

    // ==================================================================
    // 11. BOTÓN SALIR (TODAS LAS PÁGINAS)
    // ==================================================================
    const btnSalir = document.getElementById('btnSalir');
    if (btnSalir) {
        btnSalir.addEventListener('click', () => {
            Swal.fire({
                title: '¿Cerrar sesión?',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Sí',
                cancelButtonText: 'No'
            }).then(result => {
                if (result.isConfirmed) {
                    window.location.href = '/logout';
                }
            });
        });
    }


});