package com.missaccesorios.app.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// Redirige al nuevo módulo de Pagos a Proveedores
public class HistorialPagosProveedorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, PagosProveedorActivity.class));
        finish();
    }
}