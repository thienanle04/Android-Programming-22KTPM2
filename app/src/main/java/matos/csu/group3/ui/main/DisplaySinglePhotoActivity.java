package matos.csu.group3.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import matos.csu.group3.R;
import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.AlbumDao;
import matos.csu.group3.data.local.dao.PhotoAlbumDao;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.editor.CropAndRotateActivity;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class DisplaySinglePhotoActivity extends AppCompatActivity {

    private ImageView imgSoloPhoto, favoriteBtn;
    private TextView txtSoloMsg;
    private Button btnSoloBack, btnEdit, btnShare;
    private PhotoViewModel photoViewModel;
    private PhotoEntity photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_single_photo);
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        // Ánh xạ các view
        imgSoloPhoto = findViewById(R.id.imgSoloPhoto);
        txtSoloMsg = findViewById(R.id.txtSoloMsg);
        btnSoloBack = findViewById(R.id.btnSoloBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnShare = findViewById(R.id.btnShare);
        favoriteBtn = findViewById(R.id.imgFavourite);

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("photoEntity")) {
            photo = (PhotoEntity) intent.getSerializableExtra("photoEntity");

            // Cập nhật giao diện với dữ liệu ảnh
            if (photo != null) {
                txtSoloMsg.setText(photo.getDateTaken());
                Glide.with(this)
                        .load(new File(photo.getFilePath()))
                        .into(imgSoloPhoto);
                if(photo.isFavorite()){
                    favoriteBtn.setImageResource(R.drawable.ic_star_filled);
                }
                else {
                    favoriteBtn.setImageResource(R.drawable.ic_star_outline);
                }
            }
        }

        // Nút chỉnh sửa ảnh
        btnEdit.setOnClickListener(v -> {
            if (photo != null) {
                Intent cropIntent = new Intent(DisplaySinglePhotoActivity.this, CropAndRotateActivity.class);
                cropIntent.setData(Uri.fromFile(new File(photo.getFilePath())));
                cropIntent.putExtra("photoEntity", photo);
                startActivity(cropIntent);
                if(photo.isFavorite()){
                    favoriteBtn.setImageResource(R.drawable.ic_star_outline);
                }
                else {
                    favoriteBtn.setImageResource(R.drawable.ic_star_filled);
                }
            }
        });

        // Nút quay lại
        btnSoloBack.setOnClickListener(v -> finish());

        // Nút chia sẻ ảnh
        btnShare.setOnClickListener(v -> {
            if (photo != null) {
                // Create a list with the single photo
                List<PhotoEntity> photos = new ArrayList<>();
                photos.add(photo);

                // Call the sharePhotosViaMessenger method
                PhotoListOfAlbumActivity.sharePhotosViaPackage(DisplaySinglePhotoActivity.this, photos);
            } else {
                Toast.makeText(this, "Không có ảnh để chia sẻ", Toast.LENGTH_SHORT).show();
            }
        });

        favoriteBtn.setOnClickListener(v -> {
            boolean isFavorite = !photo.isFavorite();
            photo.setFavorite(isFavorite);

            // Cập nhật hình ảnh nút yêu thích
            if (isFavorite) {
                favoriteBtn.setImageResource(R.drawable.ic_star_filled);
            } else {
                favoriteBtn.setImageResource(R.drawable.ic_star_outline);
            }

            photoViewModel.updateFavoriteStatus(photo, isFavorite);
        });

    }
}
