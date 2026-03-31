package com.reportes.urbanos.reportes_api.security;

import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.model.Usuario;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired private UsuarioRepository usuarioRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");

        if (usuarioRepository.findByEmail(email).isEmpty()) {
            Usuario nuevo = new Usuario();
            nuevo.setNombre(nombre);
            nuevo.setEmail(email);
            nuevo.setPassword("");
            nuevo.setRol(Rol.CIUDADANO);
            nuevo.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(nuevo);
        }

        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        if (usuario.get().getRol() == Rol.ADMIN) {
            getRedirectStrategy().sendRedirect(request, response, "/admin/inicio");
        } else {
            getRedirectStrategy().sendRedirect(request, response, "/usuario/inicio");
        }
    }
}