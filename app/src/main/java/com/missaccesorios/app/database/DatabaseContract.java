package com.missaccesorios.app.database;

public class DatabaseContract {

    public static class Proveedores {
        public static final String TABLE    = "proveedores";
        public static final String ID       = "_id";
        public static final String NOMBRE   = "nombre";
        public static final String TELEFONO = "telefono";
    }

    public static class Ventas {
        public static final String TABLE             = "ventas";
        public static final String ID                = "_id";
        public static final String FECHA             = "fecha";
        public static final String TOTAL             = "total";
        public static final String METODO_PAGO       = "metodo_pago";
        public static final String COMISION          = "comision";
        public static final String TOTAL_RECIBIR     = "total_recibir";
        public static final String PAGO_CON          = "pago_con";
        public static final String CAMBIO            = "cambio";
        public static final String REFERENCIA        = "referencia";
        public static final String TURNO             = "turno";
        public static final String CORTADA           = "cortada";
        public static final String ID_CORTE          = "id_corte";
    }

    public static class DetalleVenta {
        public static final String TABLE          = "detalle_venta";
        public static final String ID             = "_id";
        public static final String ID_VENTA       = "id_venta";
        public static final String ID_PROVEEDOR   = "id_proveedor";
        public static final String NOMBRE_PROVEEDOR = "nombre_proveedor";
        public static final String NOMBRE_PRODUCTO= "nombre_producto";
        public static final String PRECIO         = "precio";
        public static final String CANTIDAD       = "cantidad";
        public static final String TOTAL          = "total";
    }

    public static class CortesCaja {
        public static final String TABLE                = "cortes_caja";
        public static final String ID                   = "_id";
        public static final String FECHA_INICIO         = "fecha_inicio";
        public static final String FECHA_FIN            = "fecha_fin";
        public static final String TURNO                = "turno";
        public static final String TOTAL_VENTAS         = "total_ventas";
        public static final String TOTAL_EFECTIVO       = "total_efectivo";
        public static final String TOTAL_TARJETA        = "total_tarjeta";
        public static final String TOTAL_TRANSFERENCIA  = "total_transferencia";
        public static final String COMISION_TARJETA     = "comision_tarjeta";
        public static final String TOTAL_REAL           = "total_real";
    }

    public static class PagosProveedores {
        public static final String TABLE            = "pagos_proveedores";
        public static final String ID               = "_id";
        public static final String ID_PROVEEDOR     = "id_proveedor";
        public static final String NOMBRE_PROVEEDOR = "nombre_proveedor";
        public static final String FECHA_INICIO     = "fecha_inicio";
        public static final String FECHA_FIN        = "fecha_fin";
        public static final String ID_CORTE         = "id_corte";      // ← NUEVO: vincula al corte exacto
        public static final String TOTAL_VENTAS     = "total_ventas";
        public static final String TOTAL_PAGADO     = "total_pagado";
        public static final String FECHA_PAGO       = "fecha_pago";
        public static final String FIRMA_PATH       = "firma_path";    // ← NUEVO: ruta imagen de firma
    }
}