'use strict';
const canal = new BroadcastChannel('password_reset');
canal.onmessage = (e) => {
    if (e.data === 'restablecida') {
        canal.close();
        window.location.replace('/login?passwordRestablecida');
    }
};