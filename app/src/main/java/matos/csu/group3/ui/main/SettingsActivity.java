package matos.csu.group3.ui.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import matos.csu.group3.R;
import matos.csu.group3.ui.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity implements  SettingsFragment.OnGridUpdateListener {
    @Override
    public void onGridUpdateRequested() {
        // Chuyển request đến MainActivity nếu cần
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("UPDATE_GRID", true);
        startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings); // Add a toolbar

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable Back Button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Load SettingsFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .commit();
        }
    }

    // Handle Back Button in Toolbar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
