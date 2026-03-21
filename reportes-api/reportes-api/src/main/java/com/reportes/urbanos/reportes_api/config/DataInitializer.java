package com.reportes.urbanos.reportes_api.config;

import com.reportes.urbanos.reportes_api.entity.Barrio;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.BarrioRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository == null) {
            System.err.println("Error: UsuarioRepository no ha sido inyectado.");
            return;
        }

        if (usuarioRepository.findByEmail("adminMain@gmail.com") == null) {
            Usuario adminPrincipal = new Usuario();
            adminPrincipal.setNombre("adminMain");
            adminPrincipal.setEmail("adminMain@gmail.com");
            adminPrincipal.setPassword("adminMain");
            adminPrincipal.setRol(Rol.ADMIN);
            adminPrincipal.setFechaCreacion(LocalDateTime.now());
            try {
                usuarioRepository.save(adminPrincipal);
                System.out.println("Admin principal creado exitosamente.");
            } catch (Exception e) {
                System.err.println("Error al guardar el admin principal: " + e.getMessage());
            }
        }

        if (barrioRepository.count() == 0) {
            List<Barrio> barrios = List.of(
                new Barrio("1", "Daniel Lemaitre"),
                new Barrio("2", "Santa Maria"),
                new Barrio("3", "Canapote"),
                new Barrio("4", "San Pedro Y Libertad"),
                new Barrio("5", "Siete De Agosto"),
                new Barrio("6", "Torices"),
                new Barrio("7", "San Francisco"),
                new Barrio("8", "Pablo Vi - Ii"),
                new Barrio("9", "Pedro Salazar"),
                new Barrio("10", "Los Comuneros"),
                new Barrio("11", "Petare"),
                new Barrio("12", "Palestina"),
                new Barrio("13", "Republica Del Caribe"),
                new Barrio("14", "Paraiso Ii"),
                new Barrio("15", "La Paz"),
                new Barrio("16", "Loma Fresca"),
                new Barrio("17", "Pablo Vi - I"),
                new Barrio("18", "San Diego"),
                new Barrio("19", "Cerro De La Popa"),
                new Barrio("20", "San Bernardo"),
                new Barrio("21", "Nariño"),
                new Barrio("22", "Espinal"),
                new Barrio("23", "Lo Amador"),
                new Barrio("24", "El Pozon"),
                new Barrio("25", "Pie De La Popa"),
                new Barrio("26", "La Quinta"),
                new Barrio("27", "La Candelaria"),
                new Barrio("28", "Boston"),
                new Barrio("29", "Barrio Chino"),
                new Barrio("30", "Republica Del Libano"),
                new Barrio("31", "Alcibia"),
                new Barrio("32", "El Prado"),
                new Barrio("33", "Martinez Martelo"),
                new Barrio("34", "Tesca"),
                new Barrio("35", "Villa Estrella"),
                new Barrio("36", "Amberes"),
                new Barrio("37", "España"),
                new Barrio("38", "Armenia"),
                new Barrio("39", "Bruselas"),
                new Barrio("40", "Chiquinquira"),
                new Barrio("41", "Republica De Venezuela"),
                new Barrio("42", "Zaragocilla"),
                new Barrio("43", "Escallon Villa"),
                new Barrio("44", "Las Gaviotas"),
                new Barrio("45", "Piedra De Bolivar"),
                new Barrio("46", "San Jose Obrero"),
                new Barrio("47", "Paraguay"),
                new Barrio("48", "Nuevo Porvenir"),
                new Barrio("49", "Las Palmeras"),
                new Barrio("50", "Juan Xxiii"),
                new Barrio("51", "Junin"),
                new Barrio("52", "Los Alpes"),
                new Barrio("53", "Jose Antonio Galan"),
                new Barrio("54", "Nueve De Abril"),
                new Barrio("55", "Los Ejecutivos"),
                new Barrio("56", "San Isidro"),
                new Barrio("57", "Villa Rosita"),
                new Barrio("58", "Republica De Chile"),
                new Barrio("59", "El Gallo"),
                new Barrio("60", "La Castellana"),
                new Barrio("61", "La Floresta"),
                new Barrio("62", "Chipre"),
                new Barrio("63", "Los Angeles"),
                new Barrio("64", "Anita"),
                new Barrio("65", "Nueva Granada"),
                new Barrio("66", "San Antonio"),
                new Barrio("67", "Altos De San Isidro"),
                new Barrio("68", "Bosquecito"),
                new Barrio("69", "La Campiña"),
                new Barrio("70", "Camaguey"),
                new Barrio("71", "Los Cerros"),
                new Barrio("72", "Santa Lucia"),
                new Barrio("73", "La Concepcion"),
                new Barrio("74", "Villa Sandra"),
                new Barrio("75", "Nuevo Bosque"),
                new Barrio("76", "Rubi"),
                new Barrio("77", "Buenos Aires"),
                new Barrio("78", "Las Delicias"),
                new Barrio("79", "El Country"),
                new Barrio("80", "San Pedro"),
                new Barrio("81", "El Carmen"),
                new Barrio("82", "Alto Bosque"),
                new Barrio("83", "Blas De Lezo"),
                new Barrio("84", "El Recreo"),
                new Barrio("85", "Santa Monica"),
                new Barrio("86", "Los Caracoles"),
                new Barrio("87", "Almirante Colon"),
                new Barrio("88", "Los Corales"),
                new Barrio("89", "Alameda La Victoria"),
                new Barrio("90", "El Socorro"),
                new Barrio("91", "Ceballos"),
                new Barrio("92", "La Central"),
                new Barrio("93", "San Fernando"),
                new Barrio("94", "El Milagro"),
                new Barrio("95", "El Campestre"),
                new Barrio("96", "Santa Clara"),
                new Barrio("97", "El Carmelo"),
                new Barrio("98", "Vista Hermosa"),
                new Barrio("99", "Ciudadela 11 De Noviembre"),
                new Barrio("100", "Urbanizacion Simon Bolivar"),
                new Barrio("101", "La Victoria"),
                new Barrio("102", "Los Jardines"),
                new Barrio("103", "La Consolata"),
                new Barrio("104", "Villa Rubia"),
                new Barrio("105", "Jorge Eliecer Gaitan"),
                new Barrio("106", "La Florida"),
                new Barrio("107", "Veinte De Julio Sur"),
                new Barrio("108", "Nelson Mandela"),
                new Barrio("109", "Cesar Florez"),
                new Barrio("110", "Luis Carlos Galan"),
                new Barrio("111", "El Reposo"),
                new Barrio("112", "El Educador"),
                new Barrio("113", "Rossedal"),
                new Barrio("114", "Maria Cano"),
                new Barrio("115", "Camilo Torres"),
                new Barrio("116", "Nueva Delhi"),
                new Barrio("117", "La Esmeralda I"),
                new Barrio("118", "Los Santanderes"),
                new Barrio("119", "Sectores Unidos"),
                new Barrio("120", "Nueva Jerusalen"),
                new Barrio("121", "La Sierrita"),
                new Barrio("122", "Nazareno"),
                new Barrio("123", "Manuela Vergara De Curi"),
                new Barrio("124", "Jaime Pardo Leal"),
                new Barrio("125", "La Esmeralda Ii"),
                new Barrio("126", "Villa Barraza"),
                new Barrio("127", "Villa Fanny"),
                new Barrio("128", "Arroz Barato"),
                new Barrio("129", "Puerta De Hierro"),
                new Barrio("130", "Policarpa"),
                new Barrio("131", "Marbella"),
                new Barrio("132", "Bocagrande"),
                new Barrio("133", "Centro"),
                new Barrio("134", "Fredonia"),
                new Barrio("135", "Crespo"),
                new Barrio("136", "San Pedro Martir"),
                new Barrio("137", "Nuevo Paraiso"),
                new Barrio("138", "El Laguito"),
                new Barrio("139", "Las Brisas"),
                new Barrio("140", "La Matuna"),
                new Barrio("141", "Getsemani"),
                new Barrio("142", "Chambacu"),
                new Barrio("143", "El Cabrero"),
                new Barrio("144", "La Maria"),
                new Barrio("145", "Trece De Junio"),
                new Barrio("146", "Chapacua"),
                new Barrio("147", "La Troncal"),
                new Barrio("148", "Tacarigua"),
                new Barrio("149", "Viejo Porvenir"),
                new Barrio("150", "Ternera"),
                new Barrio("151", "Olaya St. Rafael Nuñez"),
                new Barrio("152", "Olaya St.11 De Noviembre"),
                new Barrio("153", "Olaya St. Ricaurte"),
                new Barrio("154", "Olaya St. Central"),
                new Barrio("155", "Olaya St. La Magdalena"),
                new Barrio("156", "Olaya St. Progreso"),
                new Barrio("157", "Olaya St. Stella"),
                new Barrio("158", "Olaya St. Zarabanda"),
                new Barrio("159", "Villa Hermosa"),
                new Barrio("160", "Mamonal"),
                new Barrio("161", "Ciudadela 2000"),
                new Barrio("162", "Providencia"),
                new Barrio("163", "Manga"),
                new Barrio("164", "Castillogrande"),
                new Barrio("165", "Pie Del Cerro"),
                new Barrio("166", "El Bosque"),
                new Barrio("167", "Albornoz"),
                new Barrio("168", "Villa Rosa"),
                new Barrio("169", "Henequen"),
                new Barrio("170", "Antonio Jose De Sucre"),
                new Barrio("171", "El Libertador"),
                new Barrio("172", "Bellavista"),
                new Barrio("173", "La India"),
                new Barrio("174", "Flor Del Campo"),
                new Barrio("175", "Ciudad Bicentenario"),
                new Barrio("176", "Urbanizacion Colombiaton"),
                new Barrio("177", "Villas De La Candelaria"),
                new Barrio("178", "Zona Industrial"),
                new Barrio("179", "Olaya St. La Puntilla"),
                new Barrio("180", "Olaya St. Playa Blanca"),
                new Barrio("181", "Villas De Aranjuez"),
                new Barrio("182", "La Esperanza"),
                new Barrio("183", "Calamares"),
                new Barrio("184", "La Carolina"),
                new Barrio("185", "San Jose De Los Campanos"),
                new Barrio("186", "Los Girasoles"),
                new Barrio("187", "Ciudad Jardin")
            );
            barrioRepository.saveAll(barrios);
            System.out.println("Barrios de Cartagena cargados exitosamente.");
        }
    }
}