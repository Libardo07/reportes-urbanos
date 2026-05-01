'use strict';

// Reemplaza el historial para que "atrás" vaya al login, no al token inválido
history.replaceState(null, '', '/login');