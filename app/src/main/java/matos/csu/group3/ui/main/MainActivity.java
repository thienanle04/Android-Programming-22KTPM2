package matos.csu.group3.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.SearchView;

import androidx.fragment.app.FragmentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Observer;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.adapter.PhotoAdapter;
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
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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


        // Initialize RecyclerView and adapter
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        photoAdapter = new PhotoAdapter(new ArrayList<>(), photo -> {
            // Handle item click events here
        });
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3); // 3 ảnh mỗi hàng
        photoRecyclerView.setLayoutManager(layoutManager);
        photoRecyclerView.setAdapter(photoAdapter);

        // Initialize ViewModel
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);

        // Observe LiveData from ViewModel
        photoViewModel.getAllPhotos().observe(this, new Observer<List<PhotoEntity>>() {
            @Override
            public void onChanged(List<PhotoEntity> photoEntities) {
                // Update the full list of photos
                allPhotos = photoEntities;
                // Update the adapter with the latest photos
                photoAdapter.setPhotos(photoEntities);
            }
        });

        // Initialize SearchView
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Handle search query submission (optional)
                filterPhotos(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter photos as the user types
                filterPhotos(newText);
                return true;
            }
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

    @Override
    public void onItemClick(PhotoEntity photo) {
        // Handle item click events here
        // For example, open a detailed view of the photo
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
    }

    // Method to load photos from the MediaStore
    private void loadPhotos() {
        // Fetch photos after the permission is granted
        // You can put the previous logic here to load the photos from MediaStore
    }
}