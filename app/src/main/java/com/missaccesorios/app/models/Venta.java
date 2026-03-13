package com.missaccesorios.app.models;

import java.util.ArrayList;
import java.util.List;

public class Venta {
    private long id;
    private long fecha;
    private double total;
    private String metodoPago;
    private double pagaCon;
    private double cambio;
    private double comision;
    private double totalRecibir;
    private String referencia;
    private String turno;
    private boolean cortada;
    private long idCorte;
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public double getPagaCon() { return pagaCon; }
    public void setPagaCon(double pagaCon) { this.pagaCon = pagaCon; }
    public double getCambio() { return cambio; }
    public void setCambio(double cambio) { this.cambio = cambio; }
    public double getComision() { return comision; }
    public void setComision(double comision) { this.comision = comision; }
    public double getTotalRecibir() { return totalRecibir; }
    public void setTotalRecibir(double totalRecibir) { this.totalRecibir = totalRecibir; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
    public boolean isCortada() { return cortada; }
    public void setCortada(boolean cortada) { this.cortada = cortada; }
    public long getIdCorte() { return idCorte; }
    public void setIdCorte(long idCorte) { this.idCorte = idCorte; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
}