package matos.csu.group3.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import matos.csu.group3.R;

public class LocationMapComponent extends RelativeLayout implements OnMapReadyCallback {
    private MapView mapView;
    private TextView tvLocationAddress;
    private GoogleMap googleMap;
    private String address;
    private LatLng location;
    private OnMapClickListener onMapClickListener;

    public interface OnMapClickListener {
        void onMapClicked(LatLng location);
    }

    public LocationMapComponent(Context context) {
        super(context);
        init(context);
    }

    public LocationMapComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_location_map, this, true);
        mapView = findViewById(R.id.mapView);
        tvLocationAddress = findViewById(R.id.tvLocationAddress);

        // Initialize map
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    public void setOnMapClickListener(OnMapClickListener listener) {
        this.onMapClickListener = listener;
    }

    public void setLocation(String address, LatLng location) {
        this.address = address;
        this.location = location;
        updateMap();
    }

    private void updateMap() {
        if (googleMap != null && location != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Vị trí chụp ảnh"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            tvLocationAddress.setText(address);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setAllGesturesEnabled(true);

        // Thêm sự kiện click cho map
        googleMap.setOnMapClickListener(latLng -> {
            if (onMapClickListener != null) {
                onMapClickListener.onMapClicked(latLng);
            }
        });

        if (location != null) {
            updateMap();
        }
    }

    // Các phương thức lifecycle
    public void onResume() {
        mapView.onResume();
    }

    public void onPause() {
        mapView.onPause();
    }

    public void onDestroy() {
        mapView.onDestroy();
    }

    public void onLowMemory() {
        mapView.onLowMemory();
    }

    public void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
    }
}