package com.reportes.urbanos.reportes_api.entity;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import com.reportes.urbanos.reportes_api.enums.Rol;


@Entity // entidad en la base de datos
@Data // genera getters, setters, toString, equals, hashCode
@NoArgsConstructor // genera constructor sin parametros
@AllArgsConstructor // con parametros
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // genera id automaticamente
    private Long id;

    @NotBlank(message = "El nombre no puede estar vacío")// valida los campos no pueden estar vacios ni con espacios antes de guardarlos
    @Column(nullable = false)
    private String nombre;

    @Email(message = "El correo debe tener formato válido") // valida que el correo tenga formato valido
    @Column(unique = true, nullable = false) // valida que el correo sea unico 
    private String email;

    @NotBlank(message = "La contraseña es obligatoria" )
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
