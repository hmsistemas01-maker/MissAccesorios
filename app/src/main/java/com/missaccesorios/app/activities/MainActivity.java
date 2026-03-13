package com.missaccesorios.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.missaccesorios.app.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void irNuevaVenta(View v) {
        startActivity(new Intent(this, NuevaVentaActivity.class));
    }

    public void irHistorialVentas(View v) {
        startActivity(new Intent(this, HistorialVentasActivity.class));
    }

    public void irCorte(View v) {
        startActivity(new Intent(this, CorteCajaActivity.class));
    }

    public void irHistorialCortes(View v) {
        startActivity(new Intent(this, HistorialCortesActivity.class));
    }

    public void irProveedores(View v) {
        startActivity(new Intent(this, ProveedorActivity.class));
    }

    public void irReportes(View v) {
        startActivity(new Intent(this, ReportesActivity.class));
    }

    public void irPagos(View v) {
        startActivity(new Intent(this, PagosProveedorActivity.class));
    }
}