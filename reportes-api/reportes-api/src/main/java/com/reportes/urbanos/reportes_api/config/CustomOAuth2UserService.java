package com.reportes.urbanos.reportes_api.config;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String nombre = (String) attributes.get("name");

        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            // Si no existe lo creamos automáticamente como CIUDADANO
            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(nombre);
            usuario.setPassword(""); // sin contraseña porque usa Google
            usuario.setRol(Rol.CIUDADANO);
            usuario.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
            usuarioRepository.save(usuario);
        }

        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())),
            attributes,
            "email"
        );
    }
}
