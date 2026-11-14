package com.sauldiaz.cineclub_api.resources;

import com.sauldiaz.cineclub_api.dao.PeliculaDAO;
import com.sauldiaz.cineclub_api.dao.ReseniaDAO;
import com.sauldiaz.cineclub_api.excepciones.ApiException;
import com.sauldiaz.cineclub_api.modelo.Pelicula;
import com.sauldiaz.cineclub_api.modelo.dto.AjusteStock;
import com.sauldiaz.cineclub_api.util.GsonUtil;
import com.sauldiaz.cineclub_api.util.Respuesta;
import com.sauldiaz.cineclub_api.util.Validacion;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Catálogo de películas: consulta (todos) y administración (empleado). */
@Path("peliculas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PeliculaResource {

    private final PeliculaDAO dao = new PeliculaDAO();
    private final ReseniaDAO reseniaDAO = new ReseniaDAO();

    /** GET /peliculas?titulo=&genero=&director=&disponibles=true&orden=titulo|estreno|rating|precio */
    @GET
    public Response listar(@QueryParam("titulo") String titulo,
                           @QueryParam("genero") String genero,
                           @QueryParam("director") String director,
                           @QueryParam("disponibles") boolean disponibles,
                           @QueryParam("orden") String orden) throws SQLException {
        return Respuesta.ok(dao.listar(titulo, genero, director, disponibles, orden));
    }

    /** GET /peliculas/generos — para un Picker en SwiftUI. */
    @GET
    @Path("generos")
    public Response generos() throws SQLException {
        return Respuesta.ok(dao.generos());
    }

    @GET
    @Path("{id}")
    public Response obtener(@PathParam("id") int id) throws SQLException {
        return Respuesta.ok(buscarOFallar(id));
    }

    @GET
    @Path("{id}/resenias")
    public Response resenias(@PathParam("id") int id) throws SQLException {
        buscarOFallar(id);
        return Respuesta.ok(reseniaDAO.listar(id, null, null));
    }

    @POST
    public Response crear(String cuerpo) throws SQLException {
        Pelicula p = GsonUtil.leer(cuerpo, Pelicula.class);
        validar(p);
        int id = dao.insertar(p);
        return Respuesta.creado(dao.buscarPorId(id));
    }

    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") int id, String cuerpo) throws SQLException {
        buscarOFallar(id);
        Pelicula p = GsonUtil.leer(cuerpo, Pelicula.class);
        validar(p);
        p.setIdPelicula(id);
        dao.actualizar(p);
        return Respuesta.ok(dao.buscarPorId(id));
    }

    /** PATCH /peliculas/{id}/stock  { "cantidad": 5 } o { "cantidad": -2 } */
    @PATCH
    @Path("{id}/stock")
    public Response ajustarStock(@PathParam("id") int id, String cuerpo) throws SQLException {
        Pelicula actual = buscarOFallar(id);
        AjusteStock ajuste = GsonUtil.leer(cuerpo, AjusteStock.class);
        int cantidad = Validacion.requerido(ajuste.getCantidad(), "cantidad");
        if (cantidad == 0) {
            throw ApiException.badRequest("La cantidad no puede ser 0");
        }
        if (!dao.ajustarStock(id, cantidad)) {
            throw ApiException.conflict("El stock no puede quedar negativo (stock actual: "
                    + actual.getStockDisponible() + ")");
        }
        return Respuesta.ok(dao.buscarPorId(id));
    }

    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") int id) throws SQLException {
        buscarOFallar(id);
        int rentas = dao.contarRentas(id);
        int resenias = dao.contarResenias(id);
        if (rentas > 0 || resenias > 0) {
            Map<String, Integer> deps = new LinkedHashMap<>();
            deps.put("rentas", rentas);
            deps.put("resenias", resenias);
            throw ApiException.conflict("No se puede eliminar la película porque tiene registros asociados",
                    deps, "Ajusta su stock a 0 para retirarla del catálogo");
        }
        dao.eliminar(id);
        return Respuesta.sinContenido();
    }

    // ---------- Privados ----------

    private Pelicula buscarOFallar(int id) throws SQLException {
        Pelicula p = dao.buscarPorId(id);
        if (p == null) {
            throw ApiException.notFound("La película " + id + " no existe");
        }
        return p;
    }

    private void validar(Pelicula p) {
        p.setTitulo(Validacion.texto(p.getTitulo(), "titulo", 150));
        p.setGenero(Validacion.texto(p.getGenero(), "genero", 50));
        p.setDirector(Validacion.textoOpcional(p.getDirector(), "director", 100));
        p.setImagenArchivo(Validacion.textoOpcional(p.getImagenArchivo(), "imagenArchivo", 255));

        BigDecimal costo = Validacion.requerido(p.getCostoRenta(), "costoRenta");
        if (costo.signum() <= 0) {
            throw ApiException.badRequest("El costoRenta debe ser mayor a 0");
        }
        if (p.getStockDisponible() == null) {
            p.setStockDisponible(0);
        } else if (p.getStockDisponible() < 0) {
            throw ApiException.badRequest("El stockDisponible no puede ser negativo");
        }
    }
}
