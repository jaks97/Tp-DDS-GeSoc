package Dominio.Presupuesto;

import Dominio.Operacion;

import java.util.OptionalInt;

public class ValidacionPresupuestoMenorValor implements Validacion {
    private Operacion egreso;
    private String nombreValidacion;
    
    public ValidacionPresupuestoMenorValor(Operacion egreso) {
        this.egreso = egreso;
        this.nombreValidacion = "Validacion de seleccion de presupuesto de menor valor: ";
    }

    @Override
    public boolean validar() {
    	boolean resultado = true;
    	if (egreso.getCriterioDeSeleccionMinimoValor()) {
	        // Faltaria chequear por si el criterio de seleccion es el del menor valor. Pero eso donde lo ponemos? (tanto el criterio en si, como la validacion)
	        OptionalInt costoMinimo = egreso.getPresupuestos().stream().mapToInt(Presupuesto::getTotal).min();
	        resultado = costoMinimo.isPresent() && costoMinimo.getAsInt() == egreso.getPresupuestoSeleccionado().get().getTotal();
    	}
    	return resultado;
    }

    public Operacion getEgreso() {
        return egreso;
    }
    
    public String getNombre() {
    	return nombreValidacion;
    }
}