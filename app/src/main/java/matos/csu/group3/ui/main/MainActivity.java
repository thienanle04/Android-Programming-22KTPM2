package matos.csu.group3.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Observer;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.ui.editor.CropActivity;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class MainActivity extends FragmentActivity implements PhotoAdapter.OnItemClickListener {

    private PhotoViewModel photoViewModel;
    private RecyclerView photoRecyclerView;
    private PhotoAdapter photoAdapter;
    private List<PhotoEntity> allPhotos; // Store the full list of photos

    // Register the permission request launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, load photos
                    loadPhotos();
                } else {
                    // Permission denied, show message or handle accordingly
//                    Toast.makeText(this, "Permission denied! Cannot load photos.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();

        // Check for permissions using ActivityResultContracts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                loadPhotos(); // Permission granted, load photos
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // For older versions, handle `READ_EXTERNAL_STORAGE` permission
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadPhotos();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onItemClick(PhotoEntity photo) {
        // Handle item click events here
        // For example, open a detailed view of the photo

        Intent cropIntent = new Intent(this, CropActivity.class);

        Uri photoUri = Uri.parse(photo.getFilePath());
        cropIntent.setData(photoUri);

        cropIntent.putExtra("photoEntity", (CharSequence) photo);

        startActivity(cropIntent);
    }

    // Method to filter photos based on search query
    private void filterPhotos(String query) {
        if (allPhotos == null) return;

        List<PhotoEntity> filteredPhotos = new ArrayList<>();
        for (PhotoEntity photo : allPhotos) {
            // Check if the photo name contains the search query (case-insensitive)
            if (photo.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredPhotos.add(photo);
            }
        }

        // Update the adapter with the filtered list
        photoAdapter.setPhotos(filteredPhotos);

        // Khởi tạo BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Xử lý sự kiện khi chọn một mục trong BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_photos) {
                    // Xử lý khi chọn "Ảnh"
                    return true;
                } else if (id == R.id.nav_albums) {
                    // Xử lý khi chọn "Album"
                    return true;
                } else if (id == R.id.nav_menu) {
                    // Khi nhấn vào "Menu", hiển thị BottomSheetDialogFragment
                    BottomExtendedMenu popupMenu = new BottomExtendedMenu();
                    popupMenu.show(getSupportFragmentManager(), "PopupMenuDialogFragment");

                    return false;
                }

                return false;
            }
        });
    }


    // Method to load photos from the MediaStore
    private void loadPhotos() {
        // Fetch photos after the permission is granted
        // You can put the previous logic here to load the photos from MediaStore
        if (photoViewModel != null) {
            photoViewModel.refreshPhotos();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Kiểm tra hướng màn hình mới
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Thiết bị đang ở chế độ ngang
            Toast.makeText(this, "Landscape Mode", Toast.LENGTH_SHORT).show();
            GridLayoutManager layoutManager = new GridLayoutManager(this, 6); // 4 ảnh mỗi hàng khi xoay ngang
            photoRecyclerView.setLayoutManager(layoutManager);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Thiết bị đang ở chế độ dọc
            Toast.makeText(this, "Portrait Mode", Toast.LENGTH_SHORT).show();
            GridLayoutManager layoutManager = new GridLayoutManager(this, 3); // 3 ảnh mỗi hàng khi xoay dọc
            photoRecyclerView.setLayoutManager(layoutManager);
        }
    }
    private void showBigScreen(PhotoEntity photo) {
        // Hiển thị layout chứa ảnh lớn
        setContentView(R.layout.solo_picture);

        // Ánh xạ các view trong layout solo_picture
        TextView txtSoloMsg = findViewById(R.id.txtSoloMsg);
        ImageView imgSoloPhoto = findViewById(R.id.imgSoloPhoto);
        Button btnSoloBack = findViewById(R.id.btnSoloBack);

        // Đặt caption và ảnh lớn
        txtSoloMsg.setText(photo.getName());
        Glide.with(this) // "this" là Context (Activity hoặc Fragment)
                .load(new File(photo.getFilePath())) // Load ảnh từ đường dẫn tệp
//                .placeholder(R.drawable.loading_image_placeholder) // Use a placeholder image
                .into(imgSoloPhoto);

        // Xử lý sự kiện nút "GO BACK"
        btnSoloBack.setOnClickListener(v -> {
            // Quay lại layout chính (activity_main)
            setContentView(R.layout.activity_main);

            // Khởi tạo lại các view và RecyclerView
            initializeViews();
        });
    }
    private void initializeViews() {
        // Khởi tạo lại RecyclerView và các view khác
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        // Handle item click events here
        photoAdapter = new PhotoAdapter(new ArrayList<>(), this::showBigScreen);
        // Kiểm tra hướng màn hình
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Thiết bị đang ở chế độ ngang
            GridLayoutManager layoutManager = new GridLayoutManager(this, 6); // 4 ảnh mỗi hàng khi xoay ngang
            photoRecyclerView.setLayoutManager(layoutManager);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Thiết bị đang ở chế độ dọc
            GridLayoutManager layoutManager = new GridLayoutManager(this, 3); // 3 ảnh mỗi hàng khi xoay dọc
            photoRecyclerView.setLayoutManager(layoutManager);
        }
        photoRecyclerView.setAdapter(photoAdapter);

        // Khởi tạo lại ViewModel và quan sát dữ liệu
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.getAllPhotos().observe(this, new Observer<List<PhotoEntity>>() {
            @Override
            public void onChanged(List<PhotoEntity> photoEntities) {
                // Update the full list of photos
                allPhotos = photoEntities;
                // Update the adapter with the latest photos
                photoAdapter.setPhotos(photoEntities);
            }
        });

        // Khởi tạo lại SearchView và các sự kiện liên quan
        EditText searchEditText = findViewById(R.id.search_src_text);
        ImageView searchIcon = findViewById(R.id.search_mag_icon);
        searchIcon.setOnClickListener(v -> {
            String query = searchEditText.getText().toString();
            filterPhotos(query);
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString();
            filterPhotos(query);
            return true;
        });

        // Khởi tạo BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Xử lý sự kiện khi chọn một mục trong BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_photos) {
                    // Xử lý khi chọn "Ảnh"
                    return true;
                } else if (id == R.id.nav_albums) {
                    // Xử lý khi chọn "Album"
                    return true;
                } else if (id == R.id.nav_menu) {
                    // Khi nhấn vào "Menu", hiển thị BottomSheetDialogFragment
                    BottomExtendedMenu popupMenu = new BottomExtendedMenu();
                    popupMenu.show(getSupportFragmentManager(), "PopupMenuDialogFragment");

                    return false;
                }

                return false;
            }
        });

    }
}