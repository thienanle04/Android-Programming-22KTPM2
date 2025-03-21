package matos.csu.group3.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.editor.CropAndRotateActivity;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class DisplaySinglePhotoActivity extends AppCompatActivity {

    private ImageView imgSoloPhoto;
    private TextView txtSoloMsg;
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnSoloBack;
    Menu menu;
    MenuItem favouriteItem;
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
        bottomNavigationView = findViewById(R.id.bottomNavigationSinglePhotoView);
        resetBottomNavSelection(bottomNavigationView);

        Menu menu = bottomNavigationView.getMenu();
        MenuItem favouriteItem = menu.findItem(R.id.action_favorite);
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
                if (photo.isFavorite()) {
                    favouriteItem.setIcon(R.drawable.ic_favorite_selected); // Icon khi được yêu thích
                } else {
                    favouriteItem.setIcon(R.drawable.ic_favorite); // Icon mặc định
                }
            }
        }
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                if (photo != null) {
                    // Create a list with the single photo
                    List<PhotoEntity> photos = new ArrayList<>();
                    photos.add(photo);

                    // Call the sharePhotosViaMessenger method
                    PhotoListOfAlbumActivity.sharePhotosViaPackage(DisplaySinglePhotoActivity.this, photos);
                } else {
                    Toast.makeText(this, "Không có ảnh để chia sẻ", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (item.getItemId() == R.id.action_edit) {
                if (photo != null) {
                    Intent cropIntent = new Intent(DisplaySinglePhotoActivity.this, CropAndRotateActivity.class);
                    cropIntent.setData(Uri.fromFile(new File(photo.getFilePath())));
                    cropIntent.putExtra("photoEntity", photo);
                    startActivity(cropIntent);
                    if(photo.isFavorite()){
                        favouriteItem.setIcon(R.drawable.ic_favorite_selected); // Icon khi được yêu thích
                    } else {
                        favouriteItem.setIcon(R.drawable.ic_favorite); // Icon mặc định
                    }
                }
                return true;
            } else if (item.getItemId() == R.id.action_favorite) {
                if (photo != null) {
                    boolean isFavorite = !photo.isFavorite();
                    photo.setFavorite(isFavorite);

                    if (isFavorite) {
                        favouriteItem.setIcon(R.drawable.ic_favorite_selected); // Icon khi được yêu thích
                    } else {
                        favouriteItem.setIcon(R.drawable.ic_favorite); // Icon mặc định
                    }

                    photoViewModel.updateFavoriteStatus(photo, isFavorite);
                }
                return true;
            } else if (item.getItemId() == R.id.action_delete) {

                return true;
            }

            return false;
        });

        // Nút quay lại
        btnSoloBack.setOnClickListener(v -> finish());
    }
    private void resetBottomNavSelection(BottomNavigationView bottomNavView) {
        bottomNavView.getMenu().setGroupCheckable(0, true, false);
        // Bỏ chọn tất cả item ban đầu
        for (int i = 0; i < bottomNavView.getMenu().size(); i++) {
            bottomNavView.getMenu().getItem(i).setChecked(false);
        }

        // Bật lại chế độ chỉ cho chọn 1 item
        bottomNavView.getMenu().setGroupCheckable(0, true, true);
    }
}
