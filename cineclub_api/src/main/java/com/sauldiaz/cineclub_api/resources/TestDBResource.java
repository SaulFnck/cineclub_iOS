package com.sauldiaz.cineclub_api.resources;

import com.google.gson.JsonObject;
import com.sauldiaz.cineclub_api.ConexionBD;
import com.sauldiaz.cineclub_api.util.Respuesta;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Health-check: GET /resources/test-db */
@Path("test-db")
@Produces(MediaType.APPLICATION_JSON)
public class TestDBResource {

    @GET
    public Response probar() throws SQLException {
        JsonObject json = new JsonObject();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             Statement st = con.createStatement()) {
            json.addProperty("estado", "OK");
            json.addProperty("baseDatos", con.getCatalog());
            JsonObject conteos = new JsonObject();
            for (String t : new String[]{"empleados", "usuarios", "peliculas", "resenias", "rentas"}) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    rs.next();
                    conteos.addProperty(t, rs.getInt(1));
                }
            }
            json.add("registros", conteos);
        }
        return Respuesta.ok(json);
    }
}
