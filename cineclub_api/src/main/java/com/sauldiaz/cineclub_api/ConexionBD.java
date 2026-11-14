package com.sauldiaz.cineclub_api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto único de acceso a MySQL/TiDB (patrón Singleton).
 *
 * <p>La instancia es única y centraliza la configuración, pero cada llamada a
 * {@link #getConexion()} entrega una conexión nueva. Tomcat atiende varias
 * peticiones a la vez y las rentas usan transacciones; compartir una sola
 * Connection entre hilos mezclaría los commits/rollbacks de distintas peticiones.
 * Quien la pida debe cerrarla (try-with-resources).</p>
 *
 * <p>La configuración se lee de variables de entorno para no recompilar al
 * desplegar (Render, Railway, etc.):</p>
 * <ul>
 *   <li>{@code DB_URL}</li>
 *   <li>{@code DB_USER}</li>
 *   <li>{@code DB_PASSWORD}</li>
 * </ul>
 *
 * <p>Si alguna no está definida se usa el valor por defecto de desarrollo local.
 * En Render define siempre las tres.</p>
 *
 * @author sauld
 */
public final class ConexionBD {

    /** URL de TiDB Cloud. Se puede sobrescribir con la variable de entorno DB_URL. */
    private static final String URL_POR_DEFECTO =
            "jdbc:mysql://gateway01.us-west-2.prod.aws.tidbcloud.com:4000/cineclub"
            + "?sslMode=VERIFY_IDENTITY"
            + "&enabledTLSProtocols=TLSv1.2,TLSv1.3"
            + "&connectTimeout=10000"
            + "&socketTimeout=30000";

    /** Valores solo para desarrollo local; en Render se usan las variables de entorno. */
    private static final String USUARIO_POR_DEFECTO = "2EjGEaBT1g5hz1A.root";
    private static final String PASSWORD_POR_DEFECTO = "XYkXGz1UCPQBc4yi";

    private static final String URL = variableEntorno("DB_URL", URL_POR_DEFECTO);
    private static final String USUARIO = variableEntorno("DB_USER", USUARIO_POR_DEFECTO);
    private static final String PASSWORD = variableEntorno("DB_PASSWORD", PASSWORD_POR_DEFECTO);

    private static volatile ConexionBD instancia;

    /*
     * En Tomcat el driver vive dentro de WEB-INF/lib y no siempre se
     * autorregistra, así que lo cargamos explícitamente una sola vez.
     */
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se encontró el driver de MySQL (mysql-connector-j)", e);
        }
    }

    private ConexionBD() {
    }

    public static ConexionBD getInstancia() {
        if (instancia == null) {
            synchronized (ConexionBD.class) {
                if (instancia == null) {
                    instancia = new ConexionBD();
                }
            }
        }
        return instancia;
    }

    /** Abre una conexión nueva. Ciérrala con try-with-resources. */
    public Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    /** Lee una variable de entorno y usa el valor por defecto si no existe o está vacía. */
    private static String variableEntorno(String clave, String porDefecto) {
        String valor = System.getenv(clave);
        return (valor == null || valor.isBlank()) ? porDefecto : valor;
    }
}
