package com.missaccesorios.app.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.DetalleVenta;
import com.missaccesorios.app.models.Proveedor;
import com.missaccesorios.app.models.Venta;
import java.util.*;

public class ReportesActivity extends AppCompatActivity {

    private static final long ID_TODOS = -1L;

    // Filtros de estado
    private static final int FILTRO_TODAS     = 0;
    private static final int FILTRO_CORTADAS  = 1;
    private static final int FILTRO_SIN_CORTE = 2;

    private DatabaseHelper db;
    private Spinner  spinnerProv;
    private RadioGroup rgFiltro;
    private TextView tvResumenVentas, tvTotalNeto, tvCardTitulo;
    private LinearLayout llProductos;
    private CardView cardResumen, cardProductos;

    private List<Proveedor> listaSpinner = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);

        spinnerProv     = findViewById(R.id.spinnerProveedor);
        tvResumenVentas = findViewById(R.id.tvResumenVentas);
        tvTotalNeto     = findViewById(R.id.tvTotalNetoReporte);
        tvCardTitulo    = findViewById(R.id.tvCardTitulo);
        llProductos     = findViewById(R.id.llProductosReporte);
        cardResumen     = findViewById(R.id.cardResumen);
        cardProductos   = findViewById(R.id.cardProductos);
        rgFiltro        = findViewById(R.id.rgFiltroCorte);

        cargarProveedores();

        findViewById(R.id.btnBuscarReporte).setOnClickListener(v -> buscar());
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private void cargarProveedores() {
        listaSpinner.clear();
        listaSpinner.add(new Proveedor(ID_TODOS, "Todos los proveedores", ""));
        listaSpinner.addAll(db.obtenerProveedores());
        ArrayAdapter<Proveedor> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaSpinner);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProv.setAdapter(a);
    }

    private int getFiltroSeleccionado() {
        if (rgFiltro == null) return FILTRO_TODAS;
        int checked = rgFiltro.getCheckedRadioButtonId();
        if (checked == R.id.rbSoloCortadas)  return FILTRO_CORTADAS;
        if (checked == R.id.rbSinCortar)     return FILTRO_SIN_CORTE;
        return FILTRO_TODAS;
    }

    // ── Buscar ───────────────────────────────────────────────────────────────
    private void buscar() {
        Proveedor sel     = (Proveedor) spinnerProv.getSelectedItem();
        boolean modoTodos = sel == null || sel.getId() == ID_TODOS;
        int filtro        = getFiltroSeleccionado();

        // Cargar todas las ventas y aplicar filtro de corte
        List<Venta> todas = db.obtenerVentasPorRango(0, Long.MAX_VALUE);
        List<Venta> ventas = new ArrayList<>();
        for (Venta v : todas) {
            if (filtro == FILTRO_CORTADAS  && !v.isCortada()) continue;
            if (filtro == FILTRO_SIN_CORTE &&  v.isCortada()) continue;
            ventas.add(v);
        }

        // Obtener id_corte ya pagados por proveedor
        Map<Long, java.util.Set<Long>> pagosPorProv = db.obtenerTodosIdCortesPagados();
        // Fallback para ventas antiguas con id_corte=0: usar rangos de fecha
        Map<Long, List<long[]>> rangosPorProv = db.obtenerTodosCortesPagadosConRango();

        if (modoTodos) mostrarTodos(ventas, pagosPorProv, rangosPorProv);
        else           mostrarProveedor(sel, ventas, pagosPorProv, rangosPorProv);
    }

    // ── Modo UN proveedor ────────────────────────────────────────────────────
    private void mostrarProveedor(Proveedor prov, List<Venta> ventas,
                                  Map<Long, java.util.Set<Long>> pagosPorProv,
                                  Map<Long, List<long[]>> rangosPorProv) {
        java.util.Set<Long> cortesPagados = pagosPorProv.getOrDefault(prov.getId(), new java.util.HashSet<>());
        List<long[]> rangos = rangosPorProv.getOrDefault(prov.getId(), new ArrayList<>());

        Map<String, double[]> productos = new LinkedHashMap<>(); // nombre → {cant, bruto, com, pagadas, total}
        double totalNeto = 0;
        int numVentas = 0;

        for (Venta v : ventas) {
            boolean tieneDetalle = false;
            for (DetalleVenta d : v.getDetalles())
                if (d.getIdProveedor() == prov.getId()) { tieneDetalle = true; break; }
            if (!tieneDetalle) continue;

            boolean pagada = esPagada(v, cortesPagados, rangos);
            boolean contada = false;

            for (DetalleVenta d : v.getDetalles()) {
                if (d.getIdProveedor() != prov.getId()) continue;
                if (!contada) { numVentas++; contada = true; }
                double com  = calcularComision(d, v);
                double neto = d.getTotal() - com;
                // {cant, neto, pagadasCount}
                productos.computeIfAbsent(d.getNombreProducto(), k -> new double[]{0, 0, 0});
                productos.get(d.getNombreProducto())[0] += d.getCantidad();
                productos.get(d.getNombreProducto())[1] += neto;
                if (pagada) productos.get(d.getNombreProducto())[2]++;
                totalNeto += neto;
            }
        }

        // Mostrar resumen
        cardResumen.setVisibility(View.VISIBLE);
        cardProductos.setVisibility(View.VISIBLE);
        tvResumenVentas.setText(String.valueOf(numVentas));
        tvTotalNeto.setText("$" + String.format("%.2f", totalNeto));
        tvCardTitulo.setText("PRODUCTOS — " + prov.getNombre());
        llProductos.removeAllViews();

        if (productos.isEmpty()) {
            agregarVacio("Sin ventas para este proveedor");
            return;
        }

        boolean primero = true;
        for (Map.Entry<String, double[]> e : productos.entrySet()) {
            if (!primero) agregarSep();
            primero = false;
            double[] v = e.getValue();
            agregarFilaProducto(e.getKey(), (int) v[0], v[1], v[2] > 0);
        }
    }

    // ── Modo TODOS ───────────────────────────────────────────────────────────
    private void mostrarTodos(List<Venta> ventas, Map<Long, java.util.Set<Long>> pagosPorProv, Map<Long, List<long[]>> rangosPorProv) {
        // Por proveedor: {totalNeto, numVentas, numPagadas}
        Map<Long, double[]>              totales  = new LinkedHashMap<>();
        Map<Long, String>                nombres  = new LinkedHashMap<>();
        Map<Long, Map<String, double[]>> prods    = new LinkedHashMap<>();

        double totalNetoGlobal = 0;
        int totalVentasGlobal  = 0;

        for (Venta v : ventas) {
            for (DetalleVenta d : v.getDetalles()) {
                long idP = d.getIdProveedor();
                java.util.Set<Long> rp = pagosPorProv.getOrDefault(idP, new java.util.HashSet<>());
                List<long[]> rpRangos = rangosPorProv.getOrDefault(idP, new ArrayList<>());
                boolean pagada  = esPagada(v, rp, rpRangos);
                double com      = calcularComision(d, v);
                double neto     = d.getTotal() - com;

                if (!totales.containsKey(idP)) {
                    totales.put(idP, new double[]{0, 0, 0}); // neto, ventas, pagadas
                    nombres.put(idP, d.getNombreProveedor());
                    prods.put(idP, new LinkedHashMap<>());
                }
                totales.get(idP)[0] += neto;
                totales.get(idP)[1]++;
                if (pagada) totales.get(idP)[2]++;
                totalNetoGlobal += neto;
                totalVentasGlobal++;

                Map<String, double[]> mp = prods.get(idP);
                mp.computeIfAbsent(d.getNombreProducto(), k -> new double[]{0, 0, 0});
                mp.get(d.getNombreProducto())[0] += d.getCantidad();
                mp.get(d.getNombreProducto())[1] += neto;
                if (pagada) mp.get(d.getNombreProducto())[2]++;
            }
        }

        cardResumen.setVisibility(View.VISIBLE);
        cardProductos.setVisibility(View.VISIBLE);
        tvResumenVentas.setText(String.valueOf(totalVentasGlobal));
        tvTotalNeto.setText("$" + String.format("%.2f", totalNetoGlobal));
        tvCardTitulo.setText("DESGLOSE POR PROVEEDOR");
        llProductos.removeAllViews();

        if (totales.isEmpty()) { agregarVacio("Sin ventas en el periodo"); return; }

        boolean primero = true;
        for (Map.Entry<Long, double[]> e : totales.entrySet()) {
            if (!primero) agregarSepGrueso();
            primero = false;

            long   idP    = e.getKey();
            double neto   = e.getValue()[0];
            boolean todoPagado = e.getValue()[2] > 0 && e.getValue()[2] >= e.getValue()[1];

            // Encabezado proveedor
            LinearLayout enc = new LinearLayout(this);
            enc.setOrientation(LinearLayout.HORIZONTAL);
            enc.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.graphics.drawable.GradientDrawable bgEnc =
                    new android.graphics.drawable.GradientDrawable();
            bgEnc.setColor(Color.parseColor("#EDE7F6"));
            bgEnc.setCornerRadius(dp(8));
            enc.setBackground(bgEnc);
            enc.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams encLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            encLp.bottomMargin = dp(8);
            enc.setLayoutParams(encLp);

            TextView tvNom = new TextView(this);
            tvNom.setText(nombres.get(idP));
            tvNom.setTextColor(Color.parseColor("#4A148C"));
            tvNom.setTextSize(14f);
            tvNom.setTypeface(null, Typeface.BOLD);
            tvNom.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvNeto = new TextView(this);
            tvNeto.setText("$" + String.format("%.2f", neto));
            tvNeto.setTextColor(Color.parseColor("#4A148C"));
            tvNeto.setTextSize(16f);
            tvNeto.setTypeface(null, Typeface.BOLD);

            // Badge estado
            TextView tvBadge = crearBadge(
                    todoPagado ? "✓ PAGADO" : "PENDIENTE",
                    todoPagado ? "#E8F5E9" : "#FFF8E1",
                    todoPagado ? "#2E7D32" : "#F57F17");
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bLp.setMarginStart(dp(8));
            tvBadge.setLayoutParams(bLp);

            enc.addView(tvNom);
            enc.addView(tvNeto);
            enc.addView(tvBadge);
            llProductos.addView(enc);

            // Productos del proveedor
            boolean primeroProd = true;
            for (Map.Entry<String, double[]> ep : prods.get(idP).entrySet()) {
                if (!primeroProd) agregarSepFino();
                primeroProd = false;
                double[] vp = ep.getValue();
                agregarFilaProductoIndent(ep.getKey(), (int) vp[0], vp[1], vp[2] > 0);
            }
        }
    }

    // ── UI helpers ───────────────────────────────────────────────────────────
    private void agregarFilaProducto(String nombre, int cant, double neto, boolean pagada) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.VERTICAL);
        fila.setPadding(0, dp(4), 0, dp(4));

        TextView tvNom = new TextView(this);
        tvNom.setText(nombre);
        tvNom.setTextColor(Color.parseColor("#4A148C"));
        tvNom.setTextSize(15f);
        tvNom.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.bottomMargin = dp(4);
        tvNom.setLayoutParams(nlp);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chips.addView(crearChip(cant + " uds", "#EDE7F6", "#6A0DAD"));
        chips.addView(crearChip("$" + String.format("%.2f", neto), "#F3E5F5", "#6A0DAD"));
        chips.addView(crearBadge(
                pagada ? "✓ PAGADO" : "PENDIENTE",
                pagada ? "#E8F5E9" : "#FFF8E1",
                pagada ? "#2E7D32" : "#F57F17"));

        fila.addView(tvNom);
        fila.addView(chips);
        llProductos.addView(fila);
    }

    private void agregarFilaProductoIndent(String nombre, int cant, double neto, boolean pagada) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8); lp.topMargin = dp(2); lp.bottomMargin = dp(2);
        fila.setLayoutParams(lp);

        TextView tvNom = new TextView(this);
        tvNom.setText(nombre);
        tvNom.setTextColor(Color.parseColor("#5E35B1"));
        tvNom.setTextSize(13f);
        tvNom.setTypeface(null, Typeface.BOLD);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chips.addView(crearChip(cant + " uds", "#EDE7F6", "#6A0DAD"));
        chips.addView(crearChip("$" + String.format("%.2f", neto), "#F3E5F5", "#6A0DAD"));
        chips.addView(crearBadge(
                pagada ? "✓ PAGADO" : "PENDIENTE",
                pagada ? "#E8F5E9" : "#FFF8E1",
                pagada ? "#2E7D32" : "#F57F17"));

        fila.addView(tvNom);
        fila.addView(chips);
        llProductos.addView(fila);
    }

    private TextView crearChip(String txt, String bg, String fg) {
        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextColor(Color.parseColor(fg));
        tv.setTextSize(11f);
        tv.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.parseColor(bg));
        d.setCornerRadius(dp(20));
        tv.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), dp(6), 0);
        tv.setLayoutParams(lp);
        tv.setPadding(dp(10), dp(4), dp(10), dp(4));
        return tv;
    }

    private TextView crearBadge(String txt, String bg, String fg) {
        TextView tv = crearChip(txt, bg, fg);
        tv.setTextSize(9f);
        return tv;
    }

    private void agregarSep() {
        View s = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.topMargin = dp(10); lp.bottomMargin = dp(10);
        s.setLayoutParams(lp);
        s.setBackgroundColor(Color.parseColor("#F3E5F5"));
        llProductos.addView(s);
    }

    private void agregarSepFino() {
        View s = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(dp(8), dp(6), 0, dp(6));
        s.setLayoutParams(lp);
        s.setBackgroundColor(Color.parseColor("#EDE7F6"));
        llProductos.addView(s);
    }

    private void agregarSepGrueso() {
        View s = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(2));
        lp.topMargin = dp(16); lp.bottomMargin = dp(16);
        s.setLayoutParams(lp);
        s.setBackgroundColor(Color.parseColor("#EDE7F6"));
        llProductos.addView(s);
    }

    private void agregarVacio(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#CE93D8"));
        tv.setTextSize(13f);
        tv.setPadding(0, dp(8), 0, dp(8));
        llProductos.addView(tv);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private boolean esPagada(Venta v, java.util.Set<Long> cortesPagados, List<long[]> rangos) {
        // Método principal: comparar por id_corte (exacto)
        if (v.getIdCorte() > 0) return cortesPagados.contains(v.getIdCorte());
        // Fallback para ventas antiguas con id_corte=0: usar rango de fecha
        for (long[] r : rangos) {
            if (v.getFecha() >= r[0] && v.getFecha() <= r[1]) return true;
        }
        return false;
    }

    private double calcularComision(DetalleVenta d, Venta v) {
        if ("tarjeta".equals(v.getMetodoPago()) && v.getComision() > 0 && v.getTotal() > 0) {
            return (d.getTotal() / v.getTotal()) * (v.getTotal() - v.getTotalRecibir());
        }
        return 0;
    }
}