package matos.csu.group3.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.adapter.PhotoPagerAdapter;
import matos.csu.group3.ui.editor.CropAndRotateActivity;
import matos.csu.group3.utils.PhotoCache;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class DisplaySinglePhotoActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView txtSoloMsg;
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnSoloBack;
    private PhotoViewModel photoViewModel;
    private PhotoPagerAdapter photoPagerAdapter;
    private List<PhotoEntity> photos = new ArrayList<>();
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_single_photo);

        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);

        // Ánh xạ các view
        viewPager = findViewById(R.id.viewPager);
        txtSoloMsg = findViewById(R.id.txtSoloMsg);
        btnSoloBack = findViewById(R.id.btnSoloBack);
        bottomNavigationView = findViewById(R.id.bottomNavigationSinglePhotoView);
        resetBottomNavSelection(bottomNavigationView);

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("photoEntity")) {
            PhotoEntity photo = (PhotoEntity) intent.getSerializableExtra("photoEntity");
            photos = PhotoCache.getInstance().getPhotoList();
            currentPosition = intent.getIntExtra("currentPosition", 0);
            if (photo != null) {
                updateCaption(photo);
            }
        }

        // Khởi tạo Adapter
        photoPagerAdapter = new PhotoPagerAdapter(photos);
        viewPager.setAdapter(photoPagerAdapter);

        viewPager.setCurrentItem(currentPosition, false);

        // Lắng nghe sự kiện khi người dùng vuốt qua ảnh khác
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updateCaption(photos.get(position));
            }
        });

        // Xử lý sự kiện BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(item -> {
            PhotoEntity currentPhoto = photos.get(currentPosition);
            if (item.getItemId() == R.id.action_share) {
                if (currentPhoto != null) {
                    List<PhotoEntity> photosToShare = new ArrayList<>();
                    photosToShare.add(currentPhoto);
                    PhotoListOfAlbumActivity.sharePhotosViaPackage(DisplaySinglePhotoActivity.this, photosToShare);
                } else {
                    Toast.makeText(this, "Không có ảnh để chia sẻ", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (item.getItemId() == R.id.action_edit) {
                if (currentPhoto != null) {
                    Intent cropIntent = new Intent(DisplaySinglePhotoActivity.this, CropAndRotateActivity.class);
                    cropIntent.setData(Uri.fromFile(new File(currentPhoto.getFilePath())));
                    cropIntent.putExtra("photoEntity", currentPhoto);
                    startActivity(cropIntent);
                }
                return true;
            } else if (item.getItemId() == R.id.action_favorite) {
                if (currentPhoto != null) {
                    boolean isFavorite = !currentPhoto.isFavorite();
                    currentPhoto.setFavorite(isFavorite);
                    photoViewModel.updateFavoriteStatus(currentPhoto, isFavorite);
                    updateFavoriteIcon(item, isFavorite);
                }
                return true;
            } else if (item.getItemId() == R.id.action_delete) {
                // Xử lý xóa ảnh
                return true;
            }
            return false;
        });

        // Nút quay lại
        btnSoloBack.setOnClickListener(v -> finish());
    }

    private void updateCaption(PhotoEntity photo) {
        txtSoloMsg.setText(photo.getDateTaken());
    }

    private void updateFavoriteIcon(MenuItem item, boolean isFavorite) {
        if (isFavorite) {
            item.setIcon(R.drawable.ic_favorite_selected);
        } else {
            item.setIcon(R.drawable.ic_favorite);
        }
    }

    private void resetBottomNavSelection(BottomNavigationView bottomNavView) {
        bottomNavView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavView.getMenu().size(); i++) {
            bottomNavView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavView.getMenu().setGroupCheckable(0, true, true);
    }
}
