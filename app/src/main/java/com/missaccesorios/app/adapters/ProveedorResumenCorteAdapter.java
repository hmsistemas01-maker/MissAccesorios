package com.missaccesorios.app.adapters;

import android.content.Context;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class ProveedorResumenCorteAdapter extends ArrayAdapter<String[]> {
    public ProveedorResumenCorteAdapter(Context ctx, List<String[]> lista) {
        super(ctx, android.R.layout.simple_list_item_2, android.R.id.text1, lista);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View v = super.getView(pos, convertView, parent);
        String[] row = getItem(pos);
        // row: [nombre, bruto, comision, neto]
        ((TextView) v.findViewById(android.R.id.text1)).setText(row[0] + " — Neto: " + row[3]);
        ((TextView) v.findViewById(android.R.id.text2)).setText("Bruto: " + row[1] + " | Com: " + row[2]);
        return v;
    }
}
