document.addEventListener('DOMContentLoaded', function() {
    // Buscamos todos los íconos de ojo directamente
    const passwordToggles = document.querySelectorAll('.password-toggle');
    
    passwordToggles.forEach(toggle => {
        // Encontramos el input de contraseña en el mismo contenedor
        const passwordInput = toggle.parentElement.querySelector('input[type="password"]');
        
        toggle.addEventListener('click', function() {
            // Alternamos el tipo de input
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            
            // Alternamos la imagen del ícono
            const isPassword = type === 'password';
            this.src = isPassword ? '/images/icono-ojo-visible.png' : '/images/icono-ojo-oculto.png';
            this.alt = isPassword ? 'Mostrar contraseña' : 'Ocultar contraseña';
        });
    });

    // Efecto de enfoque en los campos (sin cambios)
    const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="password"]');
    
    inputs.forEach(input => {
        input.addEventListener('focus', function() {
            this.parentElement.classList.add('focused');
        });
        
        input.addEventListener('blur', function() {
            if (this.value === '') {
                this.parentElement.classList.remove('focused');
            }
        });
    });
});
