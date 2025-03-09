package matos.csu.group3.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
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
import matos.csu.group3.ui.adapter.PhotoSelectionAdapter;

public class PhotoListOfAlbumActivity extends AppCompatActivity implements PhotoSelectionAdapter.OnPhotoSelectedListener {

    private RecyclerView photoRecyclerView;
    private PhotoSelectionAdapter photoAdapter;
    private  PhotoDao photoDao;
    private AlbumEntity album;
    private PhotoRepository photoRepository;
    private AlbumRepository albumRepository;
    private List<PhotoEntity> allPhotos;
    private int currentAlbumId;
    private LinearLayout topNavigationBar;
    private ImageButton btnBack;
    private TextView tvSelectedCount;
    private ImageButton btnSelectAll;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        albumRepository = new AlbumRepository(getApplication());
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        topNavigationBar = findViewById(R.id.topNavigationBar);
        btnBack = findViewById(R.id.btnBack);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnBack.setOnClickListener(v -> {
            photoAdapter.setSelectionMode(false); // Tắt chế độ chọn ảnh
            topNavigationBar.setVisibility(View.GONE); // Ẩn top navigation bar
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
        bottomNavigationView.setVisibility(View.GONE);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                // Gọi phương thức xóa ảnh
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());
                if(!selectedPhotos.isEmpty())
                    showDeleteConfirmationDialog();
                return true;
            }
            else if(item.getItemId() == R.id.action_add){
                return true;
            }
            return false;
        });
    }

    private void loadPhotos(int albumId) {
        allPhotos = new ArrayList<>();
        photoAdapter = new PhotoSelectionAdapter(allPhotos, this, false);
        photoAdapter.setOnLongPressListener(() -> {
            topNavigationBar.setVisibility(View.VISIBLE); // Hiển thị top navigation bar
            updateSelectedCount();
            bottomNavigationView.setVisibility(View.VISIBLE); // Hiển thị BottomNavigationView
        });
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

    @Override
    public void onPhotoSelected(PhotoEntity photo, boolean isSelected) {
        photo.setSelected(isSelected);
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

    private void addPhotosToAlbum(List<PhotoEntity> photos) {
        photoRepository.addPhotosToAlbum(currentAlbumId, photos);
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
}
