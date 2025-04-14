package matos.csu.group3.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

import matos.csu.group3.R;
import matos.csu.group3.ui.fragment.LocationMapComponent;

public class MapActivity extends AppCompatActivity {
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();
        if (intent != null) {
            latitude = intent.getDoubleExtra("LATITUDE", 0.0); // 0.0 là giá trị mặc định nếu không có
            longitude = intent.getDoubleExtra("LONGITUDE", 0.0);
            showOnMap(latitude, longitude);
            Toast.makeText(this, latitude +":" + latitude, Toast.LENGTH_SHORT).show();
        }
        // Xử lý nút back
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Đóng Activity hiện tại
            }
        });

    }
    private void showOnMap(double latitude, double longitude) {
        // Kiểm tra nếu mapComponent đã được khởi tạo
        View mapContainer = findViewById(R.id.mapViewContainer);
        if (mapContainer == null) {
            Log.e("GPS", "Map container not found");
            return;
        }

        LocationMapComponent mapComponent = mapContainer.findViewById(R.id.mapView);
        if (mapComponent == null) {
            // Nếu chưa có, tạo mới và thêm vào layout
            mapComponent = new LocationMapComponent(this);
            ((ViewGroup) mapContainer).addView(mapComponent);
        }

        // Tạo địa chỉ từ tọa độ (có thể sử dụng Geocoder nếu cần)
        String address = String.format(Locale.getDefault(),
                "Lat: %.6f, Long: %.6f", latitude, longitude);

        // Hiển thị lên bản đồ
        mapComponent.setLocation(address, new LatLng(latitude, longitude));
    }
}