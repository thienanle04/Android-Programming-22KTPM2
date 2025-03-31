package matos.csu.group3.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.ui.adapter.PhotoSelectionAdapter;
import matos.csu.group3.utils.PhotoCache;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class HiddenAlbumActivity extends AppCompatActivity implements PhotoSelectionAdapter.OnPhotoSelectedListener, PhotoSelectionAdapter.OnPhotoClickListener {
    private PhotoViewModel photoViewModel;
    private RecyclerView recyclerView;
    private PhotoSelectionAdapter photoAdapter;
    private List<PhotoEntity> allPhotos;
    private ImageButton btnSoloBack;
    @Override
    public void onPhotoSelected(PhotoEntity photo, boolean isSelected) {
        photo.setSelected(isSelected);
    }
    @Override
    public void onPhotoClick(PhotoEntity photo){
        showBigScreen(photo);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_album);

        btnSoloBack = findViewById(R.id.btnSoloBack);
        recyclerView = findViewById(R.id.photoRecyclerViewHidden);

        // Set up back button click listener
        btnSoloBack.setOnClickListener(v -> onBackPressed());

        allPhotos = new ArrayList<>();
        photoAdapter = new PhotoSelectionAdapter(allPhotos, this, false);
        photoAdapter.setOnPhotoClickListener(this::showBigScreen);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(photoAdapter);

        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.getHiddenPhotos().observe(this, photos -> {
            allPhotos.clear();
            allPhotos.addAll(photos);
            photoAdapter.updateData(allPhotos); // Make sure your adapter has this method
            photoAdapter.notifyDataSetChanged();
        });

    }

    private void showBigScreen(PhotoEntity photo) {
        // Hiển thị ảnh lớn
        PhotoCache.getInstance().setPhotoList(allPhotos);
        Intent intent = new Intent(this, DisplaySinglePhotoActivity.class);
        intent.putExtra("photoEntity", photo);
        intent.putExtra("currentPosition", allPhotos.indexOf(photo));
        intent.putExtra("isTrashAlbum", false);
        Toast.makeText(this, "showBigScreen", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }
}