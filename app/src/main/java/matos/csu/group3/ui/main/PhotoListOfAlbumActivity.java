package matos.csu.group3.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.ui.adapter.PhotoSelectionAdapter;

public class PhotoListOfAlbumActivity extends AppCompatActivity implements PhotoSelectionAdapter.OnPhotoSelectedListener {

    private RecyclerView photoRecyclerView;
    private PhotoSelectionAdapter photoAdapter;
    private TextView albumNameTextView;
    private  PhotoDao photoDao;
    private AlbumEntity album;
    private PhotoRepository photoRepository;
    private AlbumRepository albumRepository;
    private List<PhotoEntity> allPhotos;
    private int currentAlbumId;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        albumRepository = new AlbumRepository(getApplication());
        albumNameTextView = findViewById(R.id.albumNameTextView);
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        photoRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        currentAlbumId = intent.getIntExtra("ALBUM_ID", -1);


        if (currentAlbumId != -1) {
            albumRepository.getAlbumById(currentAlbumId).observe(this, albumEntity -> {
                if (albumEntity != null) {
                    albumNameTextView.setText(albumEntity.getName());
                } else {
                    // Xử lý trường hợp không tìm thấy album
                    albumNameTextView.setText("Album không tồn tại");
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
            else if(item.getItemId() == R.id.action_settings){
                return true;
            }

            return false;
        });
    }

    private void loadPhotos(int albumId) {
        allPhotos = new ArrayList<>();
        photoAdapter = new PhotoSelectionAdapter(allPhotos, this);
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

    @Override
    public void onPhotoSelected(PhotoEntity photo, boolean isSelected) {
        photo.setSelected(isSelected);
    }

    private void showPhotoSelectionDialog() {
        List<PhotoEntity> photosNotInAlbum = new ArrayList<>();
        try {
             photosNotInAlbum = photoRepository.getPhotosNotInAlbum(currentAlbumId);
            // Tiếp tục xử lý photosNotInAlbum
        } catch (InterruptedException e) {
            // Xử lý khi luồng bị gián đoạn
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Đánh dấu luồng bị gián đoạn
        } catch (ExecutionException e) {
            // Xử lý khi có lỗi trong quá trình thực thi
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_photos, null);
        builder.setView(dialogView);

        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.photoRecyclerView);

        dialogRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        PhotoSelectionAdapter adapter = new PhotoSelectionAdapter(photosNotInAlbum, this);
        dialogRecyclerView.setAdapter(adapter);

        Button btnAddSelected = dialogView.findViewById(R.id.btnAddSelected);
        AlertDialog dialog = builder.create();
        List<PhotoEntity> finalPhotosNotInAlbum = photosNotInAlbum;
        btnAddSelected.setOnClickListener(v -> {
            List<PhotoEntity> selectedPhotos = finalPhotosNotInAlbum.stream()
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
