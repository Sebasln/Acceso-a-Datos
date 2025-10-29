package practica;

public class Planta {
	private int codigo,stock;
	private float precio;
	String nombre, foto, descripcion;
	public Planta(int codigo, int stock, float precio, String nombre, String foto, String descripcion) {
		super();
		this.codigo = codigo;
		this.stock = stock;
		this.precio = precio;
		this.nombre = nombre;
		this.foto = foto;
		this.descripcion = descripcion;
	}
	public int getCodigo() {
		return codigo;
	}
	public void setCodigo(int codigo) {
		this.codigo = codigo;
	}
	public int getStock() {
		return stock;
	}
	public void setStock(int stock) {
		this.stock = stock;
	}
	public float getPrecio() {
		return precio;
	}
	public void setPrecio(float precio) {
		this.precio = precio;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getFoto() {
		return foto;
	}
	public void setFoto(String foto) {
		this.foto = foto;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	@Override
	public String toString() {
		return "Planta [codigo=" + codigo + ", stock=" + stock + ", precio=" + precio + ", nombre=" + nombre + ", foto="
				+ foto + ", descripcion=" + descripcion + "]";
	}
	
	
	
	
}
