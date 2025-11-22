package com.reportes.urbanos.reportes_api.entity;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import com.reportes.urbanos.reportes_api.enums.Rol;
import jakarta.validation.constraints.Size;


@Entity // entidad en la base de datos
@Data // genera getters, setters, toString, equals, hashCode
@NoArgsConstructor // genera constructor sin parametros
@AllArgsConstructor // con parametros
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // genera id automaticamente
    private Long id;

    @NotBlank(message = "El nombre no puede estar vacío")// valida los campos no pueden estar vacios ni con espacios antes de guardarlos
    @Size(min = 4, message = "El nombre debe tener al menos 4 caracteres")
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El correo debe tener formato válido")// valida que el correo tenga formato valido
    @Size(min = 14, message = "El correo debe tener al menos 14 caracteres")
    @Column(unique = true, nullable = false) // valida que el correo sea unico 
    private String email;

    @NotBlank(message = "La contraseña es obligatoria" )
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(name = "fecha_creacion" , nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void asignarFechaCreacion() {
        this.fechaCreacion = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "usuario" , cascade = CascadeType.ALL, orphanRemoval = true) // relacion
    private List<Reporte> reportes;
    //Si se guarda, actualiza o elimina el usuario, también se hace lo mismo con sus reportes
   // Si se quita un reporte de la lista, también se elimina de la base de datos


}
