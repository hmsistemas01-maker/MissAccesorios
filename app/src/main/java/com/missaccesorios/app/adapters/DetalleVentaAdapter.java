package com.missaccesorios.app.adapters;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.missaccesorios.app.models.DetalleVenta;
import java.util.List;

public class DetalleVentaAdapter extends BaseAdapter {

    private final Context context;
    private final List<DetalleVenta> lista;
    private OnDeleteListener listener;

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public DetalleVentaAdapter(Context ctx, List<DetalleVenta> lista) {
        this.context = ctx;
        this.lista = lista;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.listener = listener;
    }

    @Override public int getCount() { return lista.size(); }
    @Override public DetalleVenta getItem(int pos) { return lista.get(pos); }
    @Override public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
        }
        DetalleVenta d = lista.get(pos);

        TextView text1 = convertView.findViewById(android.R.id.text1);
        TextView text2 = convertView.findViewById(android.R.id.text2);

        text1.setText(d.getNombreProducto() + "  x" + d.getCantidad()
                + "   →  $" + String.format("%.2f", d.getTotal()));
        text2.setText("Proveedor: " + d.getNombreProveedor()
                + "  |  $" + String.format("%.2f", d.getPrecio()) + " c/u");

        // Botón eliminar al hacer long press
        convertView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(pos);
            return true;
        });

        // Tinte rojo en long press para indicar que se puede eliminar
        convertView.setOnClickListener(v -> {
            Toast.makeText(context, "Mantén presionado para eliminar", Toast.LENGTH_SHORT).show();
        });

        return convertView;
    }
}
