package com.missaccesorios.app.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.Venta;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistorialVentasActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private List<Venta> ventas = new ArrayList<>();
    private ListView lvVentas;
    private TextView tvNumVentas, tvTotalBruto, tvTotalNeto;
    private Button btnHoy, btnAyer, btnSemana, btnMes, btnFechaInicio, btnFechaFin, btnBuscar;

    private long fechaInicioMs, fechaFinMs;
    private final SimpleDateFormat sdfFecha  = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat sdfHora   = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // Botón activo de acceso rápido
    private Button btnRapidoActivo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_ventas);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db             = new DatabaseHelper(this);
        lvVentas       = findViewById(R.id.lvVentas);
        tvNumVentas    = findViewById(R.id.tvNumVentas);
        tvTotalBruto   = findViewById(R.id.tvTotalBruto);
        tvTotalNeto    = findViewById(R.id.tvTotalNeto);
        btnHoy         = findViewById(R.id.btnHoy);
        btnAyer        = findViewById(R.id.btnAyer);
        btnSemana      = findViewById(R.id.btnSemana);
        btnMes         = findViewById(R.id.btnMes);
        btnFechaInicio = findViewById(R.id.btnFechaInicio);
        btnFechaFin    = findViewById(R.id.btnFechaFin);
        btnBuscar      = findViewById(R.id.btnBuscar);

        btnHoy.setOnClickListener(v    -> { setRapido(btnHoy);    cargarHoy(); });
        btnAyer.setOnClickListener(v   -> { setRapido(btnAyer);   cargarAyer(); });
        btnSemana.setOnClickListener(v -> { setRapido(btnSemana); cargarUltimos(7); });
        btnMes.setOnClickListener(v    -> { setRapido(btnMes);    cargarUltimos(30); });

        btnFechaInicio.setOnClickListener(v -> mostrarDatePicker(true));
        btnFechaFin.setOnClickListener(v    -> mostrarDatePicker(false));
        btnBuscar.setOnClickListener(v      -> { deseleccionarRapidos(); cargarRango(fechaInicioMs, fechaFinMs); });

        lvVentas.setOnItemClickListener((parent, view, pos, id) -> {
            Intent i = new Intent(this, DetalleVentaActivity.class);
            i.putExtra("venta_id", ventas.get(pos).getId());
            startActivity(i);
        });

        // Por defecto: cargar HOY
        cargarHoy();
        setRapido(btnHoy);
    }

    // ─── Accesos rápidos ─────────────────────────────────────────────────────
    private void setRapido(Button btn) {
        if (btnRapidoActivo != null) {
            btnRapidoActivo.setBackgroundResource(R.drawable.bg_input);
            btnRapidoActivo.setTextColor(Color.parseColor("#6A0DAD"));
        }
        btnRapidoActivo = btn;
        btn.setBackgroundResource(R.drawable.bg_btn_purple);
        btn.setTextColor(Color.WHITE);
    }

    private void deseleccionarRapidos() {
        if (btnRapidoActivo != null) {
            btnRapidoActivo.setBackgroundResource(R.drawable.bg_input);
            btnRapidoActivo.setTextColor(Color.parseColor("#6A0DAD"));
            btnRapidoActivo = null;
        }
    }

    // ─── Rangos de fecha ─────────────────────────────────────────────────────
    private void cargarHoy() {
        Calendar c = Calendar.getInstance();
        long inicio = inicioDia(c); long fin = finDia(c);
        btnFechaInicio.setText(sdfFecha.format(new Date(inicio)));
        btnFechaFin.setText(sdfFecha.format(new Date(fin)));
        fechaInicioMs = inicio; fechaFinMs = fin;
        cargarRango(inicio, fin);
    }

    private void cargarAyer() {
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -1);
        long inicio = inicioDia(c); long fin = finDia(c);
        btnFechaInicio.setText(sdfFecha.format(new Date(inicio)));
        btnFechaFin.setText(sdfFecha.format(new Date(fin)));
        fechaInicioMs = inicio; fechaFinMs = fin;
        cargarRango(inicio, fin);
    }

    private void cargarUltimos(int dias) {
        Calendar fin = Calendar.getInstance();
        Calendar ini = Calendar.getInstance(); ini.add(Calendar.DAY_OF_YEAR, -(dias - 1));
        long inicio = inicioDia(ini); long finMs = finDia(fin);
        btnFechaInicio.setText(sdfFecha.format(new Date(inicio)));
        btnFechaFin.setText(sdfFecha.format(new Date(finMs)));
        fechaInicioMs = inicio; fechaFinMs = finMs;
        cargarRango(inicio, finMs);
    }

    private long inicioDia(Calendar c) {
        Calendar d = (Calendar) c.clone();
        d.set(Calendar.HOUR_OF_DAY, 0); d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0); d.set(Calendar.MILLISECOND, 0);
        return d.getTimeInMillis();
    }

    private long finDia(Calendar c) {
        Calendar d = (Calendar) c.clone();
        d.set(Calendar.HOUR_OF_DAY, 23); d.set(Calendar.MINUTE, 59);
        d.set(Calendar.SECOND, 59); d.set(Calendar.MILLISECOND, 999);
        return d.getTimeInMillis();
    }

    // ─── DatePicker ──────────────────────────────────────────────────────────
    private void mostrarDatePicker(boolean esInicio) {
        Calendar cal = Calendar.getInstance();
        if (esInicio && fechaInicioMs > 0) cal.setTimeInMillis(fechaInicioMs);
        if (!esInicio && fechaFinMs > 0)   cal.setTimeInMillis(fechaFinMs);

        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d);
            if (esInicio) {
                fechaInicioMs = inicioDia(sel);
                btnFechaInicio.setText(sdfFecha.format(new Date(fechaInicioMs)));
            } else {
                fechaFinMs = finDia(sel);
                btnFechaFin.setText(sdfFecha.format(new Date(fechaFinMs)));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ─── Cargar ventas ────────────────────────────────────────────────────────
    private void cargarRango(long inicio, long fin) {
        ventas = db.obtenerVentasPorRango(inicio, fin);
        actualizarResumen();
        lvVentas.setAdapter(new VentaAdapter());
    }

    // ─── Resumen ─────────────────────────────────────────────────────────────
    private void actualizarResumen() {
        double bruto = 0, neto = 0;
        for (Venta v : ventas) {
            bruto += v.getTotal();
            neto  += getNetoVenta(v);
        }
        tvNumVentas.setText(String.valueOf(ventas.size()));
        tvTotalBruto.setText("$" + String.format("%.2f", bruto));
        tvTotalNeto.setText("$" + String.format("%.2f", neto));
    }

    // ─── Neto según método ───────────────────────────────────────────────────
    private double getNetoVenta(Venta v) {
        if ("tarjeta".equals(v.getMetodoPago()) && v.getTotalRecibir() > 0) {
            return v.getTotalRecibir();
        }
        return v.getTotal();
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────
    private class VentaAdapter extends BaseAdapter {
        @Override public int getCount() { return ventas.size(); }
        @Override public Venta getItem(int pos) { return ventas.get(pos); }
        @Override public long getItemId(int pos) { return ventas.get(pos).getId(); }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            if (cv == null) cv = getLayoutInflater().inflate(R.layout.item_venta, parent, false);

            Venta v = ventas.get(pos);

            TextView tvHora       = cv.findViewById(R.id.tvHora);
            TextView tvMetodo     = cv.findViewById(R.id.tvMetodo);
            TextView tvMetodoIcon = cv.findViewById(R.id.tvMetodoIcon);
            TextView tvTotalN     = cv.findViewById(R.id.tvTotalNeto);
            TextView tvProductos  = cv.findViewById(R.id.tvProductos);
            TextView tvBrutoInfo  = cv.findViewById(R.id.tvBrutoInfo);

            tvHora.setText(sdfHora.format(new Date(v.getFecha())));

            double neto = getNetoVenta(v);
            tvTotalN.setText("$" + String.format("%.2f", neto));

            int numProductos = v.getDetalles() != null ? v.getDetalles().size() : 0;
            tvProductos.setText(numProductos + (numProductos == 1 ? " producto" : " productos"));

            switch (v.getMetodoPago() != null ? v.getMetodoPago() : "") {
                case "tarjeta":
                    tvMetodoIcon.setText("💳");
                    tvMetodo.setText(" · Tarjeta");
                    // Mostrar bruto tachado solo para tarjeta
                    if (v.getComision() > 0) {
                        tvBrutoInfo.setText("bruto $" + String.format("%.2f", v.getTotal())
                                + " (-" + String.format("%.1f", v.getComision()) + "%)");
                    } else {
                        tvBrutoInfo.setText("");
                    }
                    break;
                case "transferencia":
                    tvMetodoIcon.setText("🏦");
                    tvMetodo.setText(" · Transferencia");
                    tvBrutoInfo.setText("");
                    break;
                default: // efectivo
                    tvMetodoIcon.setText("💵");
                    tvMetodo.setText(" · Efectivo");
                    tvBrutoInfo.setText("");
                    break;
            }

            return cv;
        }
    }
}
