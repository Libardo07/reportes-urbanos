'use strict';

/* ─────────────────────────────────────────────────────────────
   Helpers globales
   ───────────────────────────────────────────────────────────── */
function esc(s) {
    return String(s)
        .replace(/&/g,'&amp;').replace(/</g,'&lt;')
        .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

/** Resalta @Nombre al inicio del texto con span azul */
function resaltarMencion(texto) {
    return esc(texto).replace(
        /^(@[\w\s]+?)(\s)/,
        '<span class="det-mention">$1</span>$2'
    );
}

/** HTML del avatar-wrap de un comentario raíz (con o sin línea) */
function avatarWrapComentario(inicial, conLinea) {
    return `
        <div class="det-comentario-avatar-wrap">
            <div class="det-comentario-avatar"><span>${esc(inicial)}</span></div>
            ${conLinea ? '<div class="det-avatar-line"></div>' : ''}
        </div>`;
}

/** HTML del avatar-wrap de una respuesta */
function avatarWrapReply(inicial) {
    return `
        <div class="det-reply-avatar-wrap">
            <div class="det-reply-avatar"><span>${esc(inicial)}</span></div>
            <div class="det-reply-line"></div>
        </div>`;
}

/** Formulario de respuesta para inyectar en comentarios creados dinámicamente */
function htmlReplyForm(reporteId, parentId, nombrePadre, miInicial) {
    return `
        <div class="det-reply-form" style="display:none;"
             data-reporte-id="${reporteId}" data-parent-id="${parentId}">
            <div class="det-reply-input-row">
                <div class="det-reply-avatar">${esc(miInicial)}</div>
                <textarea class="det-reply-textarea" maxlength="500" rows="2"
                          placeholder="Responder a ${esc(nombrePadre)}..."></textarea>
            </div>
            <div class="det-reply-footer">
                <span class="det-reply-char"><span class="det-reply-char-count">0</span>/500</span>
                <div class="det-reply-actions-btns">
                    <button class="det-reply-cancel-btn" onclick="cerrarReplyForm(this)">Cancelar</button>
                    <button class="det-reply-send-btn" onclick="enviarRespuestaDet(this)">
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                            <line x1="22" y1="2" x2="11" y2="13"/>
                            <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                        </svg>
                        Responder
                    </button>
                </div>
            </div>
        </div>`;
}

/* ─────────────────────────────────────────────────────────────
   Contador
   ───────────────────────────────────────────────────────────── */
function actualizarContadorDet(delta) {
    const el = document.getElementById('det-count');
    if (el) el.textContent = Math.max(0, parseInt(el.textContent || '0') + delta);
}

/* ─────────────────────────────────────────────────────────────
   Botón ver/ocultar respuestas
   ───────────────────────────────────────────────────────────── */
function actualizarVerBtn(body, repliesDiv, forzarAbrir) {
    const total = repliesDiv.querySelectorAll('.det-reply-item').length;
    let verBtn  = body.querySelector('.det-ver-respuestas');

    if (total === 0) { if (verBtn) verBtn.remove(); return; }

    if (!verBtn) {
        verBtn = document.createElement('button');
        verBtn.className = 'det-ver-respuestas';
        verBtn.onclick = function() { toggleVerRespuestas(this); };
        // Insertar antes del div de respuestas
        body.insertBefore(verBtn, repliesDiv);
    }

    const open = forzarAbrir === true ? true : !repliesDiv.classList.contains('det-replies-ocultas');
    verBtn.innerHTML = `
        <svg class="det-ver-icon" width="12" height="12" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2.5" stroke-linecap="round"
             style="transform:rotate(${open ? 180 : 0}deg);transition:transform .2s">
            <polyline points="6 9 12 15 18 9"/>
        </svg>
        <span class="det-ver-texto">${open
            ? `Ocultar respuestas (<span class="det-replies-count">${total}</span>)`
            : `Ver <span class="det-replies-count">${total}</span> respuesta(s)`
        }</span>`;
}

/** Muestra/oculta la línea vertical del avatar principal según haya respuestas */
function actualizarLineaAvatar(comentarioDiv, repliesDiv) {
    const wrap = comentarioDiv.querySelector('.det-comentario-avatar-wrap');
    if (!wrap) return;
    let linea = wrap.querySelector('.det-avatar-line');
    const hayRespuestas = repliesDiv && repliesDiv.querySelectorAll('.det-reply-item').length > 0;
    if (hayRespuestas && !linea) {
        linea = document.createElement('div');
        linea.className = 'det-avatar-line';
        wrap.appendChild(linea);
    } else if (!hayRespuestas && linea) {
        linea.remove();
    }
}

/* ─────────────────────────────────────────────────────────────
   Ver / ocultar respuestas
   ───────────────────────────────────────────────────────────── */
window.toggleVerRespuestas = function(btn) {
    const body       = btn.closest('.det-comentario-body');
    const repliesDiv = body.querySelector('.det-replies');
    if (!repliesDiv) return;
    const estaOculto = repliesDiv.classList.contains('det-replies-ocultas');
    repliesDiv.classList.toggle('det-replies-ocultas');
    actualizarVerBtn(body, repliesDiv, estaOculto);
};

/* ─────────────────────────────────────────────────────────────
   Toggle formulario de respuesta (comentario raíz)
   ───────────────────────────────────────────────────────────── */
window.toggleReplyForm = function(btn) {
    const body   = btn.closest('.det-comentario-body');
    const nombre = btn.getAttribute('data-nombre');

    // Crear form si no existe (comentarios creados dinámicamente)
    let form = body.querySelector('.det-reply-form');
    if (!form) {
        const comentarioDiv = btn.closest('.det-comentario');
        const reporteId     = btn.getAttribute('data-reporte-id');
        const parentId      = comentarioDiv.getAttribute('data-id');
        const miInicial     = btn.getAttribute('data-mi-inicial') || '?';
        body.insertAdjacentHTML('beforeend', htmlReplyForm(reporteId, parentId, nombre, miInicial));
        form = body.querySelector('.det-reply-form');
    }

    // Checar visibilidad ANTES de cerrar todo
    const estaVisible = form.style.display === 'flex';

    // Cerrar todos los forms
    document.querySelectorAll('.det-reply-form').forEach(f => {
        f.style.display = 'none';
    });

    // Si no estaba visible, abrirlo
    if (!estaVisible) {
        form.style.display = 'flex';
        const ta = form.querySelector('.det-reply-textarea');
        ta.value = `@${nombre} `;
        ta.focus();
        ta.selectionStart = ta.selectionEnd = ta.value.length;
        const cc = form.querySelector('.det-reply-char-count');
        if (!ta._listenerRegistrado) {
            ta.addEventListener('input', () => cc.textContent = ta.value.length);
            ta._listenerRegistrado = true;
        }
    }
};

/* ─────────────────────────────────────────────────────────────
   Responder a una respuesta (abre el form del comentario padre)
   ───────────────────────────────────────────────────────────── */
window.responderARespuesta = function(btn) {
    const nombre       = btn.getAttribute('data-nombre');
    const comentarioId = btn.getAttribute('data-comentario-id');
    const replyItem    = btn.closest('.det-reply-item');
    const comentarioDiv = document.querySelector(`.det-comentario[data-id="${comentarioId}"]`);
    if (!comentarioDiv) return;

    const reporteId = document.getElementById('det-btn-enviar')?.getAttribute('data-reporte-id') || '';
    const miInicial = document.querySelector('.det-avatar span')?.textContent?.trim() || '?';

    // Cerrar y eliminar todos los forms temporales
    document.querySelectorAll('.det-reply-form-temp').forEach(f => f.remove());
    document.querySelectorAll('.det-reply-form').forEach(f => f.style.display = 'none');

    // Crear form temporal debajo de esta respuesta
    const tempForm = document.createElement('div');
    tempForm.className = 'det-reply-form det-reply-form-temp';
    tempForm.setAttribute('data-reporte-id', reporteId);
    tempForm.setAttribute('data-parent-id', comentarioId);
    tempForm.style.display = 'flex';
    tempForm.innerHTML = `
        <div class="det-reply-input-row">
            <div class="det-reply-avatar">${esc(miInicial)}</div>
            <textarea class="det-reply-textarea" maxlength="500" rows="2"
                      placeholder="Responder a ${esc(nombre)}..."></textarea>
        </div>
        <div class="det-reply-footer">
            <span class="det-reply-char"><span class="det-reply-char-count">0</span>/500</span>
            <div class="det-reply-actions-btns">
                <button class="det-reply-cancel-btn" onclick="this.closest('.det-reply-form-temp').remove()">Cancelar</button>
                <button class="det-reply-send-btn" onclick="enviarRespuestaDet(this)">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
                         stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                        <line x1="22" y1="2" x2="11" y2="13"/>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                    </svg>
                    Responder
                </button>
            </div>
        </div>`;

    // Insertar después del reply-item
    replyItem.insertAdjacentElement('afterend', tempForm);

    const ta = tempForm.querySelector('.det-reply-textarea');
    ta.value = `@${nombre} `;
    ta.focus();
    ta.selectionStart = ta.selectionEnd = ta.value.length;
    const cc = tempForm.querySelector('.det-reply-char-count');
    ta.addEventListener('input', () => cc.textContent = ta.value.length);
};

/* ─────────────────────────────────────────────────────────────
   Cerrar formulario de respuesta
   ───────────────────────────────────────────────────────────── */
window.cerrarReplyForm = function(btn) {
    btn.closest('.det-reply-form').style.display = 'none';
};

/* ─────────────────────────────────────────────────────────────
   Enviar comentario raíz
   ───────────────────────────────────────────────────────────── */
window.enviarComentarioDet = function(btn) {
    const ta        = document.getElementById('det-textarea');
    const cc        = document.getElementById('det-char-count');
    const texto     = ta.value.trim();
    const reporteId = btn.getAttribute('data-reporte-id');
    const errDiv    = document.getElementById('det-error');

    // Registrar listener del contador si aún no está
    if (ta && cc && !ta._listenerRegistrado) {
        ta.addEventListener('input', () => cc.textContent = ta.value.length);
        ta._listenerRegistrado = true;
    }

    if (!texto) {
        errDiv.textContent = 'Escribe algo primero.';
        errDiv.style.display = 'block';
        return;
    }
    errDiv.style.display = 'none';
    btn.disabled    = true;
    btn.textContent = 'Enviando...';

    // Leer la inicial del usuario desde el avatar existente
    const miInicial = document.querySelector('.det-avatar span')?.textContent?.trim() || '?';

    fetch(`/reporte/${reporteId}/comentar`, { method: 'POST', body: new URLSearchParams({ texto }) })
    .then(r => r.json())
    .then(data => {
        if (data.success) {
            ta.value = ''; cc.textContent = '0';
            actualizarContadorDet(1);

            const lista = document.getElementById('det-lista');
            const vacio = lista.querySelector('.det-sin-comentarios');
            if (vacio) vacio.remove();

            const ahora = new Date();
            const fecha = ahora.toLocaleDateString('es-CO') + ' ' + ahora.toLocaleTimeString('es-CO',{hour:'2-digit',minute:'2-digit'});

            const div = document.createElement('div');
            div.className = 'det-comentario';
            div.setAttribute('data-id', data.id);
            const esAdmin = document.querySelector('.det-card')?.getAttribute('data-es-admin') === 'true';

            div.innerHTML = `
                ${avatarWrapComentario(data.inicial, false)}
                <div class="det-comentario-body">
                    <div class="det-comentario-header">
                        <span class="det-comentario-nombre">${esc(data.nombre)}</span>
                        <span class="det-comentario-fecha">${fecha}</span>
                        ${esAdmin ? `<button class="det-btn-eliminar" data-id="${data.id}" onclick="eliminarComentarioDet(this)" title="Eliminar">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                                <polyline points="3 6 5 6 21 6"/>
                                <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                            </svg>
                        </button>` : ''}
                    </div>
                    <p class="det-comentario-texto">${esc(data.texto)}</p>
                    <button class="det-btn-responder"
                            data-nombre="${esc(data.nombre)}"
                            data-reporte-id="${reporteId}"
                            data-mi-inicial="${esc(miInicial)}"
                            onclick="toggleReplyForm(this)">
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
                            stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                            <polyline points="9 14 4 9 9 4"/>
                            <path d="M20 20v-7a4 4 0 0 0-4-4H4"/>
                        </svg>
                        Responder
                    </button>
                </div>`;
            lista.insertBefore(div, lista.firstChild);
        } else {
            errDiv.textContent = data.error || 'Error al enviar.';
            errDiv.style.display = 'block';
        }
    })
    .catch(() => { errDiv.textContent = 'Error de conexión.'; errDiv.style.display = 'block'; })
    .finally(() => {
        btn.disabled = false;
        btn.innerHTML = `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Comentar`;
    });
};

/* ─────────────────────────────────────────────────────────────
   Enviar respuesta
   ───────────────────────────────────────────────────────────── */
window.enviarRespuestaDet = function(btn) {
    const form      = btn.closest('.det-reply-form');
    const ta        = form.querySelector('.det-reply-textarea');
    const texto     = ta.value.trim();
    const reporteId = form.getAttribute('data-reporte-id');
    const parentId  = form.getAttribute('data-parent-id');
    if (!texto) return;

    btn.disabled    = true;
    btn.textContent = 'Enviando...';

    const miInicial = document.querySelector('.det-avatar span')?.textContent?.trim() || '?';

    fetch(`/reporte/${reporteId}/responder/${parentId}`, { method: 'POST', body: new URLSearchParams({ texto }) })
    .then(r => r.json())
    .then(data => {
        if (data.success) {
            form.style.display = 'none';
            ta.value = '';
            actualizarContadorDet(1);

            const comentarioDiv = document.querySelector(`.det-comentario[data-id="${parentId}"]`);
            const body          = comentarioDiv.querySelector('.det-comentario-body');
            let repliesDiv      = body.querySelector('.det-replies');

            if (!repliesDiv) {
                repliesDiv = document.createElement('div');
                repliesDiv.className = 'det-replies';
                repliesDiv.style.cssText = 'display:flex;flex-direction:column;';
                body.appendChild(repliesDiv);
            } else {
                repliesDiv.classList.remove('det-replies-ocultas');
            }

            const ahora = new Date();
            const fecha = ahora.toLocaleDateString('es-CO') + ' ' + ahora.toLocaleTimeString('es-CO',{hour:'2-digit',minute:'2-digit'});

            const replyEl = document.createElement('div');
            replyEl.className = 'det-reply-item';
            replyEl.setAttribute('data-id', data.id);
            const esAdmin = document.querySelector('.det-card')?.getAttribute('data-es-admin') === 'true';

            replyEl.innerHTML = `
            ${avatarWrapReply(data.inicial)}
            <div class="det-reply-body">
                <div class="det-comentario-header">
                    <span class="det-comentario-nombre">${esc(data.nombre)}</span>
                    <span class="det-comentario-fecha">${fecha}</span>
                    ${esAdmin ? `<button class="det-btn-eliminar" data-id="${data.id}" onclick="eliminarComentarioDet(this)" title="Eliminar">
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                        </svg>
                    </button>` : ''}
                </div>
                <p class="det-comentario-texto">${resaltarMencion(data.texto)}</p>
                <button class="det-btn-responder"
                        data-nombre="${esc(data.nombre)}"
                        data-comentario-id="${parentId}"
                        onclick="responderARespuesta(this)">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
                        stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                        <polyline points="9 14 4 9 9 4"/>
                        <path d="M20 20v-7a4 4 0 0 0-4-4H4"/>
                    </svg>
                    Responder
                </button>
            </div>`;
            repliesDiv.appendChild(replyEl);

            // Ocultar línea del último reply anterior
            const items = repliesDiv.querySelectorAll('.det-reply-item');
            items.forEach((item, i) => {
                const linea = item.querySelector('.det-reply-line');
                if (linea) linea.style.display = i < items.length - 1 ? 'block' : 'none';
            });

            actualizarVerBtn(body, repliesDiv, true);
            actualizarLineaAvatar(comentarioDiv, repliesDiv);

        } else {
            alert(data.error || 'Error al responder.');
        }
    })
    .catch(() => { alert('Error de conexión.'); })
    .finally(() => {
        btn.disabled = false;
        btn.innerHTML = `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Responder`;
    });
};

/* ─────────────────────────────────────────────────────────────
   Eliminar comentario o respuesta (admin)
   ───────────────────────────────────────────────────────────── */
window.eliminarComentarioDet = function(btn) {
    const id  = btn.getAttribute('data-id');
    const row = btn.closest('.det-reply-item') || btn.closest('.det-comentario');

    Swal.fire({
        title: '¿Eliminar comentario?',
        text: 'Esta acción no se puede deshacer.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí',
        cancelButtonText: 'No'
    }).then(result => {
        if (!result.isConfirmed) return;

        fetch(`/admin/eliminar-comentario/${id}`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                row.style.opacity    = '0';
                row.style.transition = 'opacity .2s';
                setTimeout(() => {
                    const esRespuesta = row.classList.contains('det-reply-item');
                    if (esRespuesta) {
                        const body = row.closest('.det-comentario-body');
                        row.remove();
                        const repliesDiv = body.querySelector('.det-replies');
                        if (repliesDiv) actualizarVerBtn(body, repliesDiv, repliesDiv.style.display !== 'none');
                    } else {
                        row.remove();
                    }
                    actualizarContadorDet(-1);
                }, 200);
            } else {
                showErrorMessage(data.error || 'Error al eliminar.');
            }
        })
        .catch(() => showErrorMessage('Error de conexión.'));
    });
};

function inicializarComentarios() {
    document.querySelectorAll('.det-replies').forEach(repliesDiv => {
        const items = repliesDiv.querySelectorAll('.det-reply-item');
        items.forEach((item, i) => {
            const linea = item.querySelector('.det-reply-line');
            if (linea) linea.style.display = i < items.length - 1 ? 'block' : 'none';
        });
    });

    const ta = document.getElementById('det-textarea');
    const cc = document.getElementById('det-char-count');
    if (ta && cc && !ta._listenerRegistrado) {
        ta.addEventListener('input', () => cc.textContent = ta.value.length);
        ta._listenerRegistrado = true;
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inicializarComentarios);
} else {
    inicializarComentarios();
}

// ── Cerrar modal al hacer clic fuera ─────────────
document.addEventListener('click', function(e) {
    const overlay = document.getElementById('det-modal-eliminar');
    if (overlay && e.target === overlay) cerrarModalEliminar();
});