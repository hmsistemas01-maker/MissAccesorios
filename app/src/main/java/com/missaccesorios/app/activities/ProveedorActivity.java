package com.missaccesorios.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.Proveedor;
import java.util.List;

public class ProveedorActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private EditText etNombre, etTelefono;
    private Button   btnGuardar, btnCancelar;
    private TextView tvFormTitulo, tvContProveedores;
    private ListView listView;

    private List<Proveedor> lista;
    private long editandoId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedor);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db               = new DatabaseHelper(this);
        etNombre         = findViewById(R.id.etNombre);
        etTelefono       = findViewById(R.id.etTelefono);
        btnGuardar       = findViewById(R.id.btnGuardar);
        btnCancelar      = findViewById(R.id.btnCancelarEdicion);
        tvFormTitulo     = findViewById(R.id.tvFormTitulo);
        tvContProveedores= findViewById(R.id.tvContProveedores);
        listView         = findViewById(R.id.listProveedores);

        cargarLista();

        btnCancelar.setOnClickListener(v -> limpiarFormulario());
    }

    // ── Guardar / actualizar ─────────────────────────────────────────────────
    public void guardarProveedor(View v) {
        String nombre    = etNombre.getText().toString().trim();
        String telefono  = etTelefono.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Requerido");
            return;
        }
        if (editandoId >= 0) {
            db.actualizarProveedor(editandoId, nombre, telefono);
            Toast.makeText(this, "Proveedor actualizado", Toast.LENGTH_SHORT).show();
        } else {
            db.insertarProveedor(nombre, telefono);
            Toast.makeText(this, "Proveedor agregado", Toast.LENGTH_SHORT).show();
        }
        limpiarFormulario();
        cargarLista();
    }

    private void limpiarFormulario() {
        editandoId = -1;
        etNombre.setText("");
        etTelefono.setText("");
        tvFormTitulo.setText("NUEVO PROVEEDOR");
        btnCancelar.setVisibility(View.GONE);
        // El peso del botón guardar vuelve a ocupar todo
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnGuardar.setLayoutParams(lp);
    }

    // ── Lista de proveedores ─────────────────────────────────────────────────
    private void cargarLista() {
        lista = db.obtenerProveedores();
        tvContProveedores.setText(lista.size() + " registrados");
        listView.setAdapter(new ProveedorItemAdapter());
    }

    // ── Adapter inline ───────────────────────────────────────────────────────
    private class ProveedorItemAdapter extends BaseAdapter {
        @Override public int getCount()              { return lista.size(); }
        @Override public Object getItem(int pos)     { return lista.get(pos); }
        @Override public long getItemId(int pos)     { return lista.get(pos).getId(); }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ProveedorActivity.this)
                        .inflate(R.layout.item_proveedor, parent, false);
            }
            Proveedor p = lista.get(pos);

            TextView tvInicial  = convertView.findViewById(R.id.tvInicialProveedor);
            TextView tvNombre   = convertView.findViewById(R.id.tvNombreProveedor);
            TextView tvTelefono = convertView.findViewById(R.id.tvTelefonoProveedor);
            TextView tvEditar   = convertView.findViewById(R.id.tvEditarProveedor);

            // Inicial del nombre
            String nombre = p.getNombre() != null ? p.getNombre() : "?";
            tvInicial.setText(nombre.length() > 0
                    ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");
            tvNombre.setText(nombre);

            String tel = p.getTelefono();
            tvTelefono.setText((tel != null && !tel.isEmpty()) ? "📞 " + tel : "Sin teléfono");

            tvEditar.setOnClickListener(v -> activarEdicion(p));

            return convertView;
        }
    }

    private void activarEdicion(Proveedor p) {
        editandoId = p.getId();
        etNombre.setText(p.getNombre());
        etTelefono.setText(p.getTelefono() != null ? p.getTelefono() : "");
        tvFormTitulo.setText("EDITAR PROVEEDOR");
        btnCancelar.setVisibility(View.VISIBLE);

        // Ajustar pesos de botones
        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpCancel.setMarginEnd(8);
        btnCancelar.setLayoutParams(lpCancel);
        LinearLayout.LayoutParams lpGuardar = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnGuardar.setLayoutParams(lpGuardar);

        // Scroll al formulario
        etNombre.requestFocus();
    }
}