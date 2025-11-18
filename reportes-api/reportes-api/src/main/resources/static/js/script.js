document.addEventListener('DOMContentLoaded', function() {
    const lastView = sessionStorage.getItem('lastView');
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
                const url = form.getAttribute('action');
                const method = form.getAttribute('method') || 'POST';
                const formType = form.getAttribute('data-form-type'); 

                Swal.fire({
                    title: 'Procesando...',
                    html: 'Por favor, espera un momento.',
                    allowOutsideClick: false,
                    didOpen: () => Swal.showLoading()
                });
                
                fetch(url, { method: method, body: formData })
                    .then(response => {
                        if (!response.ok) throw new Error('La respuesta de la red no fue correcta');
                        return response.json();
                    })
                    .then(data => {
                        Swal.close();
                        if (data.success) {
                            showSuccessMessage(data.message || 'Operación realizada con éxito');
                            
                            if (formType === 'create') {
                                form.reset();
                                const barrioIdInput = document.getElementById('barrioId');
                                if (barrioIdInput) barrioIdInput.value = '';
                            } else {
                                const activeMenuItem = document.querySelector('.menu-item.active');
                                if (activeMenuItem) {
                                    loadView(activeMenuItem.getAttribute('data-view'));
                                }
                            }
                        } else {
                            showErrorMessage(data.error || 'Ocurrió un error durante la operación');
                        }
                    })
                    .catch(error => {
                        Swal.close();
                        showErrorMessage('Error en la solicitud: ' + error.message);
                        console.error('Error:', error);
                    });
            }
        });
    }

    setupMenuNavigation();
    setupLoginFeatures();
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

    contentArea.innerHTML = '<div class="card"><p style="text-align:center;">Cargando...</p></div>';
    
    fetch(view)
        .then(response => {
            if (!response.ok) throw new Error('Error en la respuesta del servidor');
            return response.text();
        })
        .then(html => {
            contentArea.innerHTML = html;
            if (view.includes('formulario-reporte') || view.includes('editar-reporte')) {
                setupBarrioDatalist();
            }
        })
        .catch(error => {
            console.error('Error al cargar la vista:', error);
            contentArea.innerHTML = '<div class="card error-message">Error al cargar el contenido. Por favor, inténtalo de nuevo.</div>';
        });
}


function setupBarrioDatalist() {
    const barrioInput = document.getElementById('barrioNombre');
    const barrioIdInput = document.getElementById('barrioId');
    
    if (barrioInput && barrioIdInput) {
        barrioInput.addEventListener('input', function() {
            const input = this.value;
            const options = document.querySelectorAll('#listaBarrios option');
            let barrioId = '';
            options.forEach(option => { if (option.value === input) barrioId = option.dataset.id; });
            barrioIdInput.value = barrioId;
        });
    }
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

            setTimeout(() => {
                loginForm.submit();
            }, 2000);
        });
    }
}

function loadEditForm(url) {
    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;

    contentArea.innerHTML = '<div class="card"><p style="text-align:center;">Cargando formulario...</p></div>';

    fetch(url)
        .then(response => response.text())
        .then(html => {
            contentArea.innerHTML = html;
            setupBarrioDatalist();
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
                .catch(error => {
                    showErrorMessage('Error en la solicitud: ' + error.message);
                    console.error('Error:', error);
                });
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
                .catch(error => {
                    showErrorMessage('Error en la solicitud: ' + error.message);
                    console.error('Error:', error);
                });
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
        title: title,
        text: text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí',
        cancelButtonText: 'No'
    }).then((result) => {
        if (result.isConfirmed) {
            onConfirm();
        }
    });
}

function logout() {
    showConfirmDialog(
        '¿Cerrar sesión?',
        'Se cerrará tu sesión actual.',
        () => {
            const loader = document.getElementById('loader');
            if (loader) {
                loader.classList.add('show');
            }
    
            sessionStorage.removeItem('lastView');
            setTimeout(() => {
                window.location.href = '/logout';
            }, 800); 
        }
    );
}