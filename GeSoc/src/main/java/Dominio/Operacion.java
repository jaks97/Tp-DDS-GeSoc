package Dominio;

import Dominio.Mensajes.Mensaje;
import Dominio.Pago.MedioDePago;
import Dominio.Pago.ValorMonetario;
import Dominio.Presupuesto.Presupuesto;
import Dominio.Ubicacion.Moneda;
import Dominio.Usuario.Usuario;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.*;

@Entity
public class Operacion{
	@Id
	@GeneratedValue
	private long operacion_id;

	private Integer numeroDocumento;

	@ManyToOne
	private Provedor provedor;
	private LocalDate fecha;

	
	@OneToOne
	@JoinColumn(name= "medio_id")
	private MedioDePago medioPago;

	@OneToMany
	@JoinColumn(name = "operacion_id")
	private List <ItemOperacion> detalle;
	@OneToMany
	@JoinColumn(name = "operacion_id")
	private List<Presupuesto> presupuestos = new ArrayList<>();

	@OneToOne
	@JoinColumn(name="presupuesto_id", referencedColumnName="presupuesto_id", nullable=true)
	private Presupuesto presupuestoSeleccionado;
    @Enumerated
	@Column(nullable=true)
    private DocumentoComercial documentoComercial;

	private Boolean requierePresupuestos;
	private Boolean criterioDeSeleccionMinimoValor;

	@ManyToMany
	private List<Usuario> revisores = new ArrayList<>();
	@ManyToMany
	private List<Etiqueta> etiquetas = new ArrayList<>(); 

	
	
	public Operacion(Integer numeroDocumento, Provedor provedor, LocalDate fecha, MedioDePago medioPago,
			List<ItemOperacion> detalle, DocumentoComercial documentoComercial, Boolean requierePresupuestos, Boolean criterio) {
		this.numeroDocumento = numeroDocumento;
		this.provedor = provedor;
		this.fecha = fecha;
		this.medioPago = medioPago;
		this.detalle = detalle;
		this.documentoComercial = documentoComercial;
		this.requierePresupuestos = requierePresupuestos;
		this.criterioDeSeleccionMinimoValor = criterio;
	}

	public Operacion() {}
	
	public Integer getNumeroDocumento() {
		return numeroDocumento;
	}
	public void setNumeroDocumento(Integer numeroDocumento) {
		this.numeroDocumento = numeroDocumento;
	}
	public Provedor getProvedor() {
		return provedor;
	}
	public void setProvedor(Provedor provedor) {
		this.provedor = provedor;
	}
	public LocalDate getFecha() {
		return fecha;
	}
	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}
	public MedioDePago getMedioPago() {
		return medioPago;
	}
	public void setMedioPago(MedioDePago medioPago) {
		this.medioPago = medioPago;
	}
	public List<ItemOperacion> getDetalle() {
		return detalle;
	}
	public void setDetalle(List<ItemOperacion> detalle) {
		this.detalle = detalle;
	}

	public Optional<DocumentoComercial> getDocumentoComercial() {
		return Optional.ofNullable(this.documentoComercial);
	}

	public DocumentoComercial getDocumentoComercialNullable() {
		return this.documentoComercial;
	}
	
	public void setDocumentoComercial(DocumentoComercial documentoComercial) {
		this.documentoComercial = documentoComercial;
	}

	public List<Presupuesto> getPresupuestos(){
		return presupuestos;
	}

	public void addPresupusto(Presupuesto presupuesto) {
		presupuestos.add(presupuesto);
	}

	public Boolean getRequierePresupuestos(){
		return requierePresupuestos;
	}

	public Optional<Presupuesto> getPresupuestoSeleccionado() {
		return Optional.ofNullable(presupuestoSeleccionado);
	}
	public void setPresupuestoSeleccionado(Presupuesto presupuesto) {
		presupuestoSeleccionado = presupuesto;
	}

	public Boolean getCriterioDeSeleccionMinimoValor() {
		return criterioDeSeleccionMinimoValor;
	}
	
	public void altaRevisor(Usuario revisor) {
		revisores.add(revisor);
	}
	
	public void bajarRevisor(Usuario revisor) {
		revisores.remove(revisor);
	}
	
	public List<Usuario> getRevisores() {
		return revisores;
	}
	
	public void notificar(String m) {
		Mensaje mensaje = new Mensaje(m, this);
		getRevisores().forEach(revisor->revisor.updateBandeja(mensaje));
	}

	public void addEtiqueta( Etiqueta et) {
		if (!etiquetas.contains(et))
			etiquetas.add(et);
	}
	
	public void quitarEtiqueta( Etiqueta et) {
		if (etiquetas.contains(et))
			etiquetas.remove(et);
	}
	
	public List<Etiqueta> getEtiquetas(){
			return etiquetas;
	}
	
	public ValorMonetario getTotal() {
		ValorMonetario total =new ValorMonetario(new Moneda("ARS", "$", "Peso", 2), 0); // Esto de crear una moneda aca adentro no esta tan bueno
		for(ItemOperacion i:detalle) {
			total = total.sumar(i.getValorTotal());
		}
		
		return total;
	}

	public long getId(){
		return operacion_id;
	}
}
