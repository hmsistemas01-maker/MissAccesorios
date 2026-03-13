package com.missaccesorios.app.activities;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseContract;
import com.missaccesorios.app.database.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportesPagosActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private Calendar calInicio = Calendar.getInstance();
    private Calendar calFin    = Calendar.getInstance();

    private TextView tvFechaInicio, tvFechaFin;
    private TextView tvNumPagos, tvNumProvsPagados, tvTotalPagado;
    private LinearLayout llResumenProveedores, llDetallePagos;
    private CardView cardResumenPagos, cardResumenProveedores, cardDetallePagos;

    private final SimpleDateFormat sdfFecha  = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat sdfCorto  = new SimpleDateFormat("dd/MM/yy",   Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes_pagos);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);

        tvFechaInicio          = findViewById(R.id.tvFechaInicioPagos);
        tvFechaFin             = findViewById(R.id.tvFechaFinPagos);
        tvNumPagos             = findViewById(R.id.tvNumPagos);
        tvNumProvsPagados      = findViewById(R.id.tvNumProvsPagados);
        tvTotalPagado          = findViewById(R.id.tvTotalPagado);
        llResumenProveedores   = findViewById(R.id.llResumenProveedores);
        llDetallePagos         = findViewById(R.id.llDetallePagos);
        cardResumenPagos       = findViewById(R.id.cardResumenPagos);
        cardResumenProveedores = findViewById(R.id.cardResumenProveedores);
        cardDetallePagos       = findViewById(R.id.cardDetallePagos);

        actualizarFechas();
        tvFechaInicio.setOnClickListener(v -> mostrarPicker(true));
        tvFechaFin.setOnClickListener(v    -> mostrarPicker(false));
        findViewById(R.id.btnBuscarPagos).setOnClickListener(v -> cargar());

        cargar(); // cargar al abrir con rango del mes actual
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private void mostrarPicker(boolean esInicio) {
        Calendar cal = esInicio ? calInicio : calFin;
        new DatePickerDialog(this, (view, y, m, d) -> {
            if (esInicio) calInicio.set(y, m, d);
            else          calFin.set(y, m, d);
            actualizarFechas();
        }, cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void actualizarFechas() {
        tvFechaInicio.setText(sdfFecha.format(calInicio.getTime()));
        tvFechaFin.setText(sdfFecha.format(calFin.getTime()));
    }

    private long[] getRango() {
        Calendar i = (Calendar) calInicio.clone();
        i.set(Calendar.HOUR_OF_DAY, 0); i.set(Calendar.MINUTE, 0);
        i.set(Calendar.SECOND, 0);      i.set(Calendar.MILLISECOND, 0);
        Calendar f = (Calendar) calFin.clone();
        f.set(Calendar.HOUR_OF_DAY, 23); f.set(Calendar.MINUTE, 59);
        f.set(Calendar.SECOND, 59);      f.set(Calendar.MILLISECOND, 999);
        return new long[]{i.getTimeInMillis(), f.getTimeInMillis()};
    }

    // ── Cargar datos ─────────────────────────────────────────────────────────
    private void cargar() {
        long[] rango = getRango();

        // ── Resumen por proveedor (GROUP BY) ─────────────────────────────────
        Cursor cr = db.obtenerResumenPagosPorProveedor(rango[0], rango[1]);
        Map<String, double[]> resumenMap = new LinkedHashMap<>(); // nombre → {total, numPagos}
        double totalGlobal = 0;
        int    numPagosTotal = 0;

        while (cr.moveToNext()) {
            String prov  = cr.getString(cr.getColumnIndexOrThrow(
                    DatabaseContract.PagosProveedores.NOMBRE_PROVEEDOR));
            double total = cr.getDouble(cr.getColumnIndexOrThrow("total_pagado"));
            int    num   = cr.getInt(cr.getColumnIndexOrThrow("num_pagos"));
            resumenMap.put(prov, new double[]{total, num});
            totalGlobal  += total;
            numPagosTotal += num;
        }
        cr.close();

        // ── Cabecera resumen ─────────────────────────────────────────────────
        boolean hayDatos = !resumenMap.isEmpty();
        cardResumenPagos.setVisibility(hayDatos ? View.VISIBLE : View.GONE);
        cardResumenProveedores.setVisibility(hayDatos ? View.VISIBLE : View.GONE);
        cardDetallePagos.setVisibility(hayDatos ? View.VISIBLE : View.GONE);

        if (!hayDatos) return;

        tvNumPagos.setText(String.valueOf(numPagosTotal));
        tvNumProvsPagados.setText(String.valueOf(resumenMap.size()));
        tvTotalPagado.setText("$" + String.format("%.2f", totalGlobal));

        // ── Filas resumen por proveedor ──────────────────────────────────────
        llResumenProveedores.removeAllViews();
        boolean primero = true;
        for (Map.Entry<String, double[]> e : resumenMap.entrySet()) {
            if (!primero) agregarSep(llResumenProveedores, false);
            primero = false;

            double totalProv = e.getValue()[0];
            int    numProv   = (int) e.getValue()[1];

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setPadding(0, dp(6), 0, dp(6));

            // Círculo inicial
            LinearLayout circulo = new LinearLayout(this);
            circulo.setOrientation(LinearLayout.VERTICAL);
            circulo.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable bgCirc =
                    new android.graphics.drawable.GradientDrawable();
            bgCirc.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bgCirc.setColor(Color.parseColor("#EDE7F6"));
            circulo.setBackground(bgCirc);
            LinearLayout.LayoutParams circLp = new LinearLayout.LayoutParams(dp(38), dp(38));
            circLp.setMarginEnd(dp(12));
            circulo.setLayoutParams(circLp);
            TextView tvInic = new TextView(this);
            tvInic.setText(e.getKey().length() > 0
                    ? String.valueOf(e.getKey().charAt(0)).toUpperCase() : "?");
            tvInic.setTextColor(Color.parseColor("#9C27B0"));
            tvInic.setTextSize(16f);
            tvInic.setTypeface(null, Typeface.BOLD);
            circulo.addView(tvInic);

            // Nombre + num pagos
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView tvNomProv = new TextView(this);
            tvNomProv.setText(e.getKey());
            tvNomProv.setTextColor(Color.parseColor("#4A148C"));
            tvNomProv.setTextSize(14f);
            tvNomProv.setTypeface(null, Typeface.BOLD);
            TextView tvNumProv = new TextView(this);
            tvNumProv.setText(numProv + (numProv == 1 ? " pago" : " pagos"));
            tvNumProv.setTextColor(Color.parseColor("#AB47BC"));
            tvNumProv.setTextSize(12f);
            info.addView(tvNomProv);
            info.addView(tvNumProv);

            // Total
            TextView tvTot = new TextView(this);
            tvTot.setText("$" + String.format("%.2f", totalProv));
            tvTot.setTextColor(Color.parseColor("#4A148C"));
            tvTot.setTextSize(16f);
            tvTot.setTypeface(null, Typeface.BOLD);

            fila.addView(circulo);
            fila.addView(info);
            fila.addView(tvTot);
            llResumenProveedores.addView(fila);
        }

        // ── Pagos individuales ───────────────────────────────────────────────
        Cursor cd = db.obtenerPagosPorRango(rango[0], rango[1]);
        llDetallePagos.removeAllViews();
        boolean primeroPago = true;

        while (cd.moveToNext()) {
            String prov  = cd.getString(cd.getColumnIndexOrThrow(
                    DatabaseContract.PagosProveedores.NOMBRE_PROVEEDOR));
            double total = cd.getDouble(cd.getColumnIndexOrThrow(
                    DatabaseContract.PagosProveedores.TOTAL_PAGADO));
            long   fi        = cd.getLong(cd.getColumnIndexOrThrow(
                    DatabaseContract.PagosProveedores.FECHA_INICIO));
            long   ff        = cd.getLong(cd.getColumnIndexOrThrow(
                    DatabaseContract.PagosProveedores.FECHA_FIN));
            String firmaPath = "";
            int colFirma = cd.getColumnIndex(DatabaseContract.PagosProveedores.FIRMA_PATH);
            if (colFirma >= 0) firmaPath = cd.getString(colFirma);
            boolean tieneFirma = firmaPath != null && !firmaPath.isEmpty();

            if (!primeroPago) agregarSep(llDetallePagos, false);
            primeroPago = false;

            // Fila del pago
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.VERTICAL);
            fila.setPadding(0, dp(6), 0, dp(6));

            // Línea superior: nombre + monto + badges
            LinearLayout filaSup = new LinearLayout(this);
            filaSup.setOrientation(LinearLayout.HORIZONTAL);
            filaSup.setGravity(android.view.Gravity.CENTER_VERTICAL);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvNom = new TextView(this);
            tvNom.setText(prov);
            tvNom.setTextColor(Color.parseColor("#4A148C"));
            tvNom.setTextSize(14f);
            tvNom.setTypeface(null, Typeface.BOLD);

            TextView tvRango = new TextView(this);
            tvRango.setText(sdfCorto.format(new Date(fi)) + " → " + sdfCorto.format(new Date(ff)));
            tvRango.setTextColor(Color.parseColor("#AB47BC"));
            tvRango.setTextSize(11f);

            info.addView(tvNom);
            info.addView(tvRango);

            TextView tvMonto = new TextView(this);
            tvMonto.setText("$" + String.format("%.2f", total));
            tvMonto.setTextColor(Color.parseColor("#4A148C"));
            tvMonto.setTextSize(15f);
            tvMonto.setTypeface(null, Typeface.BOLD);

            // Badge PAGADO
            TextView tvBadge = crearBadge("✓ PAGADO", "#E8F5E9", "#2E7D32");
            // Badge FIRMADO (solo si hay firma)
            TextView tvFirmado = crearBadge("✍ FIRMADO", "#EDE7F6", "#6A0DAD");
            tvFirmado.setVisibility(tieneFirma ? View.VISIBLE : View.GONE);

            LinearLayout badges = new LinearLayout(this);
            badges.setOrientation(LinearLayout.VERTICAL);
            badges.setGravity(android.view.Gravity.END);
            LinearLayout.LayoutParams badgesLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            badgesLp.setMarginStart(dp(8));
            badges.setLayoutParams(badgesLp);

            LinearLayout.LayoutParams badgeLp2 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeLp2.bottomMargin = dp(4);
            tvBadge.setLayoutParams(badgeLp2);
            badges.addView(tvBadge);
            badges.addView(tvFirmado);

            filaSup.addView(info);
            filaSup.addView(tvMonto);
            filaSup.addView(badges);

            fila.addView(filaSup);
            llDetallePagos.addView(fila);
        }
        cd.close();
    }

    private TextView crearBadge(String texto, String bgHex, String textHex) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(Color.parseColor(textHex));
        tv.setTextSize(9f);
        tv.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(20));
        bg.setColor(Color.parseColor(bgHex));
        tv.setBackground(bg);
        tv.setPadding(dp(8), dp(3), dp(8), dp(3));
        return tv;
    }

    private void agregarSep(LinearLayout parent, boolean grueso) {
        View sep = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, grueso ? dp(2) : 1);
        lp.topMargin    = dp(grueso ? 12 : 8);
        lp.bottomMargin = dp(grueso ? 12 : 8);
        sep.setLayoutParams(lp);
        sep.setBackgroundColor(Color.parseColor("#F3E5F5"));
        parent.addView(sep);
    }
}