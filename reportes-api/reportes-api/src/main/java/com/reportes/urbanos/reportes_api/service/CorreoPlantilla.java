package com.reportes.urbanos.reportes_api.service;

// Diseño compartido de TODOS los correos del sistema (verificación, recuperar contraseña,
// reporte en proceso y reporte resuelto). Cada correo se arma aquí y los servicios solo lo envían.

// - Está hecho con tablas y estilos en línea, porque Gmail y Outlook ignoran buena parte del CSS moderno.
// - No depende de imágenes para entenderse: el nombre va en texto y los botones son de color sólido.
//   El logo es un extra; si el correo bloquea las imágenes, simplemente no se ve.
// - No usa .formatted(), así que ningún % puede romper el correo.
// - Todo texto que viene de un usuario (nombre, título del reporte) pasa por esc().
// - Para cambiar un color de todos los correos, se cambia en la tabla COLORES.
public final class CorreoPlantilla {

    private CorreoPlantilla() {
    }

    // Tabla de colores y tipografías. Los fragmentos usan {{NOMBRE}} y se reemplazan al final.
    private static final String[][] COLORES = {
        {"{{FONDO_PAGINA}}", "#E9EDF0"},
        {"{{TARJETA}}", "#0E1620"},
        {"{{TARJETA_INTERNA}}", "#142130"},
        {"{{BORDE}}", "#26333F"},
        {"{{ORO}}", "#D9A441"},
        {"{{VERDE}}", "#6FBF8E"},
        {"{{BLANCO}}", "#FFFFFF"},
        {"{{TEXTO}}", "#C9D4D7"},
        {"{{TEXTO_SUAVE}}", "#A9B8BC"},
        {"{{NEGRO}}", "#070B10"},
        {"{{PIE}}", "#5B6870"},
        {"{{FUENTE}}", "'Segoe UI',Helvetica,Arial,sans-serif"},
        {"{{FUENTE_TITULOS}}", "'Bricolage Grotesque','Segoe UI',Helvetica,Arial,sans-serif"}
    };

    // Colores que se pueden pasar a los fragmentos
    public static final String ORO = "{{ORO}}";
    public static final String VERDE = "{{VERDE}}";
    public static final String BORDE = "{{BORDE}}";
    public static final String BLANCO = "{{BLANCO}}";

    private static final String PIE_TEXTO = "Reportes Urbanos · Cartagena de Indias, Colombia";

    // Estructura general del correo
    private static final String PAGINA = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width,initial-scale=1.0">
          <title>{{TITULO}}</title>
          <link href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@800&display=swap" rel="stylesheet">
        </head>
        <body bgcolor="{{FONDO_PAGINA}}" style="margin:0;padding:0;background-color:{{FONDO_PAGINA}};">

          <!-- Texto de vista previa que se ve en la bandeja de entrada -->
          <div style="display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;font-size:1px;line-height:1px;color:{{FONDO_PAGINA}};">
            {{PREHEADER}}
          </div>

          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" bgcolor="{{FONDO_PAGINA}}"
                style="background-color:{{FONDO_PAGINA}};">
            <tr><td align="center" style="padding:32px 12px;">

              <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="width:100%;max-width:560px;">

                <tr><td bgcolor="{{TARJETA}}"
                        style="background-color:{{TARJETA}};border:1px solid {{BORDE}};border-radius:16px;">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0">

                    <tr><td height="4" bgcolor="{{ORO}}"
                            style="height:4px;font-size:0;line-height:0;border-radius:16px 16px 0 0;">&nbsp;</td></tr>

                    <!-- MARCA: logo (si el correo carga imágenes) y nombre en texto -->
                    <tr><td style="padding:28px 36px 0;">
                      <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                        <td width="36" style="width:36px;">
                          <img src="{{LOGO}}" width="36" height="36" alt=""
                              style="display:block;border:0;outline:none;text-decoration:none;">
                        </td>
                        <td style="padding-left:12px;font-family:{{FUENTE_TITULOS}};font-size:19px;font-weight:800;color:{{BLANCO}};">
                          Reportes Urbanos
                        </td>
                      </tr></table>
                    </td></tr>

                    <tr><td style="padding:28px 36px 36px;">
                      <h1 style="margin:0 0 16px;font-family:{{FUENTE_TITULOS}};font-size:30px;line-height:1.15;font-weight:800;color:{{BLANCO}};letter-spacing:-0.5px;">
                        {{TITULO}}
                      </h1>
                      {{CUERPO}}
                    </td></tr>

                  </table>
                </td></tr>

                <tr><td align="center"
                        style="padding:22px 8px 0;font-family:{{FUENTE}};font-size:12px;line-height:1.6;color:{{PIE}};">
                  Reportes Urbanos &middot; Cartagena de Indias, Colombia
                </td></tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """;

    // ---------- Herramientas ----------

    // Convierte en texto seguro cualquier dato que escriba un usuario
    public static String esc(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static String colores(String texto) {
        String resultado = texto;
        for (String[] color : COLORES) {
            resultado = resultado.replace(color[0], color[1]);
        }
        return resultado;
    }

    private static String logo(String baseUrl) {
        return (baseUrl == null ? "" : baseUrl) + "/images/logo.png";
    }

    // Arma el correo completo con la estructura general
    public static String armar(String urlLogo, String preheader, String titulo, String cuerpo) {
        String pagina = PAGINA
            .replace("{{TITULO}}", esc(titulo))
            .replace("{{PREHEADER}}", esc(preheader))
            .replace("{{LOGO}}", esc(urlLogo));
        return colores(pagina).replace("{{CUERPO}}", colores(cuerpo));
    }

    // ---------- Fragmentos ----------

    public static String negrita(String texto) {
        return "<strong style=\"color:{{BLANCO}};\">" + esc(texto) + "</strong>";
    }

    public static String parrafoHtml(String html, int margenInferior) {
        return "<p style=\"margin:0 0 " + margenInferior + "px;font-family:{{FUENTE}};font-size:16px;line-height:1.65;color:{{TEXTO}};\">"
            + html + "</p>";
    }

    public static String parrafoHtml(String html) {
        return parrafoHtml(html, 24);
    }

    public static String boton(String url, String texto) {
        return botonColor(url, texto, ORO, 28);
    }

    public static String botonColor(String url, String texto, String fondo, int margenInferior) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 " + margenInferior + "px;\"><tr>"
            + "<td bgcolor=\"" + fondo + "\" align=\"center\" style=\"border-radius:50px;background-color:" + fondo + ";\">"
            + "<a href=\"" + esc(url) + "\" style=\"display:inline-block;padding:15px 36px;font-family:{{FUENTE}};"
            + "font-size:16px;font-weight:bold;color:{{NEGRO}};text-decoration:none;border-radius:50px;\">"
            + esc(texto) + "</a></td></tr></table>";
    }

    public static String enlaceRespaldo(String url) {
        return "<p style=\"margin:0 0 6px;font-family:{{FUENTE}};font-size:13px;line-height:1.6;color:{{TEXTO_SUAVE}};\">"
            + "Si el botón no funciona, copia y pega este enlace en tu navegador:</p>"
            + "<p style=\"margin:0 0 28px;font-family:{{FUENTE}};font-size:13px;line-height:1.6;\">"
            + "<a href=\"" + esc(url) + "\" style=\"color:{{ORO}};word-break:break-all;\">" + esc(url) + "</a></p>";
    }

    public static String tarjeta(String etiqueta, String valor, String colorBorde) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 16px;\"><tr>"
            + "<td bgcolor=\"{{TARJETA_INTERNA}}\" style=\"background-color:{{TARJETA_INTERNA}};border-left:4px solid " + colorBorde
            + ";border-radius:8px;padding:14px 18px;\">"
            + "<p style=\"margin:0 0 4px;font-family:{{FUENTE}};font-size:13px;color:{{TEXTO_SUAVE}};\">" + esc(etiqueta) + "</p>"
            + "<p style=\"margin:0;font-family:{{FUENTE}};font-size:18px;line-height:1.35;font-weight:bold;color:{{BLANCO}};\">"
            + esc(valor) + "</p>"
            + "</td></tr></table>";
    }

    // Aviso al final del correo, separado por una línea. Recibe HTML ya seguro.
    public static String nota(String html) {
        return "<div style=\"border-top:1px solid {{BORDE}};padding-top:20px;margin-top:4px;font-family:{{FUENTE}};"
            + "font-size:13px;line-height:1.6;color:{{TEXTO_SUAVE}};\">" + html + "</div>";
    }

    // ---------- Los cuatro correos del sistema ----------

    public static String verificacionHtml(String baseUrl, String enlace) {
        String cuerpo = parrafoHtml("Gracias por unirte a " + negrita("Reportes Urbanos")
                + ". Con tu cuenta puedes reportar huecos en la vía, fugas de agua, alumbrado dañado y más "
                + "en tu barrio de Cartagena, y seguir cómo avanza cada reporte.")
            + boton(enlace, "Verificar correo electrónico")
            + enlaceRespaldo(enlace)
            + nota(negrita("Seguridad.") + " Este enlace expira en " + negrita("3 minutos")
                + " y solo puede usarse una vez. Si no creaste esta cuenta, ignora este correo.");
        return armar(logo(baseUrl), "Confirma tu correo y activa tu cuenta. El enlace dura 3 minutos.",
            "Confirma tu correo y activa tu cuenta", cuerpo);
    }

    public static String verificacionTexto(String enlace) {
        return "Reportes Urbanos\n\n"
            + "Confirma tu correo para activar tu cuenta.\n\n"
            + "Gracias por unirte. Abre este enlace para verificar tu correo electrónico:\n"
            + enlace + "\n\n"
            + "El enlace expira en 3 minutos y solo puede usarse una vez.\n"
            + "Si no creaste esta cuenta, ignora este correo.\n\n"
            + PIE_TEXTO;
    }

    public static String recuperacionHtml(String baseUrl, String nombre, String enlace) {
        String cuerpo = parrafoHtml("Hola, " + negrita(nombre) + ".", 8)
            + parrafoHtml("Recibimos una solicitud para restablecer la contraseña de tu cuenta en "
                + negrita("Reportes Urbanos") + ". Haz clic en el botón para crear una nueva contraseña.")
            + boton(enlace, "Restablecer contraseña")
            + enlaceRespaldo(enlace)
            + nota(negrita("Seguridad.") + " Este enlace expira en " + negrita("30 minutos")
                + ". Si no solicitaste este cambio, ignora este correo: tu contraseña no será modificada.");
        return armar(logo(baseUrl), "Crea una nueva contraseña. El enlace dura 30 minutos.",
            "Recupera tu contraseña", cuerpo);
    }

    public static String recuperacionTexto(String nombre, String enlace) {
        return "Reportes Urbanos\n\n"
            + "Hola, " + nombre + ".\n\n"
            + "Recibimos una solicitud para restablecer la contraseña de tu cuenta.\n"
            + "Abre este enlace para crear una nueva contraseña:\n"
            + enlace + "\n\n"
            + "El enlace expira en 30 minutos. Si no solicitaste este cambio, ignora este correo:\n"
            + "tu contraseña no será modificada.\n\n"
            + PIE_TEXTO;
    }

    public static String enProcesoHtml(String baseUrl, String nombre, String tituloReporte, String nombreAdmin) {
        String cuerpo = parrafoHtml("Hola, " + negrita(nombre) + ".", 8)
            + parrafoHtml("Tu reporte ha sido recibido y un administrador ya está trabajando en él.", 20)
            + tarjeta("Reporte", tituloReporte, BLANCO)
            + tarjeta("Admin asignado", nombreAdmin, BORDE)
            + "<div style=\"height:8px;line-height:8px;font-size:0;\">&nbsp;</div>"
            + parrafoHtml("Te notificaremos cuando el problema sea resuelto. Puedes ver el estado de tu reporte "
                + "en cualquier momento desde la plataforma.")
            + boton(baseUrl + "/login", "Ver mis reportes");
        return armar(logo(baseUrl), "Un administrador ya está trabajando en tu reporte.",
            "Tu reporte está en proceso", cuerpo);
    }

    public static String enProcesoTexto(String baseUrl, String nombre, String tituloReporte, String nombreAdmin) {
        return "Reportes Urbanos\n\n"
            + "Hola, " + nombre + ".\n\n"
            + "Tu reporte ha sido recibido y un administrador ya está trabajando en él.\n\n"
            + "Reporte: " + tituloReporte + "\n"
            + "Admin asignado: " + nombreAdmin + "\n\n"
            + "Te notificaremos cuando el problema sea resuelto. Puedes ver el estado de tu reporte en:\n"
            + baseUrl + "/login\n\n"
            + PIE_TEXTO;
    }

    public static String resueltoHtml(String baseUrl, String nombre, String tituloReporte, String urlSi, String urlNo) {
        String cuerpo = parrafoHtml("Hola, " + negrita(nombre) + ".", 8)
            + parrafoHtml("El equipo de gestión urbana ha marcado tu reporte como resuelto. "
                + "Queremos saber si el problema fue solucionado correctamente.", 20)
            + tarjeta("Reporte resuelto", tituloReporte, VERDE)
            + "<div style=\"height:8px;line-height:8px;font-size:0;\">&nbsp;</div>"
            + parrafoHtml(negrita("¿El problema fue resuelto satisfactoriamente?"), 16)
            + botonColor(urlSi, "Sí, fue resuelto", VERDE, 12)
            + botonColor(urlNo, "No, persiste", ORO, 28)
            + nota("Tu opinión nos ayuda a mejorar la gestión urbana de Cartagena.");
        return armar(logo(baseUrl), "Cuéntanos si el problema quedó solucionado.",
            "Tu reporte fue marcado como resuelto", cuerpo);
    }

    public static String resueltoTexto(String nombre, String tituloReporte, String urlSi, String urlNo) {
        return "Reportes Urbanos\n\n"
            + "Hola, " + nombre + ".\n\n"
            + "El equipo de gestión urbana ha marcado tu reporte como resuelto:\n"
            + tituloReporte + "\n\n"
            + "¿El problema fue resuelto satisfactoriamente?\n\n"
            + "Sí, fue resuelto: " + urlSi + "\n"
            + "No, persiste: " + urlNo + "\n\n"
            + "Tu opinión nos ayuda a mejorar la gestión urbana de Cartagena.\n\n"
            + PIE_TEXTO;
    }
}
