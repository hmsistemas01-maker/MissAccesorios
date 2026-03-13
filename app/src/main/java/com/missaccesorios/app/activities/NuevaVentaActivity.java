package com.missaccesorios.app.activities;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.missaccesorios.app.R;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.DetalleVenta;
import com.missaccesorios.app.models.Proveedor;
import com.missaccesorios.app.models.Venta;
import java.util.ArrayList;
import java.util.List;

public class NuevaVentaActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private Proveedor proveedorSeleccionado;
    private final List<DetalleVenta> detalles = new ArrayList<>();

    private EditText etProducto, etPrecio, etCantidad;
    private EditText etPagaCon, etComision, etReferencia;
    private TextView tvTotal, tvCambio, tvNeto;
    private LinearLayout layoutEfectivo, layoutTarjeta, layoutTransferencia;
    private RadioGroup rgMetodo;
    private LinearLayout llProductosLista;
    private LinearLayout llProveedoresFila1, llProveedoresFila2;
    private ScrollView scrollPrincipal;
    private Button btnActivo = null;

    // ─── Guardar estado al rotar ─────────────────────────────────────────────
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        ArrayList<String> filas = new ArrayList<>();
        for (DetalleVenta d : detalles) {
            filas.add(d.getIdProveedor() + "|" + d.getNombreProveedor() + "|"
                    + d.getNombreProducto() + "|" + d.getPrecio() + "|" + d.getCantidad());
        }
        out.putStringArrayList("detalles", filas);
        out.putInt("metodoId", rgMetodo.getCheckedRadioButtonId());
        out.putString("pagaCon",   etPagaCon.getText().toString());
        out.putString("comision",  etComision.getText().toString());
        out.putString("referencia",etReferencia.getText().toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_venta);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);

        scrollPrincipal     = findViewById(R.id.scrollPrincipal);
        etProducto          = findViewById(R.id.etProducto);
        etPrecio            = findViewById(R.id.etPrecio);
        etCantidad          = findViewById(R.id.etCantidad);
        etPagaCon           = findViewById(R.id.etPagaCon);
        etComision          = findViewById(R.id.etComision);
        etReferencia        = findViewById(R.id.etReferencia);
        tvTotal             = findViewById(R.id.tvTotal);
        tvCambio            = findViewById(R.id.tvCambio);
        tvNeto              = findViewById(R.id.tvNeto);
        layoutEfectivo      = findViewById(R.id.layoutEfectivo);
        layoutTarjeta       = findViewById(R.id.layoutTarjeta);
        layoutTransferencia = findViewById(R.id.layoutTransferencia);
        rgMetodo            = findViewById(R.id.rgMetodo);
        llProductosLista    = findViewById(R.id.llProductosLista);
        llProveedoresFila1  = findViewById(R.id.llProveedoresFila1);
        llProveedoresFila2  = findViewById(R.id.llProveedoresFila2);

        etCantidad.setText("1");
        etComision.setText("4.06");

        cargarProveedores();

        // Restaurar estado tras rotación
        if (savedInstanceState != null) {
            ArrayList<String> filas = savedInstanceState.getStringArrayList("detalles");
            if (filas != null) {
                for (String f : filas) {
                    String[] p = f.split("\\|");
                    if (p.length == 5) {
                        DetalleVenta d = new DetalleVenta(
                                Long.parseLong(p[0]), p[1], p[2],
                                Double.parseDouble(p[3]), Integer.parseInt(p[4]));
                        detalles.add(d);
                    }
                }
                reconstruirListaProductos();
            }
            int metodoId = savedInstanceState.getInt("metodoId", R.id.rbEfectivo);
            rgMetodo.check(metodoId);
            etPagaCon.setText(savedInstanceState.getString("pagaCon", ""));
            etComision.setText(savedInstanceState.getString("comision", "4.06"));
            etReferencia.setText(savedInstanceState.getString("referencia", ""));
            actualizarTotal();
        }

        rgMetodo.setOnCheckedChangeListener((g, id) -> actualizarVisibilidadPago());
        actualizarVisibilidadPago();

        etPagaCon.addTextChangedListener(new SimpleWatcher() {
            public void onTextChanged(CharSequence s, int st, int b, int c) { calcularCambio(); }
        });
        etComision.addTextChangedListener(new SimpleWatcher() {
            public void onTextChanged(CharSequence s, int st, int b, int c) { calcularNeto(); }
        });
    }

    // ─── PROVEEDORES: 2 filas dentro de un solo HorizontalScrollView ─────────
    private void cargarProveedores() {
        llProveedoresFila1.removeAllViews();
        llProveedoresFila2.removeAllViews();

        List<Proveedor> lista = db.obtenerProveedores();
        if (lista.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Sin proveedores registrados");
            tv.setTextColor(Color.parseColor("#AB47BC"));
            llProveedoresFila1.addView(tv);
            return;
        }

        int mitad = (int) Math.ceil(lista.size() / 2.0);
        for (int i = 0; i < lista.size(); i++) {
            Button btn = crearBotonProveedor(lista.get(i));
            if (i < mitad) llProveedoresFila1.addView(btn);
            else           llProveedoresFila2.addView(btn);
        }
    }

    private Button crearBotonProveedor(Proveedor p) {
        Button btn = new Button(this);
        btn.setText(p.getNombre());
        btn.setTag(p);
        btn.setAllCaps(false);
        btn.setTextSize(12f);
        btn.setSingleLine(true);
        btn.setEllipsize(TextUtils.TruncateAt.END);
        btn.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(110), dpToPx(42));
        params.setMargins(dpToPx(3), dpToPx(2), dpToPx(3), dpToPx(2));
        btn.setLayoutParams(params);
        btn.setPadding(dpToPx(6), 0, dpToPx(6), 0);
        aplicarEstiloBtn(btn, false);

        btn.setOnClickListener(v -> {
            if (btnActivo != null) aplicarEstiloBtn(btnActivo, false);
            proveedorSeleccionado = p;
            btnActivo = btn;
            aplicarEstiloBtn(btn, true);

            // Enfocar nombre producto y abrir teclado de texto
            etProducto.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etProducto, InputMethodManager.SHOW_IMPLICIT);
        });
        return btn;
    }

    private void aplicarEstiloBtn(Button btn, boolean activo) {
        if (activo) {
            btn.setBackgroundColor(Color.parseColor("#9C27B0"));
            btn.setTextColor(Color.WHITE);
            btn.setTypeface(null, Typeface.BOLD);
        } else {
            btn.setBackgroundColor(Color.parseColor("#F3E5F5"));
            btn.setTextColor(Color.parseColor("#6A0DAD"));
            btn.setTypeface(null, Typeface.NORMAL);
        }
    }

    // ─── AGREGAR PRODUCTO ────────────────────────────────────────────────────
    public void agregarProducto(View v) {
        if (proveedorSeleccionado == null) {
            Toast.makeText(this, "Selecciona un proveedor primero", Toast.LENGTH_SHORT).show();
            scrollPrincipal.smoothScrollTo(0, 0);
            return;
        }
        String nombre    = etProducto.getText().toString().trim();
        String precioStr = etPrecio.getText().toString().trim();
        String cantStr   = etCantidad.getText().toString().trim();

        if (TextUtils.isEmpty(nombre))    { etProducto.setError("Requerido"); return; }
        if (TextUtils.isEmpty(precioStr)) { etPrecio.setError("Requerido");   return; }

        double precio   = Double.parseDouble(precioStr);
        int    cantidad = TextUtils.isEmpty(cantStr) ? 1 : Integer.parseInt(cantStr);

        DetalleVenta d = new DetalleVenta(
                proveedorSeleccionado.getId(),
                proveedorSeleccionado.getNombre(),
                nombre, precio, cantidad);
        detalles.add(d);
        agregarFilaProducto(d, detalles.size() - 1);
        actualizarTotal();

        // Limpiar campos
        etProducto.setText(""); etPrecio.setText(""); etCantidad.setText("1");

        // Deseleccionar proveedor
        if (btnActivo != null) { aplicarEstiloBtn(btnActivo, false); btnActivo = null; }
        proveedorSeleccionado = null;

        // Cerrar teclado
        cerrarTeclado();

        // Scroll arriba para elegir siguiente proveedor
        scrollPrincipal.smoothScrollTo(0, 0);

        Toast.makeText(this, "✓ Producto agregado", Toast.LENGTH_SHORT).show();
    }

    private void cerrarTeclado() {
        View foco = getCurrentFocus();
        if (foco != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
            foco.clearFocus();
        }
    }

    // ─── FILA DE PRODUCTO con cantidad editable ───────────────────────────────
    private void agregarFilaProducto(DetalleVenta d, int index) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        fila.setGravity(Gravity.CENTER_VERTICAL);

        // Columna izquierda: nombre + proveedor
        LinearLayout colTexto = new LinearLayout(this);
        colTexto.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        colTexto.setLayoutParams(colParams);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(d.getNombreProducto() + "  →  $" + String.format("%.2f", d.getTotal()));
        tvNombre.setTextColor(Color.parseColor("#4A148C"));
        tvNombre.setTextSize(13f);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setTag("tvNombre_" + index); // para actualizar al cambiar cantidad

        TextView tvProv = new TextView(this);
        tvProv.setText(d.getNombreProveedor() + "  |  $" + String.format("%.2f", d.getPrecio()) + " c/u");
        tvProv.setTextColor(Color.parseColor("#AB47BC"));
        tvProv.setTextSize(11f);

        colTexto.addView(tvNombre);
        colTexto.addView(tvProv);

        // Columna derecha: controles cantidad  [−] [n] [+]
        LinearLayout colCant = new LinearLayout(this);
        colCant.setOrientation(LinearLayout.HORIZONTAL);
        colCant.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cantParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cantParams.setMarginStart(dpToPx(8));
        colCant.setLayoutParams(cantParams);

        Button btnMenos = new Button(this);
        btnMenos.setText("−");
        btnMenos.setTextSize(16f);
        btnMenos.setTypeface(null, Typeface.BOLD);
        btnMenos.setTextColor(Color.parseColor("#9C27B0"));
        btnMenos.setBackgroundColor(Color.parseColor("#F3E5F5"));
        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        btnP.setMargins(0, 0, dpToPx(2), 0);
        btnMenos.setLayoutParams(btnP);
        btnMenos.setPadding(0, 0, 0, 0);

        TextView tvCant = new TextView(this);
        tvCant.setText(String.valueOf(d.getCantidad()));
        tvCant.setTextSize(14f);
        tvCant.setTypeface(null, Typeface.BOLD);
        tvCant.setTextColor(Color.parseColor("#4A148C"));
        tvCant.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tvCantP = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(36));
        tvCant.setLayoutParams(tvCantP);

        Button btnMas = new Button(this);
        btnMas.setText("+");
        btnMas.setTextSize(16f);
        btnMas.setTypeface(null, Typeface.BOLD);
        btnMas.setTextColor(Color.WHITE);
        btnMas.setBackgroundColor(Color.parseColor("#9C27B0"));
        LinearLayout.LayoutParams btnPP = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        btnPP.setMarginStart(dpToPx(2));
        btnMas.setLayoutParams(btnPP);
        btnMas.setPadding(0, 0, 0, 0);

        // Lógica − y +
        btnMenos.setOnClickListener(vv -> {
            if (d.getCantidad() > 1) {
                d.setCantidad(d.getCantidad() - 1);
                tvCant.setText(String.valueOf(d.getCantidad()));
                tvNombre.setText(d.getNombreProducto() + "  →  $" + String.format("%.2f", d.getTotal()));
                actualizarTotal();
            }
        });
        btnMas.setOnClickListener(vv -> {
            d.setCantidad(d.getCantidad() + 1);
            tvCant.setText(String.valueOf(d.getCantidad()));
            tvNombre.setText(d.getNombreProducto() + "  →  $" + String.format("%.2f", d.getTotal()));
            actualizarTotal();
        });

        colCant.addView(btnMenos);
        colCant.addView(tvCant);
        colCant.addView(btnMas);

        fila.addView(colTexto);
        fila.addView(colCant);

        // Separador
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(fila);
        View sep = new View(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sep.setLayoutParams(sp);
        sep.setBackgroundColor(Color.parseColor("#F3E5F5"));
        wrapper.addView(sep);

        // Long press = eliminar
        wrapper.setOnLongClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar producto")
                    .setMessage("¿Eliminar \"" + d.getNombreProducto() + "\"?")
                    .setPositiveButton("Eliminar", (dialog, w) -> {
                        detalles.remove(index);
                        reconstruirListaProductos();
                        actualizarTotal();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        });

        llProductosLista.addView(wrapper);
    }

    private void reconstruirListaProductos() {
        llProductosLista.removeAllViews();
        for (int i = 0; i < detalles.size(); i++) {
            agregarFilaProducto(detalles.get(i), i);
        }
    }

    // ─── TOTALES ─────────────────────────────────────────────────────────────
    private void actualizarTotal() {
        tvTotal.setText("Total: $" + String.format("%.2f", getTotal()));
        calcularCambio();
        calcularNeto();
    }

    private double getTotal() {
        double t = 0;
        for (DetalleVenta d : detalles) t += d.getTotal();
        return t;
    }

    private void calcularCambio() {
        String s = etPagaCon.getText().toString().trim();
        double cambio = TextUtils.isEmpty(s) ? 0 : Math.max(0, Double.parseDouble(s) - getTotal());
        tvCambio.setText("Cambio: $" + String.format("%.2f", cambio));
    }

    private void calcularNeto() {
        String s = etComision.getText().toString().trim();
        double neto = TextUtils.isEmpty(s) ? getTotal() : getTotal() * (1 - Double.parseDouble(s) / 100);
        tvNeto.setText("Neto a recibir: $" + String.format("%.2f", neto));
    }

    private void actualizarVisibilidadPago() {
        int id = rgMetodo.getCheckedRadioButtonId();
        layoutEfectivo.setVisibility(id == R.id.rbEfectivo ? View.VISIBLE : View.GONE);
        layoutTarjeta.setVisibility(id == R.id.rbTarjeta ? View.VISIBLE : View.GONE);
        layoutTransferencia.setVisibility(id == R.id.rbTransferencia ? View.VISIBLE : View.GONE);
    }

    // ─── FINALIZAR VENTA ─────────────────────────────────────────────────────
    public void finalizarVenta(View v) {
        if (detalles.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un producto", Toast.LENGTH_SHORT).show();
            return;
        }
        int checked = rgMetodo.getCheckedRadioButtonId();
        String metodo = checked == R.id.rbEfectivo ? "efectivo"
                : checked == R.id.rbTarjeta ? "tarjeta" : "transferencia";

        double total = getTotal(), pagaCon = 0, cambio = 0, comision = 0, totalRecibir = total;
        String referencia = "";

        if (metodo.equals("efectivo")) {
            String s = etPagaCon.getText().toString().trim();
            if (TextUtils.isEmpty(s)) {
                etPagaCon.setError("Ingresa con cuánto paga el cliente");
                etPagaCon.requestFocus();
                return;
            }
            pagaCon = Double.parseDouble(s);
            if (pagaCon < total) {
                etPagaCon.setError("Monto menor al total ($" + String.format("%.2f", total) + ")");
                return;
            }
            cambio = pagaCon - total;
        } else if (metodo.equals("tarjeta")) {
            String s = etComision.getText().toString().trim();
            comision = TextUtils.isEmpty(s) ? 4.06 : Double.parseDouble(s);
            totalRecibir = total * (1 - comision / 100);
        } else {
            referencia = etReferencia.getText().toString().trim();
        }

        Venta venta = new Venta();
        venta.setFecha(System.currentTimeMillis());
        venta.setTotal(total);
        venta.setMetodoPago(metodo);
        venta.setPagaCon(pagaCon);
        venta.setCambio(cambio);
        venta.setComision(comision);
        venta.setTotalRecibir(totalRecibir);
        venta.setReferencia(referencia);
        venta.setDetalles(new ArrayList<>(detalles));

        db.insertarVenta(venta);
        Toast.makeText(this, "✓ Venta Registrada", Toast.LENGTH_SHORT).show();
        resetForm();
    }

    private void resetForm() {
        detalles.clear();
        llProductosLista.removeAllViews();
        tvTotal.setText("Total: $0.00");
        tvCambio.setText("Cambio: $0.00");
        tvNeto.setText("Neto a recibir: $0.00");
        etPagaCon.setText(""); etComision.setText("4.06"); etReferencia.setText("");
        etProducto.setText(""); etPrecio.setText(""); etCantidad.setText("1");
        rgMetodo.check(R.id.rbEfectivo);
        if (btnActivo != null) { aplicarEstiloBtn(btnActivo, false); btnActivo = null; }
        proveedorSeleccionado = null;
        cerrarTeclado();
        scrollPrincipal.smoothScrollTo(0, 0);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    abstract static class SimpleWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        public void afterTextChanged(Editable s) {}
    }
}
