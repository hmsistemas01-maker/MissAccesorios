package com.missaccesorios.app.adapters;

import android.content.Context;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class ReporteProveedorNetoAdapter extends ArrayAdapter<String[]> {
    public ReporteProveedorNetoAdapter(Context ctx, List<String[]> lista) {
        super(ctx, android.R.layout.simple_list_item_2, android.R.id.text1, lista);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View v = super.getView(pos, convertView, parent);
        String[] row = getItem(pos);
        // row: [producto, cantidad, bruto, neto]
        ((TextView) v.findViewById(android.R.id.text1)).setText(row[0] + " x" + row[1] + " — Neto: " + row[3]);
        ((TextView) v.findViewById(android.R.id.text2)).setText("Bruto: " + row[2]);
        return v;
    }
}
