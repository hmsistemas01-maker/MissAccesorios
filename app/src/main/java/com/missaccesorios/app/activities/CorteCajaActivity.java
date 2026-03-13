package com.missaccesorios.app.activities;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.DetalleVenta;
import com.missaccesorios.app.models.Venta;
import java.text.SimpleDateFormat;
import java.util.*;

public class CorteCajaActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private Calendar calFecha = Calendar.getInstance();

    private TextView tvFecha;
    private TextView tvContEfectivo, tvTotalEfectivo;
    private TextView tvContTarjeta, tvBrutoTarjeta, tvComisionTarjeta, tvTotalTarjeta;
    private TextView tvContTransf, tvTotalTransf;
    private TextView tvContTotal, tvTotalNeto;
    private LinearLayout llProveedoresCorte;
    private Button btnCorte;

    private List<Venta> ventasDia = new ArrayList<>();
    private final SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corte_caja);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);

        tvFecha           = findViewById(R.id.tvFechaCorte);
        tvContEfectivo    = findViewById(R.id.tvContEfectivo);
        tvTotalEfectivo   = findViewById(R.id.tvTotalEfectivo);
        tvContTarjeta     = findViewById(R.id.tvContTarjeta);
        tvBrutoTarjeta    = findViewById(R.id.tvBrutoTarjeta);
        tvComisionTarjeta = findViewById(R.id.tvComisionTarjeta);
        tvTotalTarjeta    = findViewById(R.id.tvTotalTarjeta);
        tvContTransf      = findViewById(R.id.tvContTransf);
        tvTotalTransf     = findViewById(R.id.tvTotalTransf);
        tvContTotal       = findViewById(R.id.tvContTotal);
        tvTotalNeto       = findViewById(R.id.tvTotalNeto);
        llProveedoresCorte = findViewById(R.id.llProveedoresCorte);
        btnCorte          = findViewById(R.id.btnRealizarCorte);

        actualizarFecha();

        tvFecha.setOnClickListener(v ->
                new DatePickerDialog(this, (view, y, m, d) -> {
                    calFecha.set(y, m, d);
                    actualizarFecha();
                }, calFecha.get(Calendar.YEAR),
                        calFecha.get(Calendar.MONTH),
                        calFecha.get(Calendar.DAY_OF_MONTH)).show()
        );
    }

    private void actualizarFecha() {
        tvFecha.setText(sdfFecha.format(calFecha.getTime()));
        cargarResumen();
    }

    // Convierte dp a px
    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void cargarResumen() {
        Calendar inicio = (Calendar) calFecha.clone();
        inicio.set(Calendar.HOUR_OF_DAY, 0); inicio.set(Calendar.MINUTE, 0);
        inicio.set(Calendar.SECOND, 0);      inicio.set(Calendar.MILLISECOND, 0);
        Calendar fin = (Calendar) calFecha.clone();
        fin.set(Calendar.HOUR_OF_DAY, 23); fin.set(Calendar.MINUTE, 59);
        fin.set(Calendar.SECOND, 59);      fin.set(Calendar.MILLISECOND, 999);

        ventasDia = db.obtenerVentasNoCortadasPorRango(
                inicio.getTimeInMillis(), fin.getTimeInMillis());

        // ── Contadores y totales ─────────────────────────────────────────────
        int cntEfectivo = 0, cntTarjeta = 0, cntTransf = 0;
        double totEfectivo = 0, totTarjetaBruto = 0, totTarjetaNeto = 0;
        double totTransf = 0, comisionTotal = 0;

        for (Venta v : ventasDia) {
            String metodo = v.getMetodoPago() != null ? v.getMetodoPago() : "";
            if ("efectivo".equals(metodo)) {
                cntEfectivo++;
                totEfectivo += v.getTotal();
            } else if ("tarjeta".equals(metodo)) {
                cntTarjeta++;
                totTarjetaBruto += v.getTotal();
                double neto = v.getTotalRecibir() > 0 ? v.getTotalRecibir() : v.getTotal();
                totTarjetaNeto  += neto;
                comisionTotal   += v.getTotal() - neto;
            } else {
                cntTransf++;
                totTransf += v.getTotal();
            }
        }

        double totalNeto   = totEfectivo + totTarjetaNeto + totTransf;
        int    totalVentas = ventasDia.size();

        // ── Actualizar vistas de metodos ────────────────────────────────────
        tvContEfectivo.setText("  " + cntEfectivo + (cntEfectivo == 1 ? " venta" : " ventas"));
        tvTotalEfectivo.setText("$" + String.format("%.2f", totEfectivo));

        tvContTarjeta.setText("  " + cntTarjeta + (cntTarjeta == 1 ? " venta" : " ventas"));
        if (comisionTotal > 0) {
            tvBrutoTarjeta.setText("bruto $" + String.format("%.2f", totTarjetaBruto));
            tvComisionTarjeta.setText("comision -$" + String.format("%.2f", comisionTotal));
        } else {
            tvBrutoTarjeta.setText("");
            tvComisionTarjeta.setText("");
        }
        tvTotalTarjeta.setText("$" + String.format("%.2f", totTarjetaNeto));

        tvContTransf.setText("  " + cntTransf + (cntTransf == 1 ? " venta" : " ventas"));
        tvTotalTransf.setText("$" + String.format("%.2f", totTransf));

        tvContTotal.setText(totalVentas + (totalVentas == 1 ? " venta" : " ventas"));
        tvTotalNeto.setText("$" + String.format("%.2f", totalNeto));

        // ── Desglose por proveedor ───────────────────────────────────────────
        Map<Long, double[]> porProv = new LinkedHashMap<>();
        Map<Long, String>   nombres = new HashMap<>();

        for (Venta v : ventasDia) {
            for (DetalleVenta d : v.getDetalles()) {
                long idP = d.getIdProveedor();
                if (!porProv.containsKey(idP)) {
                    porProv.put(idP, new double[]{0, 0});
                    nombres.put(idP, d.getNombreProveedor());
                }
                double propCom = 0;
                if ("tarjeta".equals(v.getMetodoPago()) && v.getComision() > 0 && v.getTotal() > 0) {
                    propCom = (d.getTotal() / v.getTotal()) * (v.getTotal() - v.getTotalRecibir());
                }
                porProv.get(idP)[0] += d.getTotal();
                porProv.get(idP)[1] += propCom;
            }
        }

        // Construir filas en el LinearLayout (sin adapter, sin ListView)
        llProveedoresCorte.removeAllViews();

        if (porProv.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("Sin ventas para este dia");
            tvVacio.setTextColor(Color.parseColor("#CE93D8"));
            tvVacio.setTextSize(13f);
            tvVacio.setPadding(0, dp(8), 0, dp(8));
            llProveedoresCorte.addView(tvVacio);
        }

        boolean primero = true;
        for (Map.Entry<Long, double[]> e : porProv.entrySet()) {
            double bruto = e.getValue()[0];
            double com   = e.getValue()[1];
            double neto  = bruto - com;
            String nombre = nombres.get(e.getKey());

            // Separador entre proveedores (no antes del primero)
            if (!primero) {
                View sep = new View(this);
                LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                sepLp.topMargin    = dp(10);
                sepLp.bottomMargin = dp(10);
                sep.setLayoutParams(sepLp);
                sep.setBackgroundColor(Color.parseColor("#F3E5F5"));
                llProveedoresCorte.addView(sep);
            }
            primero = false;

            // Contenedor de la fila del proveedor
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            filaLp.bottomMargin = dp(2);
            fila.setLayoutParams(filaLp);
            fila.setPadding(0, dp(6), 0, dp(6));

            // Fila superior: nombre | neto
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvNombre = new TextView(this);
            tvNombre.setText(nombre);
            tvNombre.setTextColor(Color.parseColor("#4A148C"));
            tvNombre.setTextSize(15f);
            tvNombre.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvNombre.setLayoutParams(nlp);

            TextView tvNeto = new TextView(this);
            tvNeto.setText("$" + String.format("%.2f", neto));
            tvNeto.setTextColor(Color.parseColor("#4A148C"));
            tvNeto.setTextSize(17f);
            tvNeto.setTypeface(null, Typeface.BOLD);

            top.addView(tvNombre);
            top.addView(tvNeto);

            // Fila inferior: bruto (y comision si aplica)
            TextView tvSub = new TextView(this);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = dp(3);
            tvSub.setLayoutParams(subLp);
            if (com > 0) {
                tvSub.setText("bruto $" + String.format("%.2f", bruto)
                        + "   comision -$" + String.format("%.2f", com));
            } else {
                tvSub.setText("bruto $" + String.format("%.2f", bruto));
            }
            tvSub.setTextColor(Color.parseColor("#AB47BC"));
            tvSub.setTextSize(12f);

            fila.addView(top);
            fila.addView(tvSub);
            llProveedoresCorte.addView(fila);
        }

        // ── Boton corte — copias final para lambdas ─────────────────────────
        btnCorte.setEnabled(!ventasDia.isEmpty());

        final double fEfectivo    = totEfectivo;
        final double fTarBruto    = totTarjetaBruto;
        final double fTarNeto     = totTarjetaNeto;
        final double fTransf      = totTransf;
        final double fComision    = comisionTotal;
        final double fNeto        = totalNeto;
        final int    fCntEf       = cntEfectivo;
        final int    fCntTar      = cntTarjeta;
        final int    fCntTr       = cntTransf;
        final int    fTotalVentas = totalVentas;
        final long   iniMs        = inicio.getTimeInMillis();
        final long   finMs        = fin.getTimeInMillis();

        btnCorte.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Confirmar Corte")
                        .setMessage(String.format(
                                "\u00bfRealizar corte de %d ventas?\n\n" +
                                        "Efectivo (%d):      $%.2f\n" +
                                        "Tarjeta (%d):       $%.2f\n" +
                                        "Transferencia (%d): $%.2f\n\n" +
                                        "Total neto: $%.2f",
                                fTotalVentas,
                                fCntEf,  fEfectivo,
                                fCntTar, fTarNeto,
                                fCntTr,  fTransf,
                                fNeto))
                        .setPositiveButton("Confirmar", (d, w) -> {
                            List<Long> ventaIds = new ArrayList<>();
                            for (Venta vv : ventasDia) ventaIds.add(vv.getId());
                            db.realizarCorte(iniMs, finMs,
                                    fEfectivo + fTarBruto + fTransf,
                                    fEfectivo, fTarBruto, fTransf,
                                    fComision, fNeto, ventaIds);
                            Toast.makeText(this, "Corte realizado", Toast.LENGTH_SHORT).show();
                            cargarResumen();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show()
        );
    }
}