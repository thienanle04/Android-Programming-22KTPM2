package matos.csu.group3.ui.main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.Hashtags;
import matos.csu.group3.ui.adapter.HashtagAdapter;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.repository.AlbumRepository;
import matos.csu.group3.ui.adapter.PhotoPagerAdapter;
import matos.csu.group3.ui.editor.CropAndRotateActivity;
import matos.csu.group3.utils.PhotoCache;
import matos.csu.group3.viewmodel.PhotoViewModel;
import matos.csu.group3.repository.PhotoRepository;

public class DisplaySinglePhotoActivity extends AppCompatActivity implements HashtagAdapter.OnHashtagRemoveListener {

    private ViewPager2 viewPager;
    private TextView txtSoloMsg;
    private Button btnSoloBack, btnEdit, btnShare, btnAddHashtag;
    private RecyclerView recyclerViewHashtags;
    private HashtagAdapter hashtagAdapter;

    private PhotoEntity photo;
    private PhotoDao photoDao;
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnSoloBack;
    private ImageButton btnToggleVisibility;
    private PhotoViewModel photoViewModel;
    private PhotoPagerAdapter photoPagerAdapter;
    private List<Integer> photoIds;
    private int currentPosition;
    private PhotoRepository photoRepository;
    private PhotoEntity currentPhoto;
    private AlbumRepository albumRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_single_photo);

        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);

        // Ánh xạ các view
        viewPager = findViewById(R.id.viewPager);
        txtSoloMsg = findViewById(R.id.txtSoloMsg);
        btnSoloBack = findViewById(R.id.btnSoloBack);
        btnToggleVisibility = findViewById(R.id.btnToggleVisibility);

        bottomNavigationView = findViewById(R.id.bottomNavigationSinglePhotoView);

        photoRepository = new PhotoRepository(getApplication());

        albumRepository = new AlbumRepository(getApplication());

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        boolean isTrashAlbum = false;
        if (intent != null) {
            photoIds = PhotoCache.getInstance().getPhotoListIds();
            currentPosition = intent.getIntExtra("currentPosition", 0);
            isTrashAlbum = intent.getBooleanExtra("isTrashAlbum", false);
            loadCurrentPhoto();
        }

        photoPagerAdapter = new PhotoPagerAdapter(photoIds, photoViewModel, this);
        viewPager.setAdapter(photoPagerAdapter);
        viewPager.setCurrentItem(currentPosition, false);

        // Lắng nghe sự kiện khi người dùng vuốt qua ảnh khác
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                loadCurrentPhoto();
            }
        });

        // Xử lý sự kiện BottomNavigationView
        setupBottomNavigationMenu(isTrashAlbum);
        resetBottomNavSelection(bottomNavigationView);

        // Nút quay lại
        btnSoloBack.setOnClickListener(v -> finish());

        btnToggleVisibility.setOnClickListener(v -> {
            boolean isHidden = !currentPhoto.isHidden();
            currentPhoto.setHidden(isHidden);
            if(isHidden){
                btnToggleVisibility.setImageResource(R.drawable.ic_eye_slash);
            } else {
                btnToggleVisibility.setImageResource(R.drawable.ic_eye);
            }
            photoViewModel.updateHiddenStatus(currentPhoto, isHidden);
            finish();
        });
    }
    private void loadCurrentPhoto() {
        if (isInvalidPosition()) return;

        photoViewModel.getPhotoById(photoIds.get(currentPosition)).removeObservers(this);
        photoViewModel.getPhotoById(photoIds.get(currentPosition)).observe(this, new Observer<PhotoEntity>() {
            @Override
            public void onChanged(PhotoEntity photo) {
                currentPhoto = photo;
                if (photo != null) {
                    updateCaption(photo);
                    updateFavoriteIcon(photo.isFavorite());
                    if(currentPhoto.isHidden()){
                        btnToggleVisibility.setImageResource(R.drawable.ic_eye_slash);
                    } else {
                        btnToggleVisibility.setImageResource(R.drawable.ic_eye);
                    }
                }
            }
        });
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        bottomNavigationView.post(() -> { // Đảm bảo chạy trên UI thread
            MenuItem favoriteItem = bottomNavigationView.getMenu().findItem(R.id.action_favorite);
            if (favoriteItem != null) {
                favoriteItem.setIcon(isFavorite ?
                        R.drawable.ic_favorite_selected :
                        R.drawable.ic_favorite);

                // Quan trọng: Yêu cầu BottomNavigationView vẽ lại
                bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
            }
        });
    }
    private boolean isInvalidPosition() {
        return photoIds == null || photoIds.isEmpty() || currentPosition < 0 || currentPosition >= photoIds.size();
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

    //Handle delete photo
    private void deletePhoto(PhotoEntity photo) {
        Log.d("DisplaySinglePhoto", "Deleting photo: " + photo.getFilePath());
        List<PhotoEntity> photosToDelete = new ArrayList<>();
        photosToDelete.add(photo);

        // Di chuyển ảnh vào thùng rác
        photoRepository.movePhotosToTrash(photosToDelete);
        photoRepository.schedulePermanentDeletion(photosToDelete, this);



        // Xác định vị trí ảnh bị xóa
        int removedPosition = photoIds.indexOf(photo.getId());
        if (removedPosition == -1) {
            Log.e("DisplaySinglePhoto", "Photo ID not found in photoIds: " + photo.getId());
            return;
        }

        // Xóa ảnh khỏi danh sách
        photoIds.remove(removedPosition);
        photoPagerAdapter.notifyItemRemoved(removedPosition); // Tối ưu hơn notifyDataSetChanged()

        // Cập nhật vị trí hiện tại
        if (!photoIds.isEmpty()) {
            // Nếu xóa ảnh cuối cùng, chuyển đến ảnh trước đó
            if (removedPosition >= photoIds.size()) {
                currentPosition = photoIds.size() - 1;
            }
            // Nếu xóa ảnh đang xem, giữ nguyên vị trí (ViewPager tự động chuyển sang ảnh kế tiếp)
            else {
                currentPosition = removedPosition;
            }

            viewPager.setAdapter(photoPagerAdapter);
            viewPager.setCurrentItem(currentPosition, false);
            loadCurrentPhoto(); // Cập nhật thông tin ảnh mới
        }

        Toast.makeText(this, "Đã chuyển ảnh vào thùng rác", Toast.LENGTH_SHORT).show();

        // Đóng Activity nếu không còn ảnh
        if (photoIds.isEmpty()) {
            Log.d("DisplaySinglePhoto", "No photos left, finishing activity");
            finish();
        }
    }

    private void showDeleteConfirmationDialog(PhotoEntity photo) {
        Log.d("DisplaySinglePhoto", "Showing delete confirmation dialog for photo: " + photo.getFilePath());
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa ảnh này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Log.d("DisplaySinglePhoto", "User confirmed deletion");
                    deletePhoto(photo);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    Log.d("DisplaySinglePhoto", "User canceled deletion");
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private void showManageStorageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cần cấp quyền truy cập ")
                .setMessage("Ứng dụng cần được cấp quyền truy cập quản lí tất cả các file, vui lòng cấp quyền truy cập trong settings.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    Toast.makeText(this, "Không được cấp quyền truy cập, một vài tính năng có thể không hoạt động.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void resetBottomNavSelection(BottomNavigationView bottomNavView) {
        bottomNavView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavView.getMenu().size(); i++) {
            bottomNavView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavView.getMenu().setGroupCheckable(0, true, true);
    }

    private void setupBottomNavigationMenu(boolean isTrashAlbum) {
        // Inflate the appropriate menu
        bottomNavigationView.getMenu().clear();
        bottomNavigationView.inflateMenu(isTrashAlbum ?
                R.menu.bottom_nav_menu_trash_album :
                R.menu.bottom_nav_menu_single_photo);

        // Set up the item selected listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
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
                if (currentPhoto != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        showManageStorageDialog();
                        return true;
                    }
                    Log.d("Delete action", "Deleting photo: ");
                    showDeleteConfirmationDialog(currentPhoto);
                    return true;
                }
                return true;
            } else if (item.getItemId() == R.id.action_restore) {
                if (currentPhoto != null) {
                    restorePhoto(currentPhoto);
                    return true;
                }
                return true;
            }
            else if (item.getItemId() == R.id.action_delete2) {
                if (currentPhoto != null) {
                    showPermanentDeleteConfirmation(currentPhoto);
                    return true;
                }
                return true;
            }
            return false;
        });
    }

    private void restorePhoto(PhotoEntity photo) {
        // Show loading indicator if needed
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        albumRepository.getTrashAlbum(trashAlbum -> {
            progressDialog.dismiss();

            if (trashAlbum == null) {
                Toast.makeText(this, "Không tìm thấy album thùng rác", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Khôi phục ảnh")
                    .setMessage("Bạn có chắc chắn muốn khôi phục ảnh này?")
                    .setPositiveButton("Khôi phục", (dialog, which) -> {
                        // Show loading again for the restore operation
                        progressDialog.show();

                        photoRepository.restoreFromTrash(
                                Collections.singletonList(photo),
                                trashAlbum.getId(),
                                () -> {
                                    // This callback runs when restore is complete
                                    progressDialog.dismiss();
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Đã khôi phục ảnh", Toast.LENGTH_SHORT).show();

                                        int removedPosition = photoIds.indexOf(photo.getId());
                                        if (removedPosition == -1) {
                                            Log.e("DisplaySinglePhoto", "Photo ID not found in photoIds: " + photo.getId());
                                            return;
                                        }

                                        // Delete from list
                                        photoIds.remove(removedPosition);
                                        photoPagerAdapter.notifyItemRemoved(removedPosition); // Tối ưu hơn notifyDataSetChanged()

                                        // Update Position
                                        if (!photoIds.isEmpty()) {
                                            // Move to previous photo if the last photo is deleted
                                            if (removedPosition >= photoIds.size()) {
                                                currentPosition = photoIds.size() - 1;
                                            }

                                            viewPager.setAdapter(photoPagerAdapter);
                                            viewPager.setCurrentItem(currentPosition, false);
                                            loadCurrentPhoto(); // Refresh the current photo
                                        }

                                        if (photoIds.isEmpty()) {
                                            finish();
                                        }
                                    });
                                }
                        );
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void showPermanentDeleteConfirmation(PhotoEntity photo) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa vĩnh viễn")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn ảnh này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    photoRepository.deletePhotoById(photo.getId());
                    Toast.makeText(this, "Đã xóa ảnh vĩnh viễn", Toast.LENGTH_SHORT).show();

                    // Update UI
                    int removedPosition = photoIds.indexOf(photo.getId());
                    photoIds.remove(Integer.valueOf(photo.getId()));
                    photoPagerAdapter.notifyDataSetChanged();

                    // Update current photo
                    if (!photoIds.isEmpty()) {
                        if (removedPosition >= photoIds.size()) {
                            currentPosition = photoIds.size() - 1;
                        }
                        loadCurrentPhoto(); // Refresh the current photo
                    }

                    if (photoIds.isEmpty()) {
                        finish();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
