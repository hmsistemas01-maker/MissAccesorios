package com.missaccesorios.app.adapters;

import android.content.Context;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.missaccesorios.app.models.Venta;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistorialVentasAdapter extends ArrayAdapter<Venta> {
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public HistorialVentasAdapter(Context ctx, List<Venta> lista) {
        super(ctx, android.R.layout.simple_list_item_2, android.R.id.text1, lista);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View v = super.getView(pos, convertView, parent);
        Venta venta = getItem(pos);
        ((TextView) v.findViewById(android.R.id.text1))
                .setText("#" + venta.getId() + " — $" + String.format("%.2f", venta.getTotal()) +
                        " — " + venta.getMetodoPago());
        ((TextView) v.findViewById(android.R.id.text2))
                .setText(sdf.format(new Date(venta.getFecha())) + " | " +
                        (venta.isCortada() ? "✓ Cortada" : "⏳ Pendiente"));
        return v;
    }
}
