import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import Dominio.DineroEnCuenta;
import Dominio.ItemOperacion;
import Dominio.MedioDePago;
import Dominio.Operacion;
import Dominio.Provedor;
import Dominio.TipoDocumento;

public class TestOperacion {

	 @Test
     public void testOperacionNoTieneDocumentoComercial() {
		 Provedor prov = new Provedor("Homero", "Thompson", "Pato feliz", 29256328, "Calle falsa 123", TipoDocumento.DNI);
		 
		 MedioDePago medio = new DineroEnCuenta(352265652);
		 List<ItemOperacion> detalle = new ArrayList<>();
		 Operacion op = new Operacion(29256328, prov , LocalDate.now(),  medio, detalle, null );
		  
		 assertFalse(op.getDocumentoComercial() != null);
     }
}