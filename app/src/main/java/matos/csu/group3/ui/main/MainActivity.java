package matos.csu.group3.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;
import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.ui.editor.CropActivity;
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
    }
}