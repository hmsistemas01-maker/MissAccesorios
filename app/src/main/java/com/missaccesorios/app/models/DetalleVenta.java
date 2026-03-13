package com.missaccesorios.app.models;

public class DetalleVenta {
    private long id;
    private long idVenta;
    private long idProveedor;
    private String nombreProveedor;
    private String nombreProducto;
    private double precio;
    private int cantidad;

    public DetalleVenta() {}
    public DetalleVenta(long idProveedor, String nombreProveedor, String nombreProducto, double precio, int cantidad) {
        this.idProveedor = idProveedor;
        this.nombreProveedor = nombreProveedor;
        this.nombreProducto = nombreProducto;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getIdVenta() { return idVenta; }
    public void setIdVenta(long idVenta) { this.idVenta = idVenta; }
    public long getIdProveedor() { return idProveedor; }
    public void setIdProveedor(long idProveedor) { this.idProveedor = idProveedor; }
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getTotal() { return precio * cantidad; }
}
