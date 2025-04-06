package matos.csu.group3.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import matos.csu.group3.R;
import matos.csu.group3.ui.fragment.LocationMapComponent;

public class PhotoDetailsActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_LOCATION_PERMISSION = 101;
    private String imagePath;
    private TextView tvExifData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_details);

        // Initialize views
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView imageView = findViewById(R.id.imageView);
        tvExifData = findViewById(R.id.tvExifData);

        // Set back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Get image path from intent
        imagePath = getIntent().getStringExtra("image_path");

        if (imagePath != null) {
            // Display image immediately
            imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));

            // Check and request permission if needed
            checkAndRequestMediaLocationPermission();
        } else {
            Toast.makeText(this, "No image path provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkAndRequestMediaLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, read EXIF data
            readExifData();
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION},
                    REQUEST_MEDIA_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_LOCATION_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readExifData();
            } else {
                Toast.makeText(this,
                        "Location permission denied. GPS data may be unavailable.",
                        Toast.LENGTH_SHORT).show();
                readExifDataWithoutLocation();
            }
        }
    }

    private void readExifData() {
        if (imagePath == null || imagePath.isEmpty()) {
            tvExifData.setText("Invalid image path");
            return;
        }

        try {
            ExifInterface exifInterface;

            // Xử lý cho cả URI content và file path thông thường
            if (imagePath.startsWith("content://")) {
                // Sử dụng ContentResolver cho URI content
                try {
                    InputStream inputStream = getContentResolver().openInputStream(Uri.parse(imagePath));
                    if (inputStream == null) {
                        tvExifData.setText("Cannot open image stream");
                        return;
                    }
                    exifInterface = new ExifInterface(inputStream);
                    inputStream.close();
                } catch (SecurityException e) {
                    // Fallback nếu không có quyền truy cập
                    Log.e("EXIF", "Security exception, using fallback", e);
                    exifInterface = tryFallbackMethods();
                    if (exifInterface == null) {
                        tvExifData.setText("Permission denied for content URI");
                        return;
                    }
                }
            } else {
                // Xử lý file path thông thường
                File file = new File(imagePath);
                if (!file.exists()) {
                    tvExifData.setText("Image file not found");
                    return;
                }
                exifInterface = new ExifInterface(file.getAbsolutePath());
            }

            displayExifData(exifInterface);
        } catch (IOException e) {
            Log.e("EXIF", "Error reading EXIF", e);
            tvExifData.setText("Cannot read image metadata");
        } catch (Exception e) {
            Log.e("EXIF", "Unexpected error", e);
            tvExifData.setText("Error processing image");
        }
    }

    private ExifInterface tryFallbackMethods() {
        try {
            // Thử phương pháp fallback 1: Sử dụng FileDescriptor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(Uri.parse(imagePath), "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    return new ExifInterface(fd);
                }
            }

            // Thử phương pháp fallback 2: Copy file tạm
            File tempFile = createTempFileFromUri(Uri.parse(imagePath));
            if (tempFile != null) {
                return new ExifInterface(tempFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e("EXIF", "Fallback failed", e);
        }
        return null;
    }

    private File createTempFileFromUri(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = File.createTempFile("temp_img_", ".jpg", getCacheDir());
            outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            return tempFile;
        } catch (Exception e) {
            Log.e("EXIF", "Error creating temp file", e);
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e("EXIF", "Error closing streams", e);
            }
        }
    }

    private void readExifDataWithoutLocation() {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            StringBuilder exifDetails = new StringBuilder();
            addNonLocationExifData(exifInterface, exifDetails);
            tvExifData.setText(exifDetails.toString());
        } catch (IOException e) {
            e.printStackTrace();
            tvExifData.setText("Error reading EXIF data");
        }
    }

    private void displayExifData(ExifInterface exifInterface) {
        StringBuilder exifDetails = new StringBuilder();

        // Add GPS info if we have permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            addGpsInfo(exifInterface, exifDetails);
        }

        // Add other EXIF data
        addNonLocationExifData(exifInterface, exifDetails);

        tvExifData.setText(exifDetails.toString());
    }

    private void addGpsInfo(ExifInterface exifInterface, StringBuilder sb) {
        String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        String latitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        String longitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

        if (latitude != null && longitude != null) {
            try {
                // Chuyển đổi tọa độ GPS sang dạng số
                double lat = convertGpsToDecimal(latitude, latitudeRef);
                double lng = convertGpsToDecimal(longitude, longitudeRef);

                // Lấy địa chỉ từ tọa độ
                String address = getAddressFromLocation(lat, lng);

                if (address != null && !address.isEmpty()) {
                    sb.append("Location Address:\n");
                    sb.append(address).append("\n\n");
                } else {
                    sb.append("GPS Coordinates:\n");
                    sb.append("Latitude: ").append(lat).append("° ").append(latitudeRef).append("\n");
                    sb.append("Longitude: ").append(lng).append("° ").append(longitudeRef).append("\n\n");
                }

                // Hiển thị lên bản đồ
                showOnMap(lat, lng);

            } catch (Exception e) {
                e.printStackTrace();
                sb.append("Invalid GPS data format\n\n");
            }
        }

    }
    private String getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Lấy địa chỉ đầy đủ
                StringBuilder addressBuilder = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressBuilder.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        addressBuilder.append(", ");
                    }
                }
                return addressBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Geocoder", "Error getting address from location", e);
        }
        return null;
    }
    private double convertGpsToDecimal(String gpsCoordinate, String ref) {
        String[] parts = gpsCoordinate.split(",");

        // Chuyển đổi độ
        String[] degParts = parts[0].split("/");
        double degrees = Double.parseDouble(degParts[0]) / Double.parseDouble(degParts[1]);

        // Chuyển đổi phút
        String[] minParts = parts[1].split("/");
        double minutes = Double.parseDouble(minParts[0]) / Double.parseDouble(minParts[1]);

        // Chuyển đổi giây
        String[] secParts = parts[2].split("/");
        double seconds = Double.parseDouble(secParts[0]) / Double.parseDouble(secParts[1]);

        // Tính toán giá trị thập phân
        double result = degrees + (minutes / 60.0) + (seconds / 3600.0);

        // Xử lý hướng (N/S/E/W)
        if (ref.equalsIgnoreCase("S") || ref.equalsIgnoreCase("W")) {
            result = -result;
        }

        return result;
    }

    private void showOnMap(double latitude, double longitude) {
        // Kiểm tra nếu mapComponent đã được khởi tạo
        View mapContainer = findViewById(R.id.mapContainer);
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

    private void addNonLocationExifData(ExifInterface exifInterface, StringBuilder sb) {
        // Các trường EXIF tiêu chuẩn
        String dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
        String width = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        String height = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

        // Các trường EXIF của camera (sẽ null với ảnh chụp màn hình)
        String make = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        String model = exifInterface.getAttribute(ExifInterface.TAG_MODEL);

        if (make == null && model == null) {
            // Lấy ngày sửa đổi file nếu không có EXIF datetime
            if (dateTime == null) {
                dateTime = getFileLastModifiedTime();
            }

            if (dateTime != null) {
                sb.append("Date taken: ").append(formatDateTime(dateTime)).append("\n");
            }
            if (width != null && height != null) {
                sb.append("Resolution: ").append(width).append("x").append(height).append("\n");
            }
        } else {
            // Hiển thị đầy đủ EXIF cho ảnh chụp từ camera
            if (dateTime != null) sb.append("Date Taken: ").append(formatDateTime(dateTime)).append("\n");
            if (make != null) sb.append("Camera Make: ").append(make).append("\n");
            if (model != null) sb.append("Camera Model: ").append(model).append("\n");
            if (width != null && height != null) {
                sb.append("Resolution: ").append(width).append("x").append(height).append("\n");
            }
        }
    }

    private String getFileLastModifiedTime() {
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                long lastModified = file.lastModified();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
                return sdf.format(new Date(lastModified));
            }
        } catch (Exception e) {
            Log.e("EXIF", "Error getting file time", e);
        }
        return null;
    }

    private String formatDateTime(String dateTime) {
        try {
            // Chuẩn hóa định dạng ngày tháng
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return targetFormat.format(originalFormat.parse(dateTime));
        } catch (Exception e) {
            return dateTime; // Giữ nguyên nếu không parse được
        }
    }
}