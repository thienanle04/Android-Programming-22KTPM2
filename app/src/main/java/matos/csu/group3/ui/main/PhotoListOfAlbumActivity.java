package matos.csu.group3.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private LinearLayout topNavigationBar;
    private ImageButton btnBack;
    private TextView tvSelectedCount;
    private ImageButton btnSelectAll;
    private BottomNavigationView bottomNavigationView;
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

        albumRepository = new AlbumRepository(getApplication());
        albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        topNavigationBar = findViewById(R.id.topNavigationBar);
        btnBack = findViewById(R.id.btnBack);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnBack.setOnClickListener(v -> {
            photoAdapter.setSelectionMode(false); // Tắt chế độ chọn ảnh
            topNavigationBar.setVisibility(View.GONE); // Ẩn top navigation bar
            bottomNavigationView.setVisibility(View.GONE);
        });
        btnSelectAll.setOnClickListener(v -> {
            boolean isAllSelected = photoAdapter.isAllSelected();
            photoAdapter.selectAll(!isAllSelected); // Đảo ngược trạng thái chọn
        });
        photoRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        currentAlbumId = intent.getIntExtra("ALBUM_ID", -1);

        if (currentAlbumId != -1) {

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
            topNavigationBar.setVisibility(View.VISIBLE); // Hiển thị top navigation bar
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
                for (int i = 0; i < allPhotos.size() - 1; i++) {
                    for (int j = i + 1; j < allPhotos.size(); j++) {
                        PhotoEntity photo1 = allPhotos.get(i);
                        PhotoEntity photo2 = allPhotos.get(j);

                        // So sánh năm, tháng, ngày
                        if (isNewer(photo2.getDateTaken(), photo1.getDateTaken())) {
                            // Hoán đổi vị trí nếu photo2 mới hơn photo1
                            Collections.swap(allPhotos, i, j);
                        }
                    }
                }
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
        List<PhotoEntity> photosNotInAlbum = new ArrayList<>();
        try {
            photosNotInAlbum = photoRepository.getPhotosNotInAlbum(currentAlbumId);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_photos, null);
        builder.setView(dialogView);

        // Ánh xạ view
        LinearLayout topNavigationBar = dialogView.findViewById(R.id.topNavigationBar);
        ImageButton btnBack = dialogView.findViewById(R.id.btnBack);
        TextView tvSelectedCount = dialogView.findViewById(R.id.tvSelectedCount);
        tvSelectedCount.setText("Chon mục");
        ImageButton btnSelectAll = dialogView.findViewById(R.id.btnSelectAll);
        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.photoRecyclerView);
        Button btnAddSelected = dialogView.findViewById(R.id.btnAddSelected);

        // Thiết lập RecyclerView
        dialogRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        List<PhotoEntity> finalPhotosNotInAlbum = photosNotInAlbum;
        PhotoSelectionAdapter adapter = new PhotoSelectionAdapter(photosNotInAlbum, (photo, isSelected) -> {
            // Cập nhật số lượng ảnh đã chọn
            int selectedCount = (int) finalPhotosNotInAlbum.stream().filter(PhotoEntity::isSelected).count();
            if(selectedCount == 0){
                tvSelectedCount.setText("Chon mục");
            }
            else
                tvSelectedCount.setText("Đã chọn " + selectedCount + " mục");
        }, true);
        dialogRecyclerView.setAdapter(adapter);

        topNavigationBar.setVisibility(View.VISIBLE);
        AlertDialog dialog = builder.create();

        // Xử lý nút Back
        btnBack.setOnClickListener(v -> {
            adapter.setSelectionMode(false); // Tắt chế độ chọn ảnh
            topNavigationBar.setVisibility(View.GONE); // Ẩn topNavigationBar
            dialog.dismiss();
        });

        // Xử lý nút Chọn Tất Cả
        btnSelectAll.setOnClickListener(v -> {
            boolean isAllSelected = adapter.isAllSelected();
            adapter.selectAll(!isAllSelected); // Đảo ngược trạng thái chọn
            int selectedCount = (int) finalPhotosNotInAlbum.stream().filter(PhotoEntity::isSelected).count();
            if(selectedCount == 0){
                tvSelectedCount.setText("Chon mục");
            }
            else
                tvSelectedCount.setText("Đã chọn " + selectedCount + " mục");
        });

        // Xử lý nút Thêm Ảnh Đã Chọn

        List<PhotoEntity> finalPhotosNotInAlbum1 = photosNotInAlbum;
        btnAddSelected.setOnClickListener(v -> {
            List<PhotoEntity> selectedPhotos = finalPhotosNotInAlbum1.stream()
                    .filter(PhotoEntity::isSelected)
                    .collect(Collectors.toList());
            addPhotosToAlbum(selectedPhotos);
            selectedPhotos.forEach(photo -> photo.setSelected(false));
            dialog.dismiss();
        });

        dialog.show();
    }
    private void showAlbumSelectionDialog() {
        // Tạo dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_album, null);
        builder.setView(dialogView);

        // Ánh xạ view
        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.albumRecyclerView);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        // Thiết lập RecyclerView
        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        AlbumAdapter adapter = new AlbumAdapter(new ArrayList<>(), new AlbumAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AlbumEntity album) {
                // Xử lý sự kiện click

            }
        }, albumViewModel, this);

        // Lấy danh sách các album từ repository
        AlbumViewModel albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        albumViewModel.getAllAlbums().observe(this, new Observer<List<AlbumEntity>>() {
            @Override
            public void onChanged(List<AlbumEntity> albumEntities) {
                // Lọc danh sách album, loại bỏ album có ID trùng với currentAlbumId
                List<AlbumEntity> filteredAlbums = albumEntities.stream()
                        .filter(album -> album.getId() != currentAlbumId)
                        .collect(Collectors.toList());

                // Cập nhật danh sách album đã lọc vào adapter
                adapter.updateData(filteredAlbums);
            }
        });
        adapter.setSelectionMode(true);
        dialogRecyclerView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        // Xử lý nút Xác nhận
        btnConfirm.setOnClickListener(v -> {
            // Lấy danh sách các album đã được chọn
            List<AlbumEntity> selectedAlbums = adapter.getSelectedAlbums();

            // Kiểm tra nếu có ít nhất một album được chọn
            if (!selectedAlbums.isEmpty()) {
                // Lấy danh sách các ảnh đã chọn
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());

                // Thêm các ảnh đã chọn vào từng album được chọn
                for (AlbumEntity selectedAlbum : selectedAlbums) {
                    addPhotosToAlbum(selectedPhotos, selectedAlbum.getId());
                }

                // Đóng dialog
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng chọn ít nhất một album", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
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
        Intent intent = new Intent(this, DisplaySinglePhotoActivity.class);
        intent.putExtra("photoEntity", photo);
        intent.putExtra("currentPosition", allPhotos.indexOf(photo));
        startActivity(intent);
    }
    private boolean isNewer(String date1, String date2) {
        // Tách ngày, tháng, năm từ date1
        String[] parts1 = date1.split("/");
        int day1 = Integer.parseInt(parts1[0]);
        int month1 = Integer.parseInt(parts1[1]);
        int year1 = Integer.parseInt(parts1[2]);

        // Tách ngày, tháng, năm từ date2
        String[] parts2 = date2.split("/");
        int day2 = Integer.parseInt(parts2[0]);
        int month2 = Integer.parseInt(parts2[1]);
        int year2 = Integer.parseInt(parts2[2]);

        // So sánh năm
        if (year1 > year2) {
            return true;
        } else if (year1 < year2) {
            return false;
        }

        // Nếu năm bằng nhau, so sánh tháng
        if (month1 > month2) {
            return true;
        } else if (month1 < month2) {
            return false;
        }

        // Nếu tháng bằng nhau, so sánh ngày
        if (day1 > day2) {
            return true;
        } else {
            return false;
        }
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
}
