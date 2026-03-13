package com.missaccesorios.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.missaccesorios.app.models.DetalleVenta;
import com.missaccesorios.app.models.Proveedor;
import com.missaccesorios.app.models.Venta;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "missaccesorios.db";
    private static final int    DB_VERSION = 6; // v6: ventas.id_corte

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DatabaseContract.Proveedores.TABLE + " (" +
                DatabaseContract.Proveedores.ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.Proveedores.NOMBRE   + " TEXT NOT NULL, " +
                DatabaseContract.Proveedores.TELEFONO + " TEXT)");

        db.execSQL("CREATE TABLE " + DatabaseContract.Ventas.TABLE + " (" +
                DatabaseContract.Ventas.ID            + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.Ventas.FECHA         + " INTEGER, " +
                DatabaseContract.Ventas.TOTAL         + " REAL, " +
                DatabaseContract.Ventas.METODO_PAGO   + " TEXT, " +
                DatabaseContract.Ventas.PAGO_CON      + " REAL, " +
                DatabaseContract.Ventas.CAMBIO        + " REAL, " +
                DatabaseContract.Ventas.COMISION      + " REAL, " +
                DatabaseContract.Ventas.TOTAL_RECIBIR + " REAL, " +
                DatabaseContract.Ventas.REFERENCIA    + " TEXT, " +
                DatabaseContract.Ventas.TURNO         + " TEXT, " +
                DatabaseContract.Ventas.CORTADA       + " INTEGER DEFAULT 0, " +
                DatabaseContract.Ventas.ID_CORTE       + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + DatabaseContract.DetalleVenta.TABLE + " (" +
                DatabaseContract.DetalleVenta.ID               + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.DetalleVenta.ID_VENTA         + " INTEGER, " +
                DatabaseContract.DetalleVenta.ID_PROVEEDOR     + " INTEGER, " +
                DatabaseContract.DetalleVenta.NOMBRE_PROVEEDOR + " TEXT, " +
                DatabaseContract.DetalleVenta.NOMBRE_PRODUCTO  + " TEXT, " +
                DatabaseContract.DetalleVenta.PRECIO           + " REAL, " +
                DatabaseContract.DetalleVenta.CANTIDAD         + " INTEGER, " +
                DatabaseContract.DetalleVenta.TOTAL            + " REAL)");

        db.execSQL("CREATE TABLE " + DatabaseContract.CortesCaja.TABLE + " (" +
                DatabaseContract.CortesCaja.ID                  + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.CortesCaja.FECHA_INICIO        + " INTEGER, " +
                DatabaseContract.CortesCaja.FECHA_FIN           + " INTEGER, " +
                DatabaseContract.CortesCaja.TURNO               + " TEXT, " +
                DatabaseContract.CortesCaja.TOTAL_VENTAS        + " REAL, " +
                DatabaseContract.CortesCaja.TOTAL_EFECTIVO      + " REAL, " +
                DatabaseContract.CortesCaja.TOTAL_TARJETA       + " REAL, " +
                DatabaseContract.CortesCaja.TOTAL_TRANSFERENCIA + " REAL, " +
                DatabaseContract.CortesCaja.COMISION_TARJETA    + " REAL, " +
                DatabaseContract.CortesCaja.TOTAL_REAL          + " REAL)");

        db.execSQL("CREATE TABLE " + DatabaseContract.PagosProveedores.TABLE + " (" +
                DatabaseContract.PagosProveedores.ID               + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.PagosProveedores.ID_PROVEEDOR     + " INTEGER, " +
                DatabaseContract.PagosProveedores.NOMBRE_PROVEEDOR + " TEXT, " +
                DatabaseContract.PagosProveedores.FECHA_INICIO     + " INTEGER, " +
                DatabaseContract.PagosProveedores.FECHA_FIN        + " INTEGER, " +
                DatabaseContract.PagosProveedores.ID_CORTE         + " INTEGER, " +
                DatabaseContract.PagosProveedores.TOTAL_VENTAS     + " REAL, " +
                DatabaseContract.PagosProveedores.TOTAL_PAGADO     + " REAL, " +
                DatabaseContract.PagosProveedores.FECHA_PAGO       + " INTEGER, " +
                DatabaseContract.PagosProveedores.FIRMA_PATH       + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE " + DatabaseContract.PagosProveedores.TABLE +
                    " ADD COLUMN " + DatabaseContract.PagosProveedores.ID_CORTE + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + DatabaseContract.PagosProveedores.TABLE +
                    " ADD COLUMN " + DatabaseContract.PagosProveedores.FIRMA_PATH + " TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 6) {
            // Agregar id_corte a ventas — vincula cada venta a su corte exacto
            try { db.execSQL("ALTER TABLE " + DatabaseContract.Ventas.TABLE +
                    " ADD COLUMN " + DatabaseContract.Ventas.ID_CORTE + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            // Rellenar id_corte en ventas existentes usando los cortes de caja
            // Para cada venta cortada, buscar el corte cuyo rango la contiene
            try {
                db.execSQL(
                        "UPDATE " + DatabaseContract.Ventas.TABLE + " SET " +
                                DatabaseContract.Ventas.ID_CORTE + " = (" +
                                "  SELECT c." + DatabaseContract.CortesCaja.ID +
                                "  FROM " + DatabaseContract.CortesCaja.TABLE + " c" +
                                "  WHERE " + DatabaseContract.Ventas.TABLE + "." + DatabaseContract.Ventas.FECHA +
                                "    BETWEEN c." + DatabaseContract.CortesCaja.FECHA_INICIO +
                                "    AND c." + DatabaseContract.CortesCaja.FECHA_FIN +
                                "  LIMIT 1" +
                                ") WHERE " + DatabaseContract.Ventas.CORTADA + "=1" +
                                "  AND " + DatabaseContract.Ventas.ID_CORTE + "=0"
                );
            } catch (Exception ignored) {}
        }
    }

    // ── PROVEEDORES ──────────────────────────────────────────────────────────

    public long insertarProveedor(String nombre, String telefono) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.Proveedores.NOMBRE, nombre);
        cv.put(DatabaseContract.Proveedores.TELEFONO, telefono);
        return db.insert(DatabaseContract.Proveedores.TABLE, null, cv);
    }

    public boolean actualizarProveedor(long id, String nombre, String telefono) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.Proveedores.NOMBRE, nombre);
        cv.put(DatabaseContract.Proveedores.TELEFONO, telefono);
        return db.update(DatabaseContract.Proveedores.TABLE, cv,
                DatabaseContract.Proveedores.ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public List<Proveedor> obtenerProveedores() {
        List<Proveedor> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(DatabaseContract.Proveedores.TABLE, null, null, null, null, null,
                DatabaseContract.Proveedores.NOMBRE + " ASC");
        while (c.moveToNext()) {
            lista.add(new Proveedor(
                    c.getLong(c.getColumnIndexOrThrow(DatabaseContract.Proveedores.ID)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseContract.Proveedores.NOMBRE)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseContract.Proveedores.TELEFONO))
            ));
        }
        c.close();
        return lista;
    }

    // ── VENTAS ───────────────────────────────────────────────────────────────

    public long insertarVenta(Venta v) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.Ventas.FECHA,         v.getFecha());
            cv.put(DatabaseContract.Ventas.TOTAL,         v.getTotal());
            cv.put(DatabaseContract.Ventas.METODO_PAGO,   v.getMetodoPago());
            cv.put(DatabaseContract.Ventas.PAGO_CON,      v.getPagaCon());
            cv.put(DatabaseContract.Ventas.CAMBIO,        v.getCambio());
            cv.put(DatabaseContract.Ventas.COMISION,      v.getComision());
            cv.put(DatabaseContract.Ventas.TOTAL_RECIBIR, v.getTotalRecibir());
            cv.put(DatabaseContract.Ventas.REFERENCIA,    v.getReferencia());
            cv.put(DatabaseContract.Ventas.TURNO,         v.getTurno());
            cv.put(DatabaseContract.Ventas.CORTADA,       0);
            long idVenta = db.insert(DatabaseContract.Ventas.TABLE, null, cv);

            for (DetalleVenta d : v.getDetalles()) {
                ContentValues cvd = new ContentValues();
                cvd.put(DatabaseContract.DetalleVenta.ID_VENTA,          idVenta);
                cvd.put(DatabaseContract.DetalleVenta.ID_PROVEEDOR,      d.getIdProveedor());
                cvd.put(DatabaseContract.DetalleVenta.NOMBRE_PROVEEDOR,  d.getNombreProveedor());
                cvd.put(DatabaseContract.DetalleVenta.NOMBRE_PRODUCTO,   d.getNombreProducto());
                cvd.put(DatabaseContract.DetalleVenta.PRECIO,            d.getPrecio());
                cvd.put(DatabaseContract.DetalleVenta.CANTIDAD,          d.getCantidad());
                cvd.put(DatabaseContract.DetalleVenta.TOTAL,             d.getPrecio() * d.getCantidad());
                db.insert(DatabaseContract.DetalleVenta.TABLE, null, cvd);
            }
            db.setTransactionSuccessful();
            return idVenta;
        } finally {
            db.endTransaction();
        }
    }

    public List<Venta> obtenerVentasPorRango(long inicio, long fin) {
        List<Venta> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(DatabaseContract.Ventas.TABLE, null,
                DatabaseContract.Ventas.FECHA + " BETWEEN ? AND ?",
                new String[]{String.valueOf(inicio), String.valueOf(fin)},
                null, null, DatabaseContract.Ventas.FECHA + " DESC");
        while (c.moveToNext()) lista.add(ventaFromCursor(c));
        c.close();
        for (Venta v : lista) v.setDetalles(obtenerDetallesDeVenta(v.getId()));
        return lista;
    }

    public List<Venta> obtenerVentasNoCortadasPorRango(long inicio, long fin) {
        List<Venta> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(DatabaseContract.Ventas.TABLE, null,
                DatabaseContract.Ventas.FECHA + " BETWEEN ? AND ? AND " +
                        DatabaseContract.Ventas.CORTADA + "=0",
                new String[]{String.valueOf(inicio), String.valueOf(fin)},
                null, null, DatabaseContract.Ventas.FECHA + " DESC");
        while (c.moveToNext()) lista.add(ventaFromCursor(c));
        c.close();
        for (Venta v : lista) v.setDetalles(obtenerDetallesDeVenta(v.getId()));
        return lista;
    }

    private Venta ventaFromCursor(Cursor c) {
        Venta v = new Venta();
        v.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseContract.Ventas.ID)));
        v.setFecha(c.getLong(c.getColumnIndexOrThrow(DatabaseContract.Ventas.FECHA)));
        v.setTotal(c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.Ventas.TOTAL)));
        v.setMetodoPago(c.getString(c.getColumnIndexOrThrow(DatabaseContract.Ventas.METODO_PAGO)));
        v.setPagaCon(c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.Ventas.PAGO_CON)));
        v.setCambio(c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.Ventas.CAMBIO)));
        v.setComision(c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.Ventas.COMISION)));
        v.setTotalRecibir(c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.Ventas.TOTAL_RECIBIR)));
        v.setReferencia(c.getString(c.getColumnIndexOrThrow(DatabaseContract.Ventas.REFERENCIA)));
        v.setTurno(c.getString(c.getColumnIndexOrThrow(DatabaseContract.Ventas.TURNO)));
        v.setCortada(c.getInt(c.getColumnIndexOrThrow(DatabaseContract.Ventas.CORTADA)) == 1);
        v.setIdCorte(c.getLong(c.getColumnIndexOrThrow(DatabaseContract.Ventas.ID_CORTE)));
        return v;
    }

    public List<DetalleVenta> obtenerDetallesDeVenta(long idVenta) {
        List<DetalleVenta> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(DatabaseContract.DetalleVenta.TABLE, null,
                DatabaseContract.DetalleVenta.ID_VENTA + "=?",
                new String[]{String.valueOf(idVenta)}, null, null, null);
        while (c.moveToNext()) {
            DetalleVenta d = new DetalleVenta();
            d.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseContract.DetalleVenta.ID)));
            d.setIdVenta(c.getLong(c.getColumnIndexOrThrow(DatabaseContract.DetalleVenta.ID_VENTA)));
            d.setIdProveedor(c.getLong(c.getColumnIndexOrThrow(DatabaseContract.DetalleVenta.ID_PROVEEDOR)));
            d.setNombreProveedor(c.getString(c.getColumnIndexOrThrow(DatabaseContract.DetalleVenta.NOMBRE_PROVEEDOR)));
            d.setNombreProducto(c.getString(c.getColumnIndexOrThrow(DatabaseContract.DetalleVenta.NOMBRE_PRODUCTO)));
            d.setPrecio(c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.DetalleVenta.PRECIO)));
            d.setCantidad(c.getInt(c.getColumnIndexOrThrow(DatabaseContract.DetalleVenta.CANTIDAD)));
            lista.add(d);
        }
        c.close();
        return lista;
    }

    // ── CORTES ───────────────────────────────────────────────────────────────

    public long realizarCorte(long inicio, long fin, double totalVentas, double totalEfectivo,
                              double totalTarjeta, double totalTransf, double comisionTarj,
                              double totalReal, List<Long> ventaIds) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.CortesCaja.FECHA_INICIO,        inicio);
            cv.put(DatabaseContract.CortesCaja.FECHA_FIN,           fin);
            cv.put(DatabaseContract.CortesCaja.TOTAL_VENTAS,        totalVentas);
            cv.put(DatabaseContract.CortesCaja.TOTAL_EFECTIVO,      totalEfectivo);
            cv.put(DatabaseContract.CortesCaja.TOTAL_TARJETA,       totalTarjeta);
            cv.put(DatabaseContract.CortesCaja.TOTAL_TRANSFERENCIA, totalTransf);
            cv.put(DatabaseContract.CortesCaja.COMISION_TARJETA,    comisionTarj);
            cv.put(DatabaseContract.CortesCaja.TOTAL_REAL,          totalReal);
            long idCorte = db.insert(DatabaseContract.CortesCaja.TABLE, null, cv);

            for (Long idVenta : ventaIds) {
                ContentValues upd = new ContentValues();
                upd.put(DatabaseContract.Ventas.CORTADA, 1);
                upd.put(DatabaseContract.Ventas.ID_CORTE, idCorte);
                db.update(DatabaseContract.Ventas.TABLE, upd,
                        DatabaseContract.Ventas.ID + "=?", new String[]{String.valueOf(idVenta)});
            }
            db.setTransactionSuccessful();
            Log.d("CORTES", "Corte id=" + idCorte + " ventas=" + ventaIds.size());
            return idCorte;
        } finally {
            db.endTransaction();
        }
    }

    public boolean existeCorteEnFecha(long inicio, long fin) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseContract.CortesCaja.TABLE +
                        " WHERE " + DatabaseContract.CortesCaja.FECHA_INICIO + " <= ? AND " +
                        DatabaseContract.CortesCaja.FECHA_FIN + " >= ?",
                new String[]{String.valueOf(fin), String.valueOf(inicio)});
        boolean existe = false;
        if (c.moveToFirst()) existe = c.getInt(0) > 0;
        c.close();
        return existe;
    }

    /** Devuelve el ID del corte que cubre exactamente ese rango, o -1 si no existe */
    public long obtenerIdCorte(long inicio, long fin) {
        SQLiteDatabase db = getReadableDatabase();
        // ORDER BY FECHA_FIN DESC para obtener el corte más reciente del rango
        Cursor c = db.rawQuery(
                "SELECT " + DatabaseContract.CortesCaja.ID + " FROM " + DatabaseContract.CortesCaja.TABLE +
                        " WHERE " + DatabaseContract.CortesCaja.FECHA_FIN + " <= ? AND " +
                        DatabaseContract.CortesCaja.FECHA_FIN + " >= ?" +
                        " ORDER BY " + DatabaseContract.CortesCaja.FECHA_FIN + " DESC LIMIT 1",
                new String[]{String.valueOf(fin), String.valueOf(inicio)});
        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    /**
     * Dado un id de corte, devuelve su rango exacto {inicio, fin}.
     * Retorna null si no existe.
     */
    public long[] obtenerRangoCorte(long idCorte) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DatabaseContract.CortesCaja.FECHA_INICIO + ", " +
                        DatabaseContract.CortesCaja.FECHA_FIN + " FROM " + DatabaseContract.CortesCaja.TABLE +
                        " WHERE " + DatabaseContract.CortesCaja.ID + "=? LIMIT 1",
                new String[]{String.valueOf(idCorte)});
        long[] rango = null;
        if (c.moveToFirst()) {
            rango = new long[]{c.getLong(0), c.getLong(1)};
        }
        c.close();
        return rango;
    }

    public Cursor obtenerCortes() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(DatabaseContract.CortesCaja.TABLE, null, null, null, null, null,
                DatabaseContract.CortesCaja.ID + " DESC");
    }

    /** Devuelve el ID del corte más reciente, o -1 si no hay ninguno */
    public long obtenerIdUltimoCorte() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DatabaseContract.CortesCaja.ID + " FROM " +
                        DatabaseContract.CortesCaja.TABLE + " ORDER BY " +
                        DatabaseContract.CortesCaja.ID + " DESC LIMIT 1", null);
        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public List<Venta> obtenerVentasDelCorte(long inicioCorte, long finCorte) {
        return obtenerVentasPorRango(inicioCorte, finCorte);
    }

    /**
     * Retorna lista de {fecha_inicio, fecha_fin} de todos los cortes
     * que ya fueron pagados para un proveedor dado.
     * Usado para excluir ventas ya pagadas del reporte.
     */
    /** Retorna el conjunto de id_corte ya pagados para este proveedor */
    public java.util.Set<Long> obtenerIdCortesPagados(long idProveedor) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DatabaseContract.PagosProveedores.ID_CORTE +
                        " FROM " + DatabaseContract.PagosProveedores.TABLE +
                        " WHERE " + DatabaseContract.PagosProveedores.ID_PROVEEDOR + "=?",
                new String[]{String.valueOf(idProveedor)});
        while (c.moveToNext()) ids.add(c.getLong(0));
        c.close();
        return ids;
    }

    /** @deprecated usar obtenerIdCortesPagados */
    public List<long[]> obtenerCortesPagadosConRango(long idProveedor) {
        // Mantenido por compatibilidad — internamente usa id_corte
        java.util.Set<Long> ids = obtenerIdCortesPagados(idProveedor);
        List<long[]> rangos = new ArrayList<>();
        for (long id : ids) {
            long[] r = obtenerRangoCorte(id);
            if (r != null) rangos.add(r);
        }
        return rangos;
    }

    /** @deprecated Usar obtenerCortesPagadosConRango */
    public List<Long> obtenerCortesPagadosPorProveedor(long idProveedor) {
        List<Long> ids = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DatabaseContract.PagosProveedores.ID_CORTE +
                        " FROM " + DatabaseContract.PagosProveedores.TABLE +
                        " WHERE " + DatabaseContract.PagosProveedores.ID_PROVEEDOR + "=?",
                new String[]{String.valueOf(idProveedor)});
        while (c.moveToNext()) ids.add(c.getLong(0));
        c.close();
        return ids;
    }

    // ── PAGOS PROVEEDORES ────────────────────────────────────────────────────

    /**
     * Verifica si ya existe un pago para este proveedor vinculado al corte exacto.
     * Soluciona el bug donde un segundo corte mostraba "ya pagado" aunque era diferente.
     */
    public boolean existePagoProveedorEnCorte(long idProveedor, long idCorte) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseContract.PagosProveedores.TABLE +
                        " WHERE " + DatabaseContract.PagosProveedores.ID_PROVEEDOR + "=? AND " +
                        DatabaseContract.PagosProveedores.ID_CORTE + "=?",
                new String[]{String.valueOf(idProveedor), String.valueOf(idCorte)});
        boolean existe = false;
        if (c.moveToFirst()) existe = c.getInt(0) > 0;
        c.close();
        return existe;
    }

    /** @deprecated Usar existePagoProveedorEnCorte para evitar falsos positivos */
    public boolean existePagoProveedorEnPeriodo(long idProveedor, long inicio, long fin) {
        long idCorte = obtenerIdCorte(inicio, fin);
        if (idCorte < 0) return false;
        return existePagoProveedorEnCorte(idProveedor, idCorte);
    }

    /**
     * Para todos los proveedores: mapa idProveedor → Set de id_corte ya pagados.
     * Usado por ReportesActivity para mostrar badge PAGADO/PENDIENTE por id_corte.
     */
    public Map<Long, java.util.Set<Long>> obtenerTodosIdCortesPagados() {
        Map<Long, java.util.Set<Long>> mapa = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DatabaseContract.PagosProveedores.ID_PROVEEDOR +
                        ", " + DatabaseContract.PagosProveedores.ID_CORTE +
                        " FROM " + DatabaseContract.PagosProveedores.TABLE, null);
        while (c.moveToNext()) {
            long idP    = c.getLong(0);
            long idCorte = c.getLong(1);
            if (!mapa.containsKey(idP)) mapa.put(idP, new java.util.HashSet<>());
            mapa.get(idP).add(idCorte);
        }
        c.close();
        return mapa;
    }

    /** @deprecated usar obtenerTodosIdCortesPagados */
    public Map<Long, List<long[]>> obtenerTodosCortesPagadosConRango() {
        Map<Long, List<long[]>> mapa = new HashMap<>();
        Map<Long, java.util.Set<Long>> porProv = obtenerTodosIdCortesPagados();
        for (Map.Entry<Long, java.util.Set<Long>> e : porProv.entrySet()) {
            List<long[]> rangos = new ArrayList<>();
            for (long id : e.getValue()) {
                long[] r = obtenerRangoCorte(id);
                if (r != null) rangos.add(r);
            }
            mapa.put(e.getKey(), rangos);
        }
        return mapa;
    }

    /**
     * Fecha del primer registro de venta de un proveedor (en ms), o -1 si no hay ventas.
     */
    public long obtenerFechaPrimerVentaProveedor(long idProveedor) {
        SQLiteDatabase db = getReadableDatabase();
        // buscar en detalle_venta el mínimo de fecha de la venta vinculada
        Cursor c = db.rawQuery(
                "SELECT MIN(v.fecha) FROM " + DatabaseContract.Ventas.TABLE + " v" +
                        " JOIN " + DatabaseContract.DetalleVenta.TABLE + " dv ON dv." + DatabaseContract.DetalleVenta.ID_VENTA + " = v." + DatabaseContract.Ventas.ID +
                        " WHERE dv." + DatabaseContract.DetalleVenta.ID_PROVEEDOR + " = ?",
                new String[]{String.valueOf(idProveedor)});
        long fecha = -1;
        if (c.moveToFirst() && !c.isNull(0)) fecha = c.getLong(0);
        c.close();
        return fecha;
    }

    /**
     * Fecha del último pago registrado para el proveedor (fecha_fin del corte pagado), o -1.
     */
    public long obtenerFechaUltimoPagoProveedor(long idProveedor) {
        SQLiteDatabase db = getReadableDatabase();
        // Usar fecha_fin del propio pago (rango exacto cubierto), NO del corte de caja
        Cursor c = db.rawQuery(
                "SELECT MAX(" + DatabaseContract.PagosProveedores.FECHA_FIN + ")" +
                        " FROM " + DatabaseContract.PagosProveedores.TABLE +
                        " WHERE " + DatabaseContract.PagosProveedores.ID_PROVEEDOR + "=?",
                new String[]{String.valueOf(idProveedor)});
        long fecha = -1;
        if (c.moveToFirst() && !c.isNull(0)) fecha = c.getLong(0);
        c.close();
        return fecha;
    }


    /**
     * Devuelve todas las ventas cortadas de un proveedor que aún no han sido pagadas.
     * Usa id_corte directamente — sin rangos de fecha.
     */
    public List<Venta> obtenerVentasPendientesDePago(long idProveedor) {
        // Obtener id_cortes que YA fueron pagados a este proveedor
        java.util.Set<Long> cortesPagados = obtenerIdCortesPagados(idProveedor);

        // Obtener todos los id_corte que tienen ventas cortadas de este proveedor
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT DISTINCT v." + DatabaseContract.Ventas.ID_CORTE +
                        " FROM " + DatabaseContract.Ventas.TABLE + " v" +
                        " JOIN " + DatabaseContract.DetalleVenta.TABLE + " dv" +
                        " ON dv." + DatabaseContract.DetalleVenta.ID_VENTA + " = v." + DatabaseContract.Ventas.ID +
                        " WHERE v." + DatabaseContract.Ventas.CORTADA + "=1" +
                        " AND v." + DatabaseContract.Ventas.ID_CORTE + ">0" +
                        " AND dv." + DatabaseContract.DetalleVenta.ID_PROVEEDOR + "=?",
                new String[]{String.valueOf(idProveedor)});

        java.util.Set<Long> cortesPendientes = new java.util.HashSet<>();
        while (c.moveToNext()) {
            long idC = c.getLong(0);
            if (!cortesPagados.contains(idC)) cortesPendientes.add(idC);
        }
        c.close();

        if (cortesPendientes.isEmpty()) return new ArrayList<>();

        // Traer todas las ventas de esos cortes
        StringBuilder placeholders = new StringBuilder();
        List<String> args = new ArrayList<>();
        for (long idC : cortesPendientes) {
            if (placeholders.length() > 0) placeholders.append(",");
            placeholders.append("?");
            args.add(String.valueOf(idC));
        }

        List<Venta> ventas = new ArrayList<>();
        Cursor cv = db.query(DatabaseContract.Ventas.TABLE, null,
                DatabaseContract.Ventas.ID_CORTE + " IN (" + placeholders + ")",
                args.toArray(new String[0]),
                null, null, DatabaseContract.Ventas.FECHA + " ASC");
        while (cv.moveToNext()) ventas.add(ventaFromCursor(cv));
        cv.close();
        for (Venta v : ventas) v.setDetalles(obtenerDetallesDeVenta(v.getId()));
        return ventas;
    }

    /**
     * Devuelve el id_corte más reciente que tiene ventas pendientes de pago
     * para el proveedor dado. Usado para asociar el pago al corte.
     */
    public long obtenerUltimoCorteConVentasPendientes(long idProveedor) {
        java.util.Set<Long> cortesPagados = obtenerIdCortesPagados(idProveedor);
        SQLiteDatabase db = getReadableDatabase();
        // Traer todos los id_corte con ventas de este proveedor, ordenados desc
        Cursor c = db.rawQuery(
                "SELECT DISTINCT v." + DatabaseContract.Ventas.ID_CORTE +
                        " FROM " + DatabaseContract.Ventas.TABLE + " v" +
                        " JOIN " + DatabaseContract.DetalleVenta.TABLE + " dv" +
                        " ON dv." + DatabaseContract.DetalleVenta.ID_VENTA + " = v." + DatabaseContract.Ventas.ID +
                        " WHERE v." + DatabaseContract.Ventas.CORTADA + "=1" +
                        " AND v." + DatabaseContract.Ventas.ID_CORTE + ">0" +
                        " AND dv." + DatabaseContract.DetalleVenta.ID_PROVEEDOR + "=?" +
                        " ORDER BY v." + DatabaseContract.Ventas.ID_CORTE + " DESC",
                new String[]{String.valueOf(idProveedor)});
        long idC = -1;
        // Devolver el más reciente que NO esté pagado
        while (c.moveToNext()) {
            long val = c.getLong(0);
            if (!cortesPagados.contains(val)) { idC = val; break; }
        }
        c.close();
        return idC;
    }

    /**
     * Registra el pago marcando CADA corte pendiente como pagado.
     * Si el proveedor tenía 3 cortes pendientes, se insertan 3 filas en pagos_proveedores.
     * Así obtenerIdCortesPagados() los encuentra todos correctamente.
     */
    public long registrarPagoProveedor(long idProveedor, String nombreProveedor,
                                       long inicio, long fin, long idCorte,
                                       double totalVentas, double totalPagado,
                                       String firmaPath) {
        SQLiteDatabase db = getWritableDatabase();
        long ahora = System.currentTimeMillis();
        String firma = firmaPath != null ? firmaPath : "";

        // Obtener todos los cortes pendientes de este proveedor
        java.util.Set<Long> cortesPagados = obtenerIdCortesPagados(idProveedor);
        Cursor c = db.rawQuery(
                "SELECT DISTINCT v." + DatabaseContract.Ventas.ID_CORTE +
                        " FROM " + DatabaseContract.Ventas.TABLE + " v" +
                        " JOIN " + DatabaseContract.DetalleVenta.TABLE + " dv" +
                        " ON dv." + DatabaseContract.DetalleVenta.ID_VENTA + " = v." + DatabaseContract.Ventas.ID +
                        " WHERE v." + DatabaseContract.Ventas.CORTADA + "=1" +
                        " AND v." + DatabaseContract.Ventas.ID_CORTE + ">0" +
                        " AND dv." + DatabaseContract.DetalleVenta.ID_PROVEEDOR + "=?",
                new String[]{String.valueOf(idProveedor)});

        java.util.List<Long> cortesPendientes = new ArrayList<>();
        while (c.moveToNext()) {
            long idC = c.getLong(0);
            if (!cortesPagados.contains(idC)) cortesPendientes.add(idC);
        }
        c.close();

        if (cortesPendientes.isEmpty()) {
            // Fallback: registrar con el idCorte pasado
            cortesPendientes.add(idCorte);
        }

        db.beginTransaction();
        long lastId = -1;
        try {
            for (long idC : cortesPendientes) {
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.PagosProveedores.ID_PROVEEDOR,     idProveedor);
                cv.put(DatabaseContract.PagosProveedores.NOMBRE_PROVEEDOR, nombreProveedor);
                cv.put(DatabaseContract.PagosProveedores.FECHA_INICIO,     inicio);
                cv.put(DatabaseContract.PagosProveedores.FECHA_FIN,        fin);
                cv.put(DatabaseContract.PagosProveedores.ID_CORTE,         idC);
                cv.put(DatabaseContract.PagosProveedores.TOTAL_VENTAS,     totalVentas);
                cv.put(DatabaseContract.PagosProveedores.TOTAL_PAGADO,     totalPagado);
                cv.put(DatabaseContract.PagosProveedores.FECHA_PAGO,       ahora);
                cv.put(DatabaseContract.PagosProveedores.FIRMA_PATH,       firma);
                lastId = db.insert(DatabaseContract.PagosProveedores.TABLE, null, cv);
                Log.d("PAGO", "Pago registrado id=" + lastId + " prov=" + nombreProveedor + " corte=" + idC);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return lastId;
    }

    /** Actualiza la firma de un pago ya registrado */
    public void actualizarFirmaPago(long idPago, String firmaPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.PagosProveedores.FIRMA_PATH, firmaPath);
        db.update(DatabaseContract.PagosProveedores.TABLE, cv,
                DatabaseContract.PagosProveedores.ID + "=?", new String[]{String.valueOf(idPago)});
    }

    public Cursor obtenerPagosPorRango(long inicio, long fin) {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(DatabaseContract.PagosProveedores.TABLE, null,
                DatabaseContract.PagosProveedores.FECHA_PAGO + " BETWEEN ? AND ?",
                new String[]{String.valueOf(inicio), String.valueOf(fin)},
                null, null, DatabaseContract.PagosProveedores.FECHA_PAGO + " DESC");
    }

    public Cursor obtenerTodosPagos() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(DatabaseContract.PagosProveedores.TABLE, null, null, null, null, null,
                DatabaseContract.PagosProveedores.FECHA_PAGO + " DESC");
    }

    public Cursor obtenerResumenPagosPorProveedor(long inicio, long fin) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT " + DatabaseContract.PagosProveedores.NOMBRE_PROVEEDOR +
                        ", SUM(" + DatabaseContract.PagosProveedores.TOTAL_PAGADO + ") as total_pagado" +
                        ", COUNT(*) as num_pagos FROM " + DatabaseContract.PagosProveedores.TABLE +
                        " WHERE " + DatabaseContract.PagosProveedores.FECHA_PAGO + " BETWEEN ? AND ?" +
                        " GROUP BY " + DatabaseContract.PagosProveedores.ID_PROVEEDOR,
                new String[]{String.valueOf(inicio), String.valueOf(fin)});
    }
}