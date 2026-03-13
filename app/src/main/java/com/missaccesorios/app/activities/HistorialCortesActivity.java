package com.missaccesorios.app.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseContract;
import com.missaccesorios.app.database.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistorialCortesActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ListView listView;
    private TextView tvTotalCortes;

    private final List<long[]>   rangos  = new ArrayList<>();
    private final List<double[]> totales = new ArrayList<>(); // {totalReal, efectivo, tarjeta, transf, comision}
    private final List<Date>     fechas  = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_cortes);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db            = new DatabaseHelper(this);
        listView      = findViewById(R.id.lvCortes);
        tvTotalCortes = findViewById(R.id.tvTotalCortes);

        cargarCortes();

        listView.setOnItemClickListener((parent, v, pos, id) -> {
            Intent intent = new Intent(this, DetalleCorteActivity.class);
            intent.putExtra("inicio", rangos.get(pos)[0]);
            intent.putExtra("fin",    rangos.get(pos)[1]);
            startActivity(intent);
        });
    }

    private void cargarCortes() {
        rangos.clear(); totales.clear(); fechas.clear();

        Cursor c = db.obtenerCortes();
        while (c.moveToNext()) {
            long   fi  = c.getLong(c.getColumnIndexOrThrow(DatabaseContract.CortesCaja.FECHA_INICIO));
            long   ff  = c.getLong(c.getColumnIndexOrThrow(DatabaseContract.CortesCaja.FECHA_FIN));
            double tot = c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.CortesCaja.TOTAL_REAL));
            double ef  = c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.CortesCaja.TOTAL_EFECTIVO));
            double tar = c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.CortesCaja.TOTAL_TARJETA));
            double tr  = c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.CortesCaja.TOTAL_TRANSFERENCIA));
            double com = c.getDouble(c.getColumnIndexOrThrow(DatabaseContract.CortesCaja.COMISION_TARJETA));

            rangos.add(new long[]{fi, ff});
            totales.add(new double[]{tot, ef, tar, tr, com});
            fechas.add(new Date(fi));
        }
        c.close();

        tvTotalCortes.setText(rangos.size() + " registros");
        listView.setAdapter(new CorteAdapter());
    }

    private class CorteAdapter extends BaseAdapter {
        private final SimpleDateFormat sdfDia   = new SimpleDateFormat("dd",         Locale.getDefault());
        private final SimpleDateFormat sdfMes   = new SimpleDateFormat("MMM",        Locale.getDefault());
        private final SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        @Override public int getCount()          { return rangos.size(); }
        @Override public Object getItem(int pos) { return rangos.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(HistorialCortesActivity.this)
                        .inflate(R.layout.item_corte, parent, false);
            }

            Date   fecha = fechas.get(pos);
            double tot   = totales.get(pos)[0];
            double ef    = totales.get(pos)[1];
            double tar   = totales.get(pos)[2];
            double tr    = totales.get(pos)[3];
            double com   = totales.get(pos)[4];
            double bruto = ef + tar + tr;

            ((TextView) convertView.findViewById(R.id.tvDiaCorte))
                    .setText(sdfDia.format(fecha));
            ((TextView) convertView.findViewById(R.id.tvMesCorte))
                    .setText(sdfMes.format(fecha).toUpperCase(Locale.getDefault()));
            ((TextView) convertView.findViewById(R.id.tvFechaCorteItem))
                    .setText(sdfFecha.format(fecha));

            // Resumen métodos
            List<String> partes = new ArrayList<>();
            if (ef  > 0) partes.add("💵 $" + String.format("%.0f", ef));
            if (tar > 0) partes.add("💳 $" + String.format("%.0f", tar));
            if (tr  > 0) partes.add("🏦 $" + String.format("%.0f", tr));
            ((TextView) convertView.findViewById(R.id.tvVentasCorteItem))
                    .setText(partes.isEmpty() ? "Sin ventas" : String.join("  ", partes));

            ((TextView) convertView.findViewById(R.id.tvNetoCorteItem))
                    .setText("$" + String.format("%.2f", tot));

            TextView tvBruto = convertView.findViewById(R.id.tvBrutoCorteItem);
            tvBruto.setText(com > 0 ? "bruto $" + String.format("%.2f", bruto) : "");

            return convertView;
        }
    }
}