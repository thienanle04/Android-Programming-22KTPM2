package matos.csu.group3.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import matos.csu.group3.R;
import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.repository.AlbumRepository;
import matos.csu.group3.repository.PhotoRepository;
import matos.csu.group3.ui.adapter.AlbumAdapter;
import matos.csu.group3.ui.adapter.PhotoSelectionAdapter;
import matos.csu.group3.utils.PhotoCache;
import matos.csu.group3.viewmodel.AlbumViewModel;

public class PhotoListOfAlbumActivity extends AppCompatActivity implements PhotoSelectionAdapter.OnPhotoSelectedListener, PhotoSelectionAdapter.OnPhotoClickListener {

    private RecyclerView photoRecyclerView;
    private PhotoSelectionAdapter photoAdapter;
    private  PhotoDao photoDao;
    private AlbumEntity album;
    private PhotoRepository photoRepository;
    private AlbumRepository albumRepository;
    private  AlbumViewModel albumViewModel;
    private List<PhotoEntity> allPhotos;
    private int currentAlbumId;
    private LinearLayout topNavigationBar, topNavigationSelectionBar;
    private ImageButton btnBack, btnBackSelection, btnLock;
    private TextView tvSelectedCount, tvAlbum;
    private ImageButton btnSelectAll;
    private BottomNavigationView bottomNavigationView;
    private ConstraintLayout constraintLayout;
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
        setContentView(R.layout.activity_photo_list);

        constraintLayout = findViewById(R.id.activity_photo_list);
        albumRepository = new AlbumRepository(getApplication());
        albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        photoRecyclerView = findViewById(R.id.photoRecyclerView);

        topNavigationBar = findViewById(R.id.topNavigationBar);
        topNavigationSelectionBar = findViewById(R.id.topNavigationSelectionBar);

        btnBackSelection = findViewById(R.id.btnBackSelection);
        btnBack = findViewById(R.id.btnBack);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnLock = findViewById(R.id.btnToggleLock);

        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvAlbum = findViewById(R.id.tvAlbum);

        btnBackSelection.setOnClickListener(v -> {
            photoAdapter.setSelectionMode(false); // Tắt chế độ chọn ảnh
            topNavigationSelectionBar.setVisibility(View.GONE); // Ẩn top navigation selection bar
            topNavigationBar.setVisibility(View.VISIBLE);
            updateRecyclerViewConstraints(false);
            bottomNavigationView.setVisibility(View.GONE);
        });
        btnBack.setOnClickListener(v -> onBackPressed());
        btnSelectAll.setOnClickListener(v -> {
            boolean isAllSelected = photoAdapter.isAllSelected();
            photoAdapter.selectAll(!isAllSelected); // Đảo ngược trạng thái chọn
        });
        btnLock.setOnClickListener(v -> {
            boolean isLocked = !album.isLocked();
            albumRepository.toggleAlbumLock(currentAlbumId, isLocked);
            if(isLocked){
                btnLock.setImageResource(R.drawable.ic_lock);
            } else {
                btnLock.setImageResource(R.drawable.ic_lock_open);
            }
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ALBUM_ID", currentAlbumId);
            resultIntent.putExtra("IS_LOCKED", isLocked);
            setResult(RESULT_OK, resultIntent);
        });

        photoRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        currentAlbumId = intent.getIntExtra("ALBUM_ID", -1);

        if (currentAlbumId != -1) {
            albumRepository.getAlbumById(currentAlbumId).observe(this, new Observer<AlbumEntity>() {
                @Override
                public void onChanged(AlbumEntity _album) {
                    if (_album != null) {
                        album = _album;
                        if(album.getName() != null){
                            String albumName = album.getName();
                            if (!albumName.isEmpty()) {
                                // Viết hoa chữ cái đầu và giữ nguyên các chữ cái khác
                                String capitalized = albumName.substring(0, 1).toUpperCase()
                                        + albumName.substring(1).toLowerCase();
                                tvAlbum.setText(capitalized);
                            } else {
                                tvAlbum.setText(albumName);
                            }
                        }
                        if(album.isLocked()){
                            btnLock.setImageResource(R.drawable.ic_lock);
                        } else {
                            btnLock.setImageResource(R.drawable.ic_lock_open);
                        }
                    } else {
                        tvAlbum.setText(""); // Hoặc giá trị mặc định khi album null
                    }

                }
            });

            AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database-name").allowMainThreadQueries().build();
            photoDao = db.photoDao();
            photoRepository = new PhotoRepository(getApplication());
            loadPhotos(currentAlbumId);
        } else {
            Toast.makeText(this, "Invalid album ID", Toast.LENGTH_SHORT).show();
        }

        FloatingActionButton btnAdd = findViewById(R.id.addPhotoToButton);
        btnAdd.setOnClickListener(v -> showPhotoSelectionDialog());
        // Ánh xạ view
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        resetBottomNavSelection(bottomNavigationView);
        bottomNavigationView.setVisibility(View.GONE);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                // Handle delete action
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());
                if (!selectedPhotos.isEmpty()) {
                    showDeleteConfirmationDialog();
                }
                return true;
            } else if (item.getItemId() == R.id.action_add) {
                // Handle add to album action
                showAlbumSelectionDialog();
                return true;
            } else if (item.getItemId() == R.id.action_share) {
                // Handle share action
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());
                if (!selectedPhotos.isEmpty()) {
                    sharePhotosViaPackage(this, selectedPhotos);
                } else {
                    Toast.makeText(this, "Vui lòng chọn ít nhất một ảnh để chia sẻ", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
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
    private void loadPhotos(int albumId) {
        allPhotos = new ArrayList<>();
        photoAdapter = new PhotoSelectionAdapter(allPhotos, this, false);
        photoAdapter.setOnLongPressListener(() -> {
            topNavigationSelectionBar.setVisibility(View.VISIBLE); // Hiển thị top navigation bar
            topNavigationBar.setVisibility(View.GONE);
            updateRecyclerViewConstraints(true);
            updateSelectedCount();
            bottomNavigationView.setVisibility(View.VISIBLE); // Hiển thị BottomNavigationView
        });
        photoAdapter.setOnPhotoClickListener(this::showBigScreen);
        photoAdapter.setOnSelectionChangeListener(this::updateSelectedCount);
        // Kiểm tra hướng màn hình
        int orientation = getResources().getConfiguration().orientation;

        // Thiết lập số cột dựa trên hướng màn hình
        int spanCount = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 6 : 3;

        // Khởi tạo GridLayoutManager cho album
        GridLayoutManager photoLayoutManager = new GridLayoutManager(this, spanCount);

        // Thiết lập LayoutManager mặc định cho RecyclerView (ảnh)
        photoRecyclerView.setLayoutManager(photoLayoutManager);
        photoRecyclerView.setAdapter(photoAdapter);

        photoRepository.refreshPhotos();
        photoRepository.getPhotosByAlbumId(albumId).observe(this, photos -> {
            if (photos != null) {
                allPhotos.clear();
                allPhotos.addAll(photos);
                photoAdapter.notifyDataSetChanged();
            }
        });
    }
    private void updateSelectedCount() {
        int selectedCount = photoAdapter.getSelectedCount();
        if(selectedCount == 0){
            tvSelectedCount.setText("Chọn Mục");
        }
        else tvSelectedCount.setText("Đã chọn " + selectedCount + " mục");
    }

    private void showPhotoSelectionDialog() {
        // Lấy danh sách ảnh không có trong album hiện tại
        List<PhotoEntity> photosNotInAlbum = new ArrayList<>();
        try {
            photosNotInAlbum = photoRepository.getPhotosNotInAlbum(currentAlbumId);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return;
        }

        // Tạo BottomSheetDialog
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_photos, null);
        dialog.setContentView(dialogView);

        // Thiết lập chiều cao tối đa cho dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Tùy chỉnh behavior để có thể kéo xuống mọi lúc
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setHideable(true);
        }

        // Ánh xạ view
        ImageButton btnBack = dialogView.findViewById(R.id.btnBack);
        TextView tvSelectedCount = dialogView.findViewById(R.id.tvSelectedCount);
        ImageButton btnSelectAll = dialogView.findViewById(R.id.btnSelectAll);
        RecyclerView photoRecyclerView = dialogView.findViewById(R.id.photoRecyclerView);
        Button btnAddSelected = dialogView.findViewById(R.id.btnAddSelected);

        // Thiết lập RecyclerView
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        photoRecyclerView.setLayoutManager(layoutManager);

        // Tạo adapter
        List<PhotoEntity> finalPhotosNotInAlbum = photosNotInAlbum;
        PhotoSelectionAdapter adapter = new PhotoSelectionAdapter(photosNotInAlbum, (photo, isSelected) -> {
            // Cập nhật số lượng ảnh đã chọn
            int selectedCount = (int) finalPhotosNotInAlbum.stream().filter(PhotoEntity::isSelected).count();
            tvSelectedCount.setText(selectedCount > 0 ?
                    "Đã chọn " + selectedCount + " mục" : "Chọn mục");
        }, true);

        photoRecyclerView.setAdapter(adapter);

        // Xử lý nút Back
        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Xử lý nút Chọn Tất Cả

        btnSelectAll.setOnClickListener(v -> {
            boolean isAllSelected = adapter.isAllSelected();
            adapter.selectAll(!isAllSelected);
            int selectedCount = (int) finalPhotosNotInAlbum.stream().filter(PhotoEntity::isSelected).count();
            tvSelectedCount.setText(selectedCount > 0 ?
                    "Đã chọn " + selectedCount + " mục" : "Chọn mục");
        });

        // Xử lý nút Thêm Ảnh Đã Chọn
        btnAddSelected.setOnClickListener(v -> {
            List<PhotoEntity> selectedPhotos = finalPhotosNotInAlbum.stream()
                    .filter(PhotoEntity::isSelected)
                    .collect(Collectors.toList());

            if (!selectedPhotos.isEmpty()) {
                addPhotosToAlbum(selectedPhotos);
                selectedPhotos.forEach(photo -> photo.setSelected(false));
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Hiển thị dialog
        dialog.show();
    }
    private void showAlbumSelectionDialog() {
        // Tạo BottomSheetDialog
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_album, null);
        dialog.setContentView(dialogView);

        // Thiết lập chiều cao tối đa cho dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Ánh xạ view
        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.albumRecyclerView);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Thiết lập GridLayoutManager với 3 cột
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        dialogRecyclerView.setLayoutManager(gridLayoutManager);

        // Khởi tạo adapter
        AlbumAdapter adapter = new AlbumAdapter(new ArrayList<>(), album -> {
            // Xử lý sự kiện click item
        }, albumViewModel, this);

        // Lấy dữ liệu album
        AlbumViewModel albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        albumViewModel.getAllAlbums().observe(this, albumEntities -> {
            List<AlbumEntity> filteredAlbums = albumEntities.stream()
                    .filter(album -> album.getId() != currentAlbumId)
                    .collect(Collectors.toList());
            adapter.updateData(filteredAlbums);
        });

        adapter.setSelectionMode(true);
        dialogRecyclerView.setAdapter(adapter);

        // Xử lý nút đóng
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Xử lý nút xác nhận
        btnConfirm.setOnClickListener(v -> {
            List<AlbumEntity> selectedAlbums = adapter.getSelectedAlbums();
            if (!selectedAlbums.isEmpty()) {
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());

                for (AlbumEntity selectedAlbum : selectedAlbums) {
                    addPhotosToAlbum(selectedPhotos, selectedAlbum.getId());
                }
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng chọn ít nhất một album", Toast.LENGTH_SHORT).show();
            }
        });

        // Hiển thị dialog
        dialog.show();

        // Tùy chỉnh behavior để có thể kéo xuống mọi lúc
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setHideable(true);
        }
    }

    private void addPhotosToAlbum(List<PhotoEntity> photos) {
        photoRepository.addPhotosToAlbum(currentAlbumId, photos);
        loadPhotos(currentAlbumId);
    }
    private void addPhotosToAlbum(List<PhotoEntity> photos, int albumID){
        photoRepository.addPhotosToAlbum(albumID, photos);
        loadPhotos(currentAlbumId);
    }
    private void showDeleteConfirmationDialog() {
        // Tạo AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa") // Tiêu đề dialog
                .setMessage("Bạn có chắc chắn muốn xóa các ảnh đã chọn không?") // Nội dung dialog
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Xử lý khi người dùng nhấn "Xóa"
                    deleteSelectedPhotos();
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // Xử lý khi người dùng nhấn "Hủy"
                    dialog.dismiss(); // Đóng dialog
                })
                .setIcon(android.R.drawable.ic_dialog_alert) // Biểu tượng cảnh báo
                .show(); // Hiển thị dialog
    }

    private void deleteSelectedPhotos() {
        List<PhotoEntity> selectedPhotos = allPhotos.stream()
                .filter(PhotoEntity::isSelected)
                .collect(Collectors.toList());

        // Kiểm tra nếu có ảnh được chọn
        if (!selectedPhotos.isEmpty()) {
            // Xóa các ảnh đã chọn khỏi album
            for (PhotoEntity photo : selectedPhotos) {
                albumRepository.deletePhotoFromAlbum(photo.getId(), currentAlbumId);
            }

            // Đặt lại trạng thái isSelected của các ảnh về false
            selectedPhotos.forEach(photo -> photo.setSelected(false));

            // Cập nhật RecyclerView (nếu cần)
            photoAdapter.notifyDataSetChanged();
        }
    }
    private void showBigScreen(PhotoEntity photo) {
        PhotoCache.getInstance().setPhotoList(allPhotos);
        albumRepository.getNameByAlbumId(currentAlbumId).observe(this, albumName -> {
            Intent intent = new Intent(this, DisplaySinglePhotoActivity.class);
            intent.putExtra("photoEntity", photo);
            intent.putExtra("currentPosition", allPhotos.indexOf(photo));
            intent.putExtra("isTrashAlbum", "Trash".equals(albumName));
            startActivity(intent);
        });
    }

    public static void sharePhotosViaPackage(Context context, List<PhotoEntity> photos) {
        // Create a list of URIs for the selected photos
        ArrayList<Uri> photoUris = new ArrayList<>();
        for (PhotoEntity photo : photos) {
            File photoFile = new File(photo.getFilePath());

            // Check if the file exists and can be read
            if (!photoFile.exists() || !photoFile.canRead()) {
                continue; // Skip this file
            }

            // Generate a URI for the file
            try {
                Uri photoUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", photoFile);
                photoUris.add(photoUri);
            } catch (IllegalArgumentException e) {
                continue; // Skip this file
            }
        }

        // Check if any URIs were generated
        if (photoUris.isEmpty()) {
            Toast.makeText(context, "Không có ảnh hợp lệ để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create an intent to share the photos
        Intent shareIntent;
        if (photoUris.size() == 1) {
            // Use ACTION_SEND for a single file
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, photoUris.get(0));
        } else {
            // Use ACTION_SEND_MULTIPLE for multiple files
            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("image/*");
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoUris);
        }

        // Grant temporary read permission to the receiving app
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Fallback: Open any app that can handle the share intent
        shareIntent.setPackage(null); // Remove the package restriction
        if (shareIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh"));
        } else {
            Toast.makeText(context, "Không có ứng dụng hỗ trợ chia sẻ ảnh", Toast.LENGTH_SHORT).show();
        }
    }
    void updateRecyclerViewConstraints(boolean isSeletionView) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout); // Sao chép các ràng buộc hiện tại

        if (isSeletionView) {
            // Ràng buộc với customSearchView
            constraintSet.connect(
                    R.id.photoRecyclerView,
                    ConstraintSet.TOP,
                    R.id.topNavigationSelectionBar,
                    ConstraintSet.BOTTOM
            );
        } else {
            // Ràng buộc với topNavigationBar
            constraintSet.connect(
                    R.id.photoRecyclerView,
                    ConstraintSet.TOP,
                    R.id.topNavigationBar,
                    ConstraintSet.BOTTOM
            );
        }

        // Áp dụng các thay đổi
        constraintSet.applyTo(constraintLayout);
    }
}
