'use strict';

function esc(s) {
    return String(s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// Nombres de todas las personas que comentaron en este reporte (el más largo primero)
function nombresDelHilo() {
    const nombres = new Set();
    document.querySelectorAll('.det-comentario-nombre').forEach(n => {
        const nombre = n.textContent.trim();
        if (nombre) nombres.add(nombre);
    });
    return Array.from(nombres).sort((a, b) => b.length - a.length);
}

// Respuestas ANTIGUAS que empiezan con "@Nombre ...": se resalta el nombre completo,
// aunque tenga varias palabras. Las respuestas nuevas ya no usan esto: guardan a quién responden.
function resaltarMencion(texto) {
    const seguro = esc(texto);
    if (seguro.charAt(0) !== '@') return seguro;

    const nombres = nombresDelHilo();
    for (let i = 0; i < nombres.length; i++) {
        const marca = '@' + esc(nombres[i]);
        if (seguro.toLowerCase().startsWith(marca.toLowerCase())) {
            return '<span class="det-mention">' + seguro.slice(0, marca.length) + '</span>' + seguro.slice(marca.length);
        }
    }
    return seguro;
}

function sacarIniciales(nombre) {
    const partes = (nombre || '').trim().split(' ').filter(p => p.length > 0);
    if (partes.length === 0) return '?';
    return partes.length >= 2
        ? (partes[0][0] + partes[1][0]).toUpperCase()
        : partes[0][0].toUpperCase();
}

function miNombreActual() {
    const avatar = document.querySelector('.det-avatar');
    return (avatar && avatar.getAttribute('data-nombre')) || '?';
}

function avatarWrapComentario(nombre, conLinea) {
    return `
        <div class="det-comentario-avatar-wrap">
            <div class="det-comentario-avatar">${esc(sacarIniciales(nombre))}</div>
            ${conLinea ? '<div class="det-avatar-line"></div>' : ''}
        </div>`;
}

function avatarWrapReply(nombre) {
    return `
        <div class="det-reply-avatar-wrap">
            <div class="det-reply-avatar">${esc(sacarIniciales(nombre))}</div>
            <div class="det-reply-line"></div>
        </div>`;
}

const ICONO_RESPONDER = `
    <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <polyline points="9 14 4 9 9 4"/>
        <path d="M20 20v-7a4 4 0 0 0-4-4H4"/>
    </svg>`;

const ICONO_ENVIAR = `
    <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <line x1="22" y1="2" x2="11" y2="13"/>
        <polygon points="22 2 15 22 11 13 2 9 22 2"/>
    </svg>`;

// Pastilla "↪ Nombre": dice a qué comentario exacto se está respondiendo
function htmlPastillaRespuesta(respondeAId, nombre) {
    return `
        <button type="button" class="det-reply-a" data-target="${esc(respondeAId)}"
                onclick="irAComentario(this)" title="Ir al comentario de ${esc(nombre)}">
            ${ICONO_RESPONDER}
            <span>${esc(nombre)}</span>
        </button>`;
}

function htmlFormRespuesta(reporteId, parentId, respondeAId, nombreObjetivo) {
    return `
        <div class="det-reply-form" data-reporte-id="${esc(reporteId)}"
            data-parent-id="${esc(parentId)}" data-responde-a-id="${esc(respondeAId)}">
            <div class="det-reply-para">
                Respondiendo a <strong>${esc(nombreObjetivo)}</strong>
                <button type="button" class="det-reply-para-x" aria-label="Cancelar la respuesta"
                        onclick="cerrarReplyForm(this)">&times;</button>
            </div>
            <div class="det-reply-input-row">
                <div class="det-reply-avatar">${esc(sacarIniciales(miNombreActual()))}</div>
                <textarea class="det-reply-textarea" maxlength="500" rows="2"
                        placeholder="Escribe tu respuesta..."></textarea>
            </div>
            <div class="det-reply-footer">
                <span class="det-reply-char"><span class="det-reply-char-count">0</span>/500</span>
                <div class="det-reply-actions-btns">
                    <button type="button" class="det-reply-cancel-btn" onclick="cerrarReplyForm(this)">Cancelar</button>
                    <button type="button" class="det-reply-send-btn" onclick="enviarRespuestaDet(this)">
                        ${ICONO_ENVIAR}
                        Responder
                    </button>
                </div>
            </div>
        </div>`;
}

function actualizarContadorDet(delta) {
    const el = document.getElementById('det-count');
    if (el) el.textContent = Math.max(0, parseInt(el.textContent || '0') + delta);
}

function actualizarVerBtn(body, repliesDiv, forzarAbrir) {
    const total = repliesDiv.querySelectorAll('.det-reply-item').length;
    let verBtn  = body.querySelector('.det-ver-respuestas');

    if (total === 0) { if (verBtn) verBtn.remove(); return; }

    if (!verBtn) {
        verBtn = document.createElement('button');
        verBtn.className = 'det-ver-respuestas';
        verBtn.onclick = function() { toggleVerRespuestas(this); };
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

window.toggleVerRespuestas = function(btn) {
    const body       = btn.closest('.det-comentario-body');
    const repliesDiv = body.querySelector('.det-replies');
    if (!repliesDiv) return;
    const estaOculto = repliesDiv.classList.contains('det-replies-ocultas');
    repliesDiv.classList.toggle('det-replies-ocultas');
    actualizarVerBtn(body, repliesDiv, estaOculto);
};

/* ---------- Responder: siempre se sabe a qué comentario exacto se contesta ---------- */

function cerrarFormsRespuesta() {
    document.querySelectorAll('.det-reply-form').forEach(f => f.remove());
}

function abrirFormRespuesta(btn) {
    const nombre        = btn.getAttribute('data-nombre');
    const replyItem     = btn.closest('.det-reply-item');
    const comentarioDiv = btn.closest('.det-comentario');
    if (!comentarioDiv) return;

    const parentId    = comentarioDiv.getAttribute('data-id');
    const respondeAId = replyItem ? replyItem.getAttribute('data-id') : parentId;
    const btnEnviar   = document.getElementById('det-btn-enviar');
    const reporteId   = btnEnviar ? btnEnviar.getAttribute('data-reporte-id') : '';

    // Si ya está abierta la respuesta a este mismo comentario, se cierra
    const yaAbierta = document.querySelector('.det-reply-form[data-responde-a-id="' + respondeAId + '"]');
    cerrarFormsRespuesta();
    if (yaAbierta) return;

    const contenedor = document.createElement('div');
    contenedor.innerHTML = htmlFormRespuesta(reporteId, parentId, respondeAId, nombre).trim();
    const form = contenedor.firstElementChild;
    form.style.display = 'flex';

    if (replyItem) {
        replyItem.insertAdjacentElement('afterend', form);
    } else {
        comentarioDiv.querySelector('.det-comentario-body').appendChild(form);
    }

    const ta = form.querySelector('.det-reply-textarea');
    const cc = form.querySelector('.det-reply-char-count');
    ta.addEventListener('input', () => { cc.textContent = ta.value.length; });
    ta.addEventListener('keydown', e => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            enviarRespuestaDet(form.querySelector('.det-reply-send-btn'));
        }
    });
    ta.focus();
}

window.toggleReplyForm = function(btn) { abrirFormRespuesta(btn); };
window.responderARespuesta = function(btn) { abrirFormRespuesta(btn); };

window.cerrarReplyForm = function(btn) {
    const form = btn.closest('.det-reply-form');
    if (form) form.remove();
};

// Al tocar la pastilla, la página baja hasta el comentario original y lo resalta un momento
window.irAComentario = function(pastilla) {
    const id = pastilla.getAttribute('data-target');
    const destino = document.querySelector(
        '.det-comentario[data-id="' + id + '"], .det-reply-item[data-id="' + id + '"]');
    if (!destino) return;   // el comentario original fue eliminado

    const hilo = destino.closest('.det-replies');
    if (hilo && hilo.classList.contains('det-replies-ocultas')) {
        const cuerpo = hilo.closest('.det-comentario-body');
        hilo.classList.remove('det-replies-ocultas');
        actualizarVerBtn(cuerpo, hilo, true);
    }

    destino.scrollIntoView({ behavior: 'smooth', block: 'center' });
    destino.classList.remove('det-resaltado');
    void destino.offsetWidth;
    destino.classList.add('det-resaltado');
    setTimeout(() => destino.classList.remove('det-resaltado'), 1800);
};

window.enviarComentarioDet = function(btn) {
    const ta        = document.getElementById('det-textarea');
    const cc        = document.getElementById('det-char-count');
    const texto     = ta.value.trim();
    const reporteId = btn.getAttribute('data-reporte-id');
    const errDiv    = document.getElementById('det-error');

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

    const miNombre = miNombreActual();

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
            const fecha = ahora.toLocaleDateString('es-CO') + ' ' + ahora.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });

            const div = document.createElement('div');
            div.className = 'det-comentario';
            div.setAttribute('data-id', data.id);
            const esAdmin = document.querySelector('.det-card')?.getAttribute('data-es-admin') === 'true';

            div.innerHTML = `
                ${avatarWrapComentario(data.nombre, false)}
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
                            data-mi-nombre="${esc(miNombre)}"
                            onclick="toggleReplyForm(this)">
                        ${ICONO_RESPONDER}
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

window.enviarRespuestaDet = function(btn) {
    const form        = btn.closest('.det-reply-form');
    const ta          = form.querySelector('.det-reply-textarea');
    const texto       = ta.value.trim();
    const reporteId   = form.getAttribute('data-reporte-id');
    const parentId    = form.getAttribute('data-parent-id');
    const respondeAId = form.getAttribute('data-responde-a-id');
    if (!texto) return;

    btn.disabled    = true;
    btn.textContent = 'Enviando...';

    fetch(`/reporte/${reporteId}/responder/${parentId}`, {
        method: 'POST',
        body: new URLSearchParams({ texto, respondeAId })
    })
    .then(r => r.json())
    .then(data => {
        if (data.success) {
            form.remove();
            actualizarContadorDet(1);

            // El servidor dice en qué hilo quedó la respuesta
            const raizId        = data.parentId || parentId;
            const comentarioDiv = document.querySelector(`.det-comentario[data-id="${raizId}"]`);
            if (!comentarioDiv) return;

            const body     = comentarioDiv.querySelector('.det-comentario-body');
            let repliesDiv = body.querySelector('.det-replies');

            if (!repliesDiv) {
                repliesDiv = document.createElement('div');
                repliesDiv.className = 'det-replies';
                repliesDiv.style.cssText = 'display:flex;flex-direction:column;';
                body.appendChild(repliesDiv);
            } else {
                repliesDiv.classList.remove('det-replies-ocultas');
            }

            const ahora = new Date();
            const fecha = ahora.toLocaleDateString('es-CO') + ' ' + ahora.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });

            const replyEl = document.createElement('div');
            replyEl.className = 'det-reply-item';
            replyEl.setAttribute('data-id', data.id);
            const esAdmin = document.querySelector('.det-card')?.getAttribute('data-es-admin') === 'true';

            // La pastilla solo aparece si se respondió a otra respuesta (no al comentario principal)
            const pastilla = (data.respondeAId && data.respondeAId !== raizId && data.respondeANombre)
                ? htmlPastillaRespuesta(data.respondeAId, data.respondeANombre)
                : '';

            replyEl.innerHTML = `
            ${avatarWrapReply(data.nombre)}
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
                ${pastilla}
                <p class="det-comentario-texto det-reply-texto">${esc(data.texto)}</p>
                <button class="det-btn-responder"
                        data-nombre="${esc(data.nombre)}"
                        data-comentario-id="${raizId}"
                        onclick="responderARespuesta(this)">
                    ${ICONO_RESPONDER}
                    Responder
                </button>
            </div>`;
            repliesDiv.appendChild(replyEl);

            const items = repliesDiv.querySelectorAll('.det-reply-item');
            items.forEach((item, i) => {
                const linea = item.querySelector('.det-reply-line');
                if (linea) linea.style.display = i < items.length - 1 ? 'block' : 'none';
            });

            actualizarVerBtn(body, repliesDiv, true);
            actualizarLineaAvatar(comentarioDiv, repliesDiv);

        } else {
            btn.disabled = false;
            alert(data.error || 'Error al responder.');
        }
    })
    .catch(() => { btn.disabled = false; alert('Error de conexión.'); })
    .finally(() => {
        if (btn.isConnected) {
            btn.disabled = false;
            btn.innerHTML = `${ICONO_ENVIAR} Responder`;
        }
    });
};

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

function initInicialesComentarios() {
    document.querySelectorAll('.det-comentario-avatar, .det-reply-avatar, .det-avatar').forEach(function(el) {
        const nombre = el.getAttribute('data-nombre') || '';
        if (!nombre) return;
        el.textContent = sacarIniciales(nombre);
    });
}

function inicializarComentarios() {
    // Respuestas antiguas con "@Nombre ..." al principio: se resalta el nombre completo.
    // Las que ya traen la pastilla de "responde a" no necesitan esto.
    document.querySelectorAll('.det-reply-texto').forEach(p => {
        const item = p.closest('.det-reply-item');
        if (item && item.querySelector('.det-reply-a')) return;
        p.innerHTML = resaltarMencion(p.textContent);
    });

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

    setTimeout(initInicialesComentarios, 100);
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inicializarComentarios);
} else {
    inicializarComentarios();
}

document.addEventListener('click', function(e) {
    const overlay = document.getElementById('det-modal-eliminar');
    if (overlay && e.target === overlay && typeof cerrarModalEliminar === 'function') cerrarModalEliminar();
});