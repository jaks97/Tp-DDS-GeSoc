package controllers;

import Dominio.*;
import Dominio.Pago.Efectivo;
import Dominio.Pago.MedioDePago;
import Dominio.Pago.ValorMonetario;
import Dominio.Presupuesto.Presupuesto;
import Dominio.Ubicacion.Moneda;
import Dominio.Usuario.RepositorioUsuarios;
import Dominio.Usuario.Usuario;
import org.uqbarproject.jpa.java8.extras.EntityManagerOps;
import org.uqbarproject.jpa.java8.extras.WithGlobalEntityManager;
import org.uqbarproject.jpa.java8.extras.transaction.TransactionalOps;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import javax.print.Doc;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class OperacionesController implements WithGlobalEntityManager, EntityManagerOps, TransactionalOps {
    public List<ItemOperacion> parsearRequestItems(Request request){
        String[] detalles = request.queryMap("item").values();
        int[] montos = Arrays.stream(request.queryMap("monto").values())
                .mapToInt(str -> Integer.parseInt(str)).toArray();
        String[] monedas = request.queryMap("moneda").values();
        List<ItemOperacion> items = new ArrayList<>();
        for (int i = 0; i < detalles.length; i++) {
            ValorMonetario valor = new ValorMonetario(new Moneda(monedas[i], "$", "", 2), montos[i]); // TODO: revisar esto hardcodeado
            items.add(new ItemOperacion(detalles[i], valor));
        }

        return items;
    }

    public Operacion parsearRequestOperacion(Request request) {
        Integer numeroDocumento = Integer.parseInt(request.queryParams("numeroDocumento"));
        Provedor proveedor = RepositorioProveedor.instancia.getById(Long.parseLong(request.queryParams("proveedor")));
        MedioDePago medioDePago = new Efectivo(); // TODO
        List<ItemOperacion> items = parsearRequestItems(request);
        DocumentoComercial documento = DocumentoComercial.valueOf(request.queryParams("documentoComercial"));
        boolean requierePresupuesto = Boolean.parseBoolean(request.queryParams("requierePresupuesto"));
        boolean criterioSeleccion = Boolean.parseBoolean(request.queryParams("criterioSeleccion"));

        Operacion operacion = new Operacion(numeroDocumento, proveedor, LocalDate.now(), medioDePago, items, documento, requierePresupuesto, criterioSeleccion);

        List<Usuario> revisores = Arrays.stream(request.queryMap("revisores").values())
                .map(str -> RepositorioUsuarios.instancia.getById(Long.parseLong(str)))
                .collect(Collectors.toList());
        revisores.forEach(unRevisor -> operacion.altaRevisor(unRevisor));

        List<Etiqueta> etiquetas = Arrays.stream(request.queryMap("etiqueta").values())
                .map(str -> new Etiqueta(str))
                .collect(Collectors.toList());
        etiquetas.forEach(etiqueta -> operacion.addEtiqueta(etiqueta));

        return operacion;
    }

    public ModelAndView cargarOperacion(Request request, Response response){
        Operacion operacion = parsearRequestOperacion(request);

        withTransaction(() -> RepositorioOperacion.instancia.agregar(operacion));

        response.redirect("/operaciones");

        return null;
    }


    public ModelAndView getFormularioOperaciones(Request request, Response response) {
        Map <String,Object> modelo = new HashMap<>();
        Optional<Usuario> usuarioLogueado = UsuariosController.getUsuarioLogueado(request);
        if(!usuarioLogueado.isPresent()){
            response.redirect("/login");
            return null;
        }
        modelo.put("usuario", usuarioLogueado.get());


        List<Provedor> proveedores = RepositorioProveedor.instancia.listar();

        modelo.put("proveedores", proveedores);

        modelo.put("documentos", DocumentoComercial.values());

        modelo.put("revisores", RepositorioUsuarios.instancia.listar());
        return new ModelAndView(modelo,"operacion.html.hbs");
    }


    public ModelAndView vistaOperaciones(Request request, Response response) {
        Map <String,Object> modelo = new HashMap<>();
        Optional<Usuario> usuarioLogueado = UsuariosController.getUsuarioLogueado(request);
        if(!usuarioLogueado.isPresent()){
            response.redirect("/login");
            return null;
        }
        modelo.put("usuario", usuarioLogueado.get());

        List<Operacion> operaciones = RepositorioOperacion.instancia.listar();

        modelo.put("operaciones", operaciones);


        return new ModelAndView(modelo,"operaciones.html.hbs");
    }


    public ModelAndView vistaOperacion(Request request, Response response) {
        Map <String,Object> modelo = new HashMap<>();
        Optional<Usuario> usuarioLogueado = UsuariosController.getUsuarioLogueado(request);
        if(!usuarioLogueado.isPresent()){
            response.redirect("/login");
            return null;
        }
        modelo.put("usuario", usuarioLogueado.get());

        Long id = Long.parseLong(request.params("id"));

        Operacion operacion = RepositorioOperacion.instancia.getById(id);

        modelo.put("items", operacion.getDetalle());


        return new ModelAndView(modelo,"detalle-operacion.html.hbs");
    }


    public ModelAndView vistaPresupuestos(Request request, Response response) {
        Map <String,Object> modelo = new HashMap<>();
        Optional<Usuario> usuarioLogueado = UsuariosController.getUsuarioLogueado(request);
        if(!usuarioLogueado.isPresent()){
            response.redirect("/login");
            return null;
        }
        modelo.put("usuario", usuarioLogueado.get());

        Long id = Long.parseLong(request.params("id"));

        Operacion operacion = RepositorioOperacion.instancia.getById(id);

        modelo.put("operacion", operacion);
        modelo.put("presupuestos", operacion.getPresupuestos().stream().filter(presupuesto -> !(operacion.getPresupuestoSeleccionado().isPresent() && presupuesto.equals(operacion.getPresupuestoSeleccionado().get()))).toArray());
        if(operacion.getPresupuestoSeleccionado().isPresent())
            modelo.put("seleccionado", operacion.getPresupuestoSeleccionado().get());


        return new ModelAndView(modelo,"presupuestos.html.hbs");
    }


    public ModelAndView seleccionPresupuesto(Request request, Response response) {
        Optional<Usuario> usuarioLogueado = UsuariosController.getUsuarioLogueado(request);
        if(!usuarioLogueado.isPresent()){
            response.redirect("/login");
            return null;
        }

        Long idOperacion = Long.parseLong(request.params("idOperacion"));
        Operacion operacion = RepositorioOperacion.instancia.getById(idOperacion);
        Long idPresupuesto = Long.parseLong(request.params("idPresupuesto"));
        Optional<Presupuesto> presupuesto = operacion.getPresupuestos().stream().filter(unPresupuesto -> unPresupuesto.getId() == idPresupuesto).findFirst();

        if(!presupuesto.isPresent()){
            response.redirect("/operaciones");
            return null;
        }

        withTransaction(() -> {
            operacion.setPresupuestoSeleccionado(presupuesto.get());
        });
        response.redirect("/operaciones/"+idOperacion+"/presupuestos");
        return null;
    }

    public ModelAndView detallePresupuesto(Request request, Response response) {
        Map <String,Object> modelo = new HashMap<>();
        Optional<Usuario> usuarioLogueado = UsuariosController.getUsuarioLogueado(request);
        if(!usuarioLogueado.isPresent()){
            response.redirect("/login");
            return null;
        }
        modelo.put("usuario", usuarioLogueado.get());


        Long idOperacion = Long.parseLong(request.params("idOperacion"));
        Operacion operacion = RepositorioOperacion.instancia.getById(idOperacion);
        Long idPresupuesto = Long.parseLong(request.params("idPresupuesto"));
        Optional<Presupuesto> presupuesto = operacion.getPresupuestos().stream().filter(unPresupuesto -> unPresupuesto.getId() == idPresupuesto).findFirst();

        if(!presupuesto.isPresent()){
            response.redirect("/operaciones");
            return null;
        }

        modelo.put("items", presupuesto.get().getDetalle());


        return new ModelAndView(modelo,"detalle-operacion.html.hbs");
    }

    public ModelAndView getFormularioPresupuesto(Request request, Response response) {
        Map <String,Object> modelo = new HashMap<>();

        List<Provedor> proveedores = RepositorioProveedor.instancia.listar();
        modelo.put("proveedores",proveedores);

        modelo.put("documentos",DocumentoComercial.values());
        Optional<Usuario> usuarioLogueado = UsuariosController.getUsuarioLogueado(request);
        if(!usuarioLogueado.isPresent()){
            response.redirect("/login");
            return null;
        }


        return new ModelAndView(modelo,"presupuesto.html.hbs");
    }

    public ModelAndView cargarPresupuesto(Request request, Response response) {
        String[] detalles = request.queryMap("item").values();
        int[] montos = Arrays.stream(request.queryMap("monto").values()).mapToInt(str -> Integer.parseInt(str)).toArray();
        String[] monedas = request.queryMap("monto").values();
        DocumentoComercial documentoComercial;

        List<ItemOperacion> items = new ArrayList<>();
        for (int i = 0; i < detalles.length; i++) {
            ValorMonetario valor = new ValorMonetario(new Moneda(monedas[i], "$", "", 2), montos[i]); // TODO: revisar esto hardcodeadp
            items.add(new ItemOperacion(detalles[i], valor));
        }

        Provedor proveedor = RepositorioProveedor.instancia.getById(Long.parseLong(request.queryParams("proveedor")));
        DocumentoComercial documento = DocumentoComercial.valueOf(request.queryParams("documentoComercial"));
        Presupuesto presupuesto = new Presupuesto(proveedor,items,documento);

        Long idOperacion = Long.parseLong(request.params("id"));
        Operacion operacion = RepositorioOperacion.instancia.getById(idOperacion);
        withTransaction(() -> {
            operacion.addPresupusto(presupuesto);
            items.forEach(item -> persist(item));
            persist(presupuesto);
        });

        response.redirect("/operaciones/"+idOperacion+"/presupuesto");
        return null;
    }
}



