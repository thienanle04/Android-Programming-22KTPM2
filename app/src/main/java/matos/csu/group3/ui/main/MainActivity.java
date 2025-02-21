package matos.csu.group3.ui.main;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.SearchView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class MainActivity extends ComponentActivity implements PhotoAdapter.OnItemClickListener {

    private PhotoViewModel photoViewModel;
    private PhotoAdapter photoAdapter;
    private List<PhotoEntity> allPhotos; // Store the full list of photos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize RecyclerView and adapter
        RecyclerView photoRecyclerView = findViewById(R.id.photoRecyclerView);
        photoAdapter = new PhotoAdapter(new ArrayList<>(), this);
        photoRecyclerView.setAdapter(photoAdapter);

        // Initialize ViewModel
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);

        // Observe LiveData from ViewModel
        photoViewModel.getAllPhotos().observe(this, photos -> {
            allPhotos = photos; // Store the full list of photos
            photoAdapter.setPhotos(photos); // Update adapter with all photos initially
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
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
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
                    // Khi nhấn vào "Menu", hiển thị PopupMenu
                    showPopupMenu(bottomNavigationView); // Sử dụng BottomNavigationView làm anchor view
                    return true;
                }

                return false;
            }
        });    }

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
    private void showPopupMenu(View view) {
        // Tạo PopupMenu và liên kết với anchor view (BottomNavigationView)
        PopupMenu popupMenu = new PopupMenu(this, view);

        // Nạp menu từ tệp XML (sub_nav_menu.xml)
        popupMenu.getMenuInflater().inflate(R.menu.sub_nav_menu, popupMenu.getMenu());

        // Xử lý sự kiện khi chọn một mục trong PopupMenu
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

//                if (id == R.id.sub_option1) {
//                    // Xử lý khi chọn "Sub Item 1"
//                    return true;
//                } else if (id == R.id.sub_option2) {
//                    // Xử lý khi chọn "Sub Item 2"
//                    return true;
//                }

                return false;
            }
        });

        // Hiển thị PopupMenu
        popupMenu.show();
    }
}