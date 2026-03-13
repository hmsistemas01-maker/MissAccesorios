package com.missaccesorios.app.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.DetalleVenta;
import com.missaccesorios.app.models.Venta;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetalleVentaActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private final SimpleDateFormat sdfCompleto =
            new SimpleDateFormat("dd/MM/yyyy  hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_venta);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);

        long ventaId = getIntent().getLongExtra("venta_id", -1);
        if (ventaId == -1) { finish(); return; }

        List<Venta> lista = db.obtenerVentasPorRango(0, Long.MAX_VALUE);
        Venta venta = null;
        for (Venta v : lista) { if (v.getId() == ventaId) { venta = v; break; } }
        if (venta == null) { finish(); return; }

        mostrarVenta(venta);
    }

    private void mostrarVenta(Venta v) {
        TextView tvFecha       = findViewById(R.id.tvFechaDetalle);
        TextView tvMetodo      = findViewById(R.id.tvMetodoDetalle);
        TextView tvTotalBruto  = findViewById(R.id.tvTotalBrutoDetalle);
        TextView tvTotalNeto   = findViewById(R.id.tvTotalNetoDetalle);

        LinearLayout layoutEfectivo      = findViewById(R.id.layoutDetalleEfectivo);
        LinearLayout layoutTarjeta       = findViewById(R.id.layoutDetalleTarjeta);
        LinearLayout layoutTransferencia = findViewById(R.id.layoutDetalleTransferencia);

        tvFecha.setText(sdfCompleto.format(new Date(v.getFecha())));

        boolean esTarjeta       = "tarjeta".equals(v.getMetodoPago());
        boolean esTransferencia = "transferencia".equals(v.getMetodoPago());

        // Totales en header
        double neto = esTarjeta && v.getTotalRecibir() > 0 ? v.getTotalRecibir() : v.getTotal();
        tvTotalNeto.setText("$" + String.format("%.2f", neto));

        if (esTarjeta && v.getComision() > 0) {
            tvTotalBruto.setText("bruto $" + String.format("%.2f", v.getTotal())
                    + "  (-" + String.format("%.1f", v.getComision()) + "%)");
            tvMetodo.setText("💳 Tarjeta");
        } else if (esTransferencia) {
            tvTotalBruto.setText("");
            tvMetodo.setText("🏦 Transferencia");
        } else {
            tvTotalBruto.setText("");
            tvMetodo.setText("💵 Efectivo");
        }

        // Panel de pago
        layoutEfectivo.setVisibility(View.GONE);
        layoutTarjeta.setVisibility(View.GONE);
        layoutTransferencia.setVisibility(View.GONE);

        if (esTarjeta) {
            layoutTarjeta.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvBrutoTarjeta))
                    .setText("$" + String.format("%.2f", v.getTotal()));
            ((TextView) findViewById(R.id.tvComisionDetalle))
                    .setText(String.format("%.2f", v.getComision()) + "%");
            ((TextView) findViewById(R.id.tvNetoTarjeta))
                    .setText("$" + String.format("%.2f", neto));
        } else if (esTransferencia) {
            layoutTransferencia.setVisibility(View.VISIBLE);
            String ref = v.getReferencia();
            ((TextView) findViewById(R.id.tvReferenciaDetalle))
                    .setText(ref != null && !ref.isEmpty() ? ref : "—");
        } else {
            layoutEfectivo.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvPagoCon))
                    .setText("$" + String.format("%.2f", v.getPagaCon()));
            ((TextView) findViewById(R.id.tvCambioDetalle))
                    .setText("$" + String.format("%.2f", v.getCambio()));
        }

        // Productos
        LinearLayout llProductos = findViewById(R.id.llProductosDetalle);
        List<DetalleVenta> detalles = v.getDetalles();
        double factorNeto = esTarjeta && v.getComision() > 0
                ? 1.0 - (v.getComision() / 100.0) : 1.0;

        if (detalles != null) {
            for (DetalleVenta d : detalles) {
                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.VERTICAL);
                fila.setPadding(0, 8, 0, 8);

                double totalNeto    = d.getTotal() * factorNeto;
                double precioNeto   = d.getPrecio() * factorNeto;

                // Línea 1: nombre x cant → $total neto
                TextView lNombre = new TextView(this);
                lNombre.setText(d.getNombreProducto() + "  x" + d.getCantidad()
                        + "  →  $" + String.format("%.2f", totalNeto));
                lNombre.setTextColor(Color.parseColor("#4A148C"));
                lNombre.setTextSize(13f);
                lNombre.setTypeface(null, Typeface.BOLD);

                // Línea 2: proveedor | precio neto c/u  (si tarjeta agrega nota bruto)
                String infoPrecio = d.getNombreProveedor() + "  |  $"
                        + String.format("%.2f", precioNeto) + " c/u";
                if (esTarjeta && v.getComision() > 0) {
                    infoPrecio += "  (bruto $" + String.format("%.2f", d.getPrecio()) + ")";
                }
                TextView lProv = new TextView(this);
                lProv.setText(infoPrecio);
                lProv.setTextColor(Color.parseColor("#AB47BC"));
                lProv.setTextSize(11f);

                // Separador
                View sep = new View(this);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                sp.topMargin = 6;
                sep.setLayoutParams(sp);
                sep.setBackgroundColor(Color.parseColor("#F3E5F5"));

                fila.addView(lNombre);
                fila.addView(lProv);
                fila.addView(sep);
                llProductos.addView(fila);
            }
        }
    }
}