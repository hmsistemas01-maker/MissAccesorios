package com.missaccesorios.app.adapters;

import android.content.Context;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.missaccesorios.app.models.Proveedor;
import java.util.List;

public class ProveedorAdapter extends ArrayAdapter<Proveedor> {
    public ProveedorAdapter(Context ctx, List<Proveedor> lista) {
        super(ctx, android.R.layout.simple_list_item_2, android.R.id.text1, lista);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View v = super.getView(pos, convertView, parent);
        Proveedor p = getItem(pos);
        ((TextView) v.findViewById(android.R.id.text1)).setText(p.getNombre());
        ((TextView) v.findViewById(android.R.id.text2)).setText(p.getTelefono() != null ? p.getTelefono() : "");
        return v;
    }
}
