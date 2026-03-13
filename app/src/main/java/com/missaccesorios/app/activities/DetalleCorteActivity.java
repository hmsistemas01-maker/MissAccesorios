package com.missaccesorios.app.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.missaccesorios.app.R;
import com.missaccesorios.app.adapters.HistorialVentasAdapter;
import com.missaccesorios.app.database.DatabaseHelper;
import com.missaccesorios.app.models.Venta;
import java.util.List;

public class DetalleCorteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_corte);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Detalle de Corte");

        long inicio = getIntent().getLongExtra("inicio", 0);
        long fin    = getIntent().getLongExtra("fin", 0);

        DatabaseHelper db = new DatabaseHelper(this);
        List<Venta> ventas = db.obtenerVentasDelCorte(inicio, fin);

        ListView lv = findViewById(R.id.lvVentasCorte);
        lv.setAdapter(new HistorialVentasAdapter(this, ventas));
    }
}
