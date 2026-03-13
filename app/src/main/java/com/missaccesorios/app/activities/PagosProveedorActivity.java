package com.missaccesorios.app.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.DetalleVenta;
import com.missaccesorios.app.models.Proveedor;
import com.missaccesorios.app.models.Venta;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class PagosProveedorActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private Spinner  spinnerProvPago;
    private TextView tvFechaFinPago, tvDesdeAutomatico;
    private TextView tvPeriodoDesde, tvPeriodoHasta, tvPeriodoVentas, tvPeriodoTotal;
    private LinearLayout llProductosPago;
    private CardView cardPeriodoPago, cardProductosPago;
    private Button   btnConfirmarPago;

    private Calendar calFin = Calendar.getInstance();
    private SimpleDateFormat sdfDisplay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private List<Proveedor> listaProveedores = new ArrayList<>();
    // Resultado de la búsqueda actual
    private long   inicioCalculado = -1;
    private long   finCalculado    = -1;
    private double totalNetoCalculado = 0;
    private int    numVentasCalculado = 0;
    private long   idCorteActual   = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagos_proveedor);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);

        spinnerProvPago  = findViewById(R.id.spinnerProvPago);
        tvFechaFinPago   = findViewById(R.id.tvFechaFinPago);
        tvDesdeAutomatico= findViewById(R.id.tvDesdeAutomatico);
        tvPeriodoDesde   = findViewById(R.id.tvPeriodoDesde);
        tvPeriodoHasta   = findViewById(R.id.tvPeriodoHasta);
        tvPeriodoVentas  = findViewById(R.id.tvPeriodoVentas);
        tvPeriodoTotal   = findViewById(R.id.tvPeriodoTotal);
        llProductosPago  = findViewById(R.id.llProductosPago);
        cardPeriodoPago  = findViewById(R.id.cardPeriodoPago);
        cardProductosPago= findViewById(R.id.cardProductosPago);
        btnConfirmarPago = findViewById(R.id.btnConfirmarPago);

        // Fecha fin = hoy por defecto
        tvFechaFinPago.setText(sdfDisplay.format(calFin.getTime()));

        cargarProveedores();

        // Al cambiar proveedor, actualizar la etiqueta "Desde"
        spinnerProvPago.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                actualizarDesdeLabel();
                limpiarResultados();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        findViewById(R.id.btnElegirFechaFin).setOnClickListener(v -> mostrarDatePicker());
        findViewById(R.id.btnBuscarPago).setOnClickListener(v -> buscar());
        btnConfirmarPago.setOnClickListener(v -> mostrarDialogoPago());
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private void cargarProveedores() {
        listaProveedores.clear();
        listaProveedores.addAll(db.obtenerProveedores());
        ArrayAdapter<Proveedor> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaProveedores);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvPago.setAdapter(a);
        actualizarDesdeLabel();
    }

    private void actualizarDesdeLabel() {
        if (listaProveedores.isEmpty()) return;
        Proveedor sel = (Proveedor) spinnerProvPago.getSelectedItem();
        if (sel == null) return;

        long ultimoPago = db.obtenerFechaUltimoPagoProveedor(sel.getId());
        if (ultimoPago > 0) {
            tvDesdeAutomatico.setText("Último pago: " + sdfDisplay.format(new Date(ultimoPago)));
        } else {
            tvDesdeAutomatico.setText("Sin pagos previos");
        }
    }

    private void mostrarDatePicker() {
        new DatePickerDialog(this, (view, y, m, d) -> {
            calFin.set(y, m, d);
            tvFechaFinPago.setText(sdfDisplay.format(calFin.getTime()));
            limpiarResultados();
        }, calFin.get(Calendar.YEAR),
                calFin.get(Calendar.MONTH),
                calFin.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void limpiarResultados() {
        cardPeriodoPago.setVisibility(View.GONE);
        cardProductosPago.setVisibility(View.GONE);
        btnConfirmarPago.setVisibility(View.GONE);
        llProductosPago.removeAllViews();
        totalNetoCalculado = 0;
        numVentasCalculado = 0;
        inicioCalculado    = -1;
        finCalculado       = -1;
    }

    // ── Buscar ───────────────────────────────────────────────────────────────
    private void buscar() {
        if (listaProveedores.isEmpty()) {
            Toast.makeText(this, "No hay proveedores registrados", Toast.LENGTH_SHORT).show();
            return;
        }
        Proveedor prov = (Proveedor) spinnerProvPago.getSelectedItem();
        if (prov == null) return;

        // Obtener TODAS las ventas pendientes de pago para este proveedor
        // usando id_corte — sin rangos de fecha, sin ambigüedad
        List<Venta> ventas = db.obtenerVentasPendientesDePago(prov.getId());

        // El corte más reciente con ventas pendientes es el que se asocia al pago
        idCorteActual = db.obtenerUltimoCorteConVentasPendientes(prov.getId());

        // Para mostrar el rango de fechas del período pendiente
        inicioCalculado = -1;
        finCalculado    = -1;
        for (Venta v : ventas) {
            if (inicioCalculado < 0 || v.getFecha() < inicioCalculado) inicioCalculado = v.getFecha();
            if (v.getFecha() > finCalculado) finCalculado = v.getFecha();
        }

        Map<String, double[]> productos = new LinkedHashMap<>();
        totalNetoCalculado = 0;
        numVentasCalculado = 0;

        for (Venta v : ventas) {
            boolean contada = false;
            for (DetalleVenta d : v.getDetalles()) {
                if (d.getIdProveedor() != prov.getId()) continue;
                if (!contada) { numVentasCalculado++; contada = true; }
                double com  = calcularComision(d, v);
                double neto = d.getTotal() - com;
                productos.computeIfAbsent(d.getNombreProducto(), k -> new double[]{0, 0});
                productos.get(d.getNombreProducto())[0] += d.getCantidad();
                productos.get(d.getNombreProducto())[1] += neto;
                totalNetoCalculado += neto;
            }
        }

        // Mostrar período
        tvPeriodoDesde.setText(inicioCalculado > 0 ? sdfDisplay.format(new Date(inicioCalculado)) : "—");
        tvPeriodoHasta.setText(finCalculado > 0 ? sdfDisplay.format(new Date(finCalculado)) : "—");
        tvPeriodoVentas.setText(String.valueOf(numVentasCalculado));
        tvPeriodoTotal.setText("$" + String.format("%.2f", totalNetoCalculado));
        cardPeriodoPago.setVisibility(View.VISIBLE);

        // Mostrar productos
        llProductosPago.removeAllViews();
        cardProductosPago.setVisibility(View.VISIBLE);

        if (productos.isEmpty()) {
            agregarVacio("Sin ventas cortadas pendientes de pago");
            btnConfirmarPago.setVisibility(View.GONE);
        } else {
            boolean primero = true;
            for (Map.Entry<String, double[]> e : productos.entrySet()) {
                if (!primero) agregarSep();
                primero = false;
                double[] v2 = e.getValue();
                agregarFila(e.getKey(), (int) v2[0], v2[1]);
            }
            // Siempre habilitar si hay ventas pendientes
            // (registrarPagoProveedor maneja múltiples cortes internamente)
            btnConfirmarPago.setText("Confirmar Pago  $" + String.format("%.2f", totalNetoCalculado));
            btnConfirmarPago.setVisibility(View.VISIBLE);
        }
    }

    // ── Diálogo confirmación + firma ─────────────────────────────────────────
    private void mostrarDialogoPago() {
        Proveedor prov = (Proveedor) spinnerProvPago.getSelectedItem();
        if (prov == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Pago");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        // Resumen
        TextView tvMonto = new TextView(this);
        tvMonto.setText("Proveedor: " + prov.getNombre() +
                "\n\nTotal a pagar: $" + String.format("%.2f", totalNetoCalculado));
        tvMonto.setTextColor(Color.parseColor("#4A148C"));
        tvMonto.setTextSize(15f);
        tvMonto.setTypeface(null, Typeface.BOLD);
        tvMonto.setPadding(0, 0, 0, dp(16));
        layout.addView(tvMonto);

        // Etiqueta firma
        TextView tvFirmaLabel = new TextView(this);
        tvFirmaLabel.setText("Firma del proveedor (opcional):");
        tvFirmaLabel.setTextColor(Color.parseColor("#9C27B0"));
        tvFirmaLabel.setTextSize(12f);
        layout.addView(tvFirmaLabel);

        // Canvas firma
        FirmaView firmaView = new FirmaView(this);
        LinearLayout.LayoutParams firmaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
        firmaLp.topMargin = dp(8);
        firmaView.setLayoutParams(firmaLp);
        layout.addView(firmaView);

        // Botón limpiar firma
        Button btnLimpiar = new Button(this);
        btnLimpiar.setText("Limpiar firma");
        btnLimpiar.setTextSize(12f);
        btnLimpiar.setOnClickListener(v -> firmaView.limpiar());
        LinearLayout.LayoutParams blLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blLp.gravity = android.view.Gravity.END;
        btnLimpiar.setLayoutParams(blLp);
        layout.addView(btnLimpiar);

        builder.setView(layout);
        builder.setPositiveButton("Registrar Pago", (dialog, which) -> {
            String firmaPath = "";
            if (firmaView.tieneFirma()) {
                firmaPath = guardarFirma(firmaView.obtenerBitmap(), prov.getId());
            }
            registrarPago(prov, firmaPath);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void registrarPago(Proveedor prov, String firmaPath) {
        long id = db.registrarPagoProveedor(
                prov.getId(), prov.getNombre(),
                inicioCalculado, finCalculado,
                idCorteActual,
                totalNetoCalculado, totalNetoCalculado,
                firmaPath);

        if (id > 0) {
            Toast.makeText(this, "✓ Pago registrado correctamente", Toast.LENGTH_LONG).show();
            limpiarResultados();
            actualizarDesdeLabel();
        } else {
            Toast.makeText(this, "Error al registrar el pago", Toast.LENGTH_SHORT).show();
        }
    }

    private String guardarFirma(Bitmap bmp, long idProv) {
        try {
            File dir = new File(getFilesDir(), "firmas");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "firma_prov_" + idProv + "_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }

    // ── UI helpers ───────────────────────────────────────────────────────────
    private void agregarFila(String nombre, int cant, double neto) {
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
        chips.addView(crearChip(cant + " uds", "#EDE7F6", "#6A0DAD"));
        TextView chipNeto = crearChip("$" + String.format("%.2f", neto), "#F3E5F5", "#6A0DAD");
        chipNeto.setTypeface(null, Typeface.BOLD);
        chips.addView(chipNeto);

        fila.addView(tvNom);
        fila.addView(chips);
        llProductosPago.addView(fila);
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

    private void agregarSep() {
        View s = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.topMargin = dp(10); lp.bottomMargin = dp(10);
        s.setLayoutParams(lp);
        s.setBackgroundColor(Color.parseColor("#F3E5F5"));
        llProductosPago.addView(s);
    }

    private void agregarVacio(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#CE93D8"));
        tv.setTextSize(13f);
        tv.setPadding(0, dp(8), 0, dp(8));
        llProductosPago.addView(tv);
    }

    private void agregarAviso(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#F57F17"));
        tv.setTextSize(13f);
        tv.setPadding(0, dp(12), 0, dp(4));
        llProductosPago.addView(tv);
    }

    private double calcularComision(DetalleVenta d, Venta v) {
        if ("tarjeta".equals(v.getMetodoPago()) && v.getComision() > 0 && v.getTotal() > 0) {
            return (d.getTotal() / v.getTotal()) * (v.getTotal() - v.getTotalRecibir());
        }
        return 0;
    }

    // ── Canvas de firma ───────────────────────────────────────────────────────
    static class FirmaView extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path  path  = new Path();
        private boolean hasFirma = false;

        FirmaView(Context ctx) {
            super(ctx);
            paint.setColor(Color.parseColor("#4A148C"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            setBackgroundColor(Color.WHITE);
            android.graphics.drawable.GradientDrawable border =
                    new android.graphics.drawable.GradientDrawable();
            border.setColor(Color.WHITE);
            border.setStroke(2, Color.parseColor("#CE93D8"));
            border.setCornerRadius(12f);
            setBackground(border);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN: path.moveTo(x, y); hasFirma = true; break;
                case MotionEvent.ACTION_MOVE: path.lineTo(x, y); invalidate(); break;
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPath(path, paint);
        }

        void limpiar() { path.reset(); hasFirma = false; invalidate(); }
        boolean tieneFirma() { return hasFirma; }

        Bitmap obtenerBitmap() {
            Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(Color.WHITE);
            c.drawPath(path, paint);
            return bmp;
        }
    }
}