package matos.csu.group3.ui.main;

import android.app.AlertDialog;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Observer;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.HeaderItem;
import matos.csu.group3.data.local.entity.ListItem;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.data.local.entity.PhotoItem;
import matos.csu.group3.notification.DeletePhotosWorker;
import matos.csu.group3.repository.PhotoRepository;
import matos.csu.group3.ui.adapter.AlbumAdapter;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.ui.editor.CropAndRotateActivity;
import matos.csu.group3.utils.PhotoCache;
import matos.csu.group3.viewmodel.AlbumViewModel;
import matos.csu.group3.ui.fragment.BottomExtendedMenu;
import matos.csu.group3.viewmodel.PhotoViewModel;
import matos.csu.group3.notification.NotificationHelper;

public class MainActivity extends AppCompatActivity implements PhotoAdapter.OnItemClickListener, AlbumAdapter.OnItemClickListener {

    private PhotoViewModel photoViewModel;
    private  AlbumViewModel albumViewModel;
    private RecyclerView photoRecyclerView;
    private PhotoRepository photoRepository;
    private PhotoAdapter photoAdapter;
    private AlbumAdapter albumAdapter;
    private List<PhotoEntity> allPhotos; // Store the full list of photos
    private Map<String, List<PhotoEntity>> photosByDate; // Store photos grouped by date
    List<ListItem> groupedList;
    BottomNavigationView bottomNavigationView;
    BottomNavigationView bottomNavigationSelectionView;
    private LinearLayout topNavigationBar;
    private LinearLayout customSearchView;
    private ImageButton btnBack;
    private TextView tvSelectedCount;
    private ImageButton btnSelectAll;
    private ConstraintLayout constraintLayout;
    private static final int REQUEST_CODE_DISPLAY_PHOTO = 1001; // Unique request code


    // Register the permission request launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, load photos
                    loadPhotos();

                } else {
                    // Permission denied, show message or handle accordingly
                    Toast.makeText(this, "Permission denied! Cannot load photos.", Toast.LENGTH_SHORT).show();
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        constraintLayout = findViewById(R.id.activity_main);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        resetBottomNavSelection(bottomNavigationView);
        bottomNavigationSelectionView = findViewById(R.id.bottomNavigationSelectionView);
        resetBottomNavSelection(bottomNavigationSelectionView);
        photoRepository = new PhotoRepository(getApplication());
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);

        initializeViews();
        handleIntent(getIntent());

        // Check for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                loadPhotos();
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadPhotos();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Only schedule the notification if permission is granted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms()) {
            NotificationHelper.scheduleDailyNotification(this);
        }
    }

    @Override
    public void onItemClick(PhotoEntity photo) {
        // Handle item click events here
        // For example, open a detailed view of the photo
        showBigScreen(photo);
    }

    @Override
    public void onItemClick(AlbumEntity album) {
        // Handle item click events here
        Intent intent = new Intent(this, PhotoListOfAlbumActivity.class);
        intent.putExtra("ALBUM_ID", album.getId());  // Pass the album ID
        startActivity(intent);
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

        // Nhóm lại ảnh theo ngày
        photosByDate = groupPhotosByDate(filteredPhotos);
        groupedList = convertToGroupedList(photosByDate);
        // Cập nhật adapter với danh sách ảnh đã lọc
        photoAdapter = new PhotoAdapter(groupedList, new PhotoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PhotoEntity photo) {
                // Xử lý sự kiện click
                showBigScreen(photo);
            }
        }, new PhotoAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(PhotoEntity photo) {
                // Xử lý sự kiện long press
                photoAdapter.setSelectionMode(true); // Kích hoạt chế độ chọn
                topNavigationBar.setVisibility(View.VISIBLE);
                customSearchView.setVisibility(View.GONE);
                bottomNavigationSelectionView.setVisibility(View.VISIBLE);
                bottomNavigationView.setVisibility(View.GONE);
                updateRecyclerViewConstraints(false);
                updateSelectedCount();
                photoAdapter.notifyDataSetChanged();

            }
        }, new PhotoAdapter.OnPhotoSelectedListener() {
            @Override
            public void onPhotoSelected(PhotoEntity photo, boolean isSelected) {
                photo.setSelected(isSelected);
            }
        });
        photoAdapter.setOnSelectionChangeListener(this::updateSelectedCount);
        photoRecyclerView.setAdapter(photoAdapter);
    }

    // Method to load photos from the MediaStore
    private void loadPhotos() {
        // Fetch photos after the permission is granted
        if (photoViewModel != null) {
            photoViewModel.refreshPhotos();
        }
    }
    private void loadAlbums() {
        if(albumViewModel != null)
            albumViewModel.refreshAlbums();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Kiểm tra hướng màn hình mới
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Thiết bị đang ở chế độ ngang
            Toast.makeText(this, "Landscape Mode", Toast.LENGTH_SHORT).show();
            GridLayoutManager layoutManager = new GridLayoutManager(this, 6); // 6 ảnh mỗi hàng khi xoay ngang
            photoRecyclerView.setLayoutManager(layoutManager);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Thiết bị đang ở chế độ dọc
            Toast.makeText(this, "Portrait Mode", Toast.LENGTH_SHORT).show();
            GridLayoutManager layoutManager = new GridLayoutManager(this, 3); // 3 ảnh mỗi hàng khi xoay dọc
            photoRecyclerView.setLayoutManager(layoutManager);
        }
    }

    private void showBigScreen(PhotoEntity photo) {
        // Cache the photo list
        PhotoCache.getInstance().setPhotoList(allPhotos);

        // Start DisplaySinglePhotoActivity for result
        Intent intent = new Intent(this, DisplaySinglePhotoActivity.class);
        intent.putExtra("photoEntity", photo);
        intent.putExtra("currentPosition", allPhotos.indexOf(photo));
        startActivityForResult(intent, REQUEST_CODE_DISPLAY_PHOTO);
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

    private void initializeViews() {
        topNavigationBar = findViewById(R.id.topNavigationBar);
        customSearchView = findViewById(R.id.customSearchView);
        btnBack = findViewById(R.id.btnBack);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnBack.setOnClickListener(v -> {
            photoAdapter.setSelectionMode(false); // Tắt chế độ chọn ảnh
            topNavigationBar.setVisibility(View.GONE); // Ẩn top navigation bar
            customSearchView.setVisibility(View.VISIBLE);
            bottomNavigationSelectionView.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            updateRecyclerViewConstraints(true);
        });
        btnSelectAll.setOnClickListener(v -> {
            boolean isAllSelected = photoAdapter.isAllSelected();
            photoAdapter.selectAll(!isAllSelected); // Đảo ngược trạng thái chọn
        });
        bottomNavigationSelectionView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                showAlbumSelectionDialog();
                return true;
            } else if (item.getItemId() == R.id.action_share) {
                // Handle share action
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());

                if (!selectedPhotos.isEmpty()) {
                    // Call the static method from PhotoListOfAlbumActivity
                    PhotoListOfAlbumActivity.sharePhotosViaPackage(MainActivity.this, selectedPhotos);
                } else {
                    Toast.makeText(this, "Vui lòng chọn ít nhất một ảnh để chia sẻ", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (item.getItemId() == R.id.action_add_fav){
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());
                if (!selectedPhotos.isEmpty()) {
                    photoViewModel.addPhotosToFavourite(selectedPhotos);
                    Toast.makeText(this, "Đã thêm " + selectedPhotos.size() + " ảnh vào yêu thích", Toast.LENGTH_SHORT).show();
                    // Cập nhật RecyclerView hoặc giao diện khác nếu cần
                    loadAlbums(); // Ví dụ: Tải lại danh sách album
                } else {
                    Toast.makeText(this, "Không có ảnh nào được chọn", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (item.getItemId() == R.id.action_delete) {
                // Handle delete action
                List<PhotoEntity> selectedPhotos = allPhotos.stream()
                        .filter(PhotoEntity::isSelected)
                        .collect(Collectors.toList());

                if (!selectedPhotos.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        showManageStorageDialog();
                        return true;
                    }

                    // Show delete confirmation dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc chắn muốn xóa các ảnh đã chọn không?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                // Move the selected photos to the Trash album
                                photoRepository.movePhotosToTrash(selectedPhotos);
                                //Delete photo after a schedule times
                                photoRepository.schedulePermanentDeletion(selectedPhotos, this);


                                // Reset selection state
                                selectedPhotos.forEach(photo -> photo.setSelected(false));

                                // Update UI
                                photoAdapter.notifyDataSetChanged();

                                // Show a success message
                                Toast.makeText(this, "Đã chuyển ảnh vào thùng rác", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    Toast.makeText(this, "Không có ảnh nào được chọn", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
        // Khởi tạo RecyclerView
        photoRecyclerView = findViewById(R.id.photoRecyclerView);

        // Khởi tạo adapter với danh sách ảnh rỗng ban đầu
        photosByDate = new LinkedHashMap<>();
        groupedList = convertToGroupedList(photosByDate);
        photoAdapter = new PhotoAdapter(groupedList, new PhotoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PhotoEntity photo) {
                // Xử lý sự kiện click
                showBigScreen(photo);
            }
        }, new PhotoAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(PhotoEntity photo) {
                // Xử lý sự kiện long press
                photoAdapter.setSelectionMode(true); // Kích hoạt chế độ chọn
                topNavigationBar.setVisibility(View.VISIBLE);
                customSearchView.setVisibility(View.GONE);
                bottomNavigationSelectionView.setVisibility(View.VISIBLE);
                bottomNavigationView.setVisibility(View.GONE);
                updateRecyclerViewConstraints(false);
                updateSelectedCount();
                photoAdapter.notifyDataSetChanged();
            }
        }, new PhotoAdapter.OnPhotoSelectedListener() {
            @Override
            public void onPhotoSelected(PhotoEntity photo, boolean isSelected) {
                photo.setSelected(isSelected);
            }
        });
        photoAdapter.setOnSelectionChangeListener(this::updateSelectedCount);
        // Khởi tạo adapter cho album
        albumAdapter = new AlbumAdapter(new ArrayList<>(), this, albumViewModel, this);

        // Kiểm tra hướng màn hình
        int orientation = getResources().getConfiguration().orientation;

        // Thiết lập số cột dựa trên hướng màn hình
        int spanCount = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 6 : 3;

        // Khởi tạo GridLayoutManager cho ảnh
        GridLayoutManager photoLayoutManager = new GridLayoutManager(this, spanCount);
        photoLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Nếu là Header, chiếm toàn bộ hàng (span size = số cột)
                if (photoAdapter.getItemViewType(position) == PhotoAdapter.TYPE_HEADER) {
                    return spanCount;
                }
                // Nếu là Photo, chiếm 1 ô (span size = 1)
                return 1;
            }
        });

        // Khởi tạo GridLayoutManager cho album
        GridLayoutManager albumLayoutManager = new GridLayoutManager(this, spanCount);

        // Thiết lập LayoutManager mặc định cho RecyclerView (ảnh)
        photoRecyclerView.setLayoutManager(photoLayoutManager);
        photoRecyclerView.setAdapter(photoAdapter);

        // Khởi tạo ViewModel và quan sát dữ liệu
        photoViewModel.getAllPhotos().observe(this, new Observer<List<PhotoEntity>>() {
            @Override
            public void onChanged(List<PhotoEntity> photoEntities) {
                if (photoEntities != null && !photoEntities.isEmpty()) {
                    // Cập nhật danh sách ảnh
                    allPhotos = photoEntities;
                    // Sort photos by date (newest first)
                    for (int i = 0; i < allPhotos.size() - 1; i++) {
                        for (int j = i + 1; j < allPhotos.size(); j++) {
                            PhotoEntity photo1 = allPhotos.get(i);
                            PhotoEntity photo2 = allPhotos.get(j);

                            // Compare dates
                            if (isNewer(photo2.getDateTaken(), photo1.getDateTaken())) {
                                // Swap positions if photo2 is newer than photo1
                                Collections.swap(allPhotos, i, j);
                            }
                        }
                    }
                    // Nhóm ảnh theo ngày
                    photosByDate = groupPhotosByDate(photoEntities);
                    groupedList = convertToGroupedList(photosByDate);

                    // Cập nhật adapter với danh sách ảnh mới
                    photoAdapter.updateData(groupedList);

                    // Chỉ gọi loadAlbums() sau khi ảnh đã có
                    loadAlbums();
                }
            }
        });

        // Khởi tạo ViewModel cho album
        albumViewModel.getAllAlbums().observe(this, new Observer<List<AlbumEntity>>() {
            @Override
            public void onChanged(List<AlbumEntity> albumEntities) {
                // Cập nhật danh sách album
                albumAdapter.updateData(albumEntities);
            }
        });

        // Khởi tạo SearchView và các sự kiện liên quan
        EditText searchEditText = findViewById(R.id.search_src_text);
        ImageView searchIcon = findViewById(R.id.search_mag_icon);
        searchIcon.setOnClickListener(v -> {
            String query = searchEditText.getText().toString();
            filterPhotos(query);
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString();
            filterPhotos(query);
            return true;
        });

        // Khởi tạo BottomNavigationView
        FloatingActionButton btnAddAlbum = findViewById(R.id.addAlbumButton);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Xử lý sự kiện khi chọn một mục trong BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_photos) {
                    // Xử lý khi chọn "Ảnh"
                    photoRecyclerView.setLayoutManager(photoLayoutManager);
                    photoRecyclerView.setAdapter(photoAdapter);
                    btnAddAlbum.setVisibility(View.GONE);
                    return true;
                } else if (id == R.id.nav_albums) {
                    // Xử lý khi chọn "Album"
                    photoRecyclerView.setLayoutManager(albumLayoutManager);  // Sử dụng GridLayoutManager cho album
                    photoRecyclerView.setAdapter(albumAdapter);
                    btnAddAlbum.setVisibility(View.VISIBLE);
                    btnAddAlbum.setOnClickListener(v -> showAddAlbumDialog());
                    return true;
                } else if (id == R.id.nav_menu) {
                    // Khi nhấn vào "Menu", hiển thị BottomSheetDialogFragment
                    BottomExtendedMenu.show(getSupportFragmentManager());

                    return false;
                }

                return false;
            }
        });
    }

    //Handle delete function
    //Permission required message
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
        },albumViewModel, this);

        // Lấy danh sách các album từ repository
        AlbumViewModel albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        albumViewModel.getAllAlbums().observe(this, new Observer<List<AlbumEntity>>() {
            @Override
            public void onChanged(List<AlbumEntity> albumEntities) {
                // Cập nhật danh sách album đã lọc vào adapter
                adapter.updateData(albumEntities);
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

                // Đặt lại trạng thái isSelected = false cho tất cả ảnh trong allPhotos
                for (PhotoEntity photo : allPhotos) {
                    photo.setSelected(false);
                }

                // Thêm các ảnh đã chọn vào từng album được chọn
                for (AlbumEntity selectedAlbum : selectedAlbums) {
                    addPhotosToAlbum(selectedPhotos, selectedAlbum.getId());
                }

                // Đóng dialog
                Toast.makeText(this, "Đã thêm ảnh vào album", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng chọn ít nhất một album", Toast.LENGTH_SHORT).show();
            }
            photoAdapter.setSelectionMode(false); // Tắt chế độ chọn ảnh
            topNavigationBar.setVisibility(View.GONE); // Ẩn top navigation bar
            customSearchView.setVisibility(View.VISIBLE);
            bottomNavigationSelectionView.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            updateRecyclerViewConstraints(true);
        });

        dialog.show();
    }
    private void addPhotosToAlbum(List<PhotoEntity> photos, int albumID){
        photoRepository.addPhotosToAlbum(albumID, photos);
    }

    private void updateSelectedCount() {
        int selectedCount = photoAdapter.getSelectedCount();
        if(selectedCount == 0){
            tvSelectedCount.setText("Chọn Mục");
        }
        else tvSelectedCount.setText("Đã chọn " + selectedCount + " mục");
    }
    private void showAddAlbumDialog() {
        // Tạo một AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm album mới");

        // Tạo layout cho dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_album, null);
        builder.setView(dialogView);

        // Lấy các view từ dialog layout
        EditText edtAlbumName = dialogView.findViewById(R.id.edtAlbumName);

        // Thiết lập nút "Thêm"
        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String albumName = edtAlbumName.getText().toString().trim();
            if (!albumName.isEmpty()) {
                // Tạo album mới và thêm vào danh sách
                AlbumEntity newAlbum = new AlbumEntity(albumName, getCurrentDate());
                addAlbum(newAlbum);
            } else {
                Toast.makeText(this, "Vui lòng nhập tên album", Toast.LENGTH_SHORT).show();
            }
        });

        // Thiết lập nút "Hủy"
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        // Hiển thị dialog
        builder.create().show();
    }

    private void addAlbum(AlbumEntity album) {
        albumViewModel.insert(album);
    }

    // Phương thức lấy ngày hiện tại
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Phương thức để nhóm ảnh theo ngày
    private Map<String, List<PhotoEntity>> groupPhotosByDate(List<PhotoEntity> photos) {
        // Sử dụng LinkedHashMap để giữ nguyên thứ tự các ngày
        Map<String, List<PhotoEntity>> photosByDate = new LinkedHashMap<>();

        // Nhóm ảnh theo ngày
        for (PhotoEntity photo : photos) {
            if (photo != null) {
                String date = photo.getDateTaken(); // Lấy ngày từ PhotoEntity
                if (!photosByDate.containsKey(date)) {
                    photosByDate.put(date, new ArrayList<>());
                }
                photosByDate.get(date).add(photo);
            }
        }

        // Sắp xếp các ngày (keys của Map) từ mới nhất đến cũ nhất
        List<String> sortedDates = new ArrayList<>(photosByDate.keySet());
        sortedDates.sort((date1, date2) -> {
            // Chuyển đổi ngày thành số để so sánh (giả sử định dạng ngày là "dd/MM/yyyy")
            long date1Millis = 0, date2Millis = 0;
            try {
                date1Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date1).getTime();
                date2Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date2).getTime();

            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            return Long.compare(date2Millis, date1Millis); // Sắp xếp từ mới nhất đến cũ nhất
        });

        // Tạo một LinkedHashMap mới với thứ tự các ngày đã được sắp xếp
        Map<String, List<PhotoEntity>> sortedPhotosByDate = new LinkedHashMap<>();
        for (String date : sortedDates) {
            sortedPhotosByDate.put(date, photosByDate.get(date));
        }

        // Sắp xếp các ảnh trong từng nhóm theo thời gian chụp (từ mới nhất đến cũ nhất)
        for (List<PhotoEntity> photoList : sortedPhotosByDate.values()) {
            photoList.sort((photo1, photo2) -> {
                // Giả sử PhotoEntity có phương thức getDateTaken() trả về định dạng "dd/MM/yyyy"
                long date1Millis = 0, date2Millis = 0;
                try {
                    date1Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(photo1.getDateTaken()).getTime();
                    date2Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(photo2.getDateTaken()).getTime();

                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return Long.compare(date2Millis, date1Millis); // Sắp xếp từ mới nhất đến cũ nhất
            });
        }

        return sortedPhotosByDate;
    }

    private List<ListItem> convertToGroupedList(Map<String, List<PhotoEntity>> photosByDate) {
        List<ListItem> groupedList = new ArrayList<>();

        for (Map.Entry<String, List<PhotoEntity>> entry : photosByDate.entrySet()) {
            groupedList.add(new HeaderItem(entry.getKey()));
            for (PhotoEntity photo : entry.getValue()) {
                groupedList.add(new PhotoItem(photo));
            }
        }

        return groupedList;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == UCrop.REQUEST_CROP) {
                // Handle photo editing result
                if (data != null && data.hasExtra("photoEntity")) {
                    PhotoEntity updatedPhoto = (PhotoEntity) data.getSerializableExtra("photoEntity");

                    if (photoViewModel != null) {
                        photoViewModel.updatePhoto(updatedPhoto);
                        photoViewModel.refreshPhotos();
                    }

                    if (updatedPhoto.getFilePath() != null) {
                        MediaScannerConnection.scanFile(
                                this,
                                new String[]{updatedPhoto.getFilePath()},
                                new String[]{"image/jpeg"},
                                (path, uri) -> Log.d("MainActivity", "Rescanned edited file: " + path)
                        );
                    }
                }
            } else if (requestCode == REQUEST_CODE_DISPLAY_PHOTO) {
                // Handle photo deletion result
                if (data != null && data.hasExtra("deletedPhotoId")) {
                    int deletedPhotoId = data.getIntExtra("deletedPhotoId", -1);
                    if (deletedPhotoId != -1) {
                        // Remove the deleted photo from the list and update the UI
                        allPhotos.removeIf(photo -> photo.getId() == deletedPhotoId);
                        photoAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    //Handle Notification

    private void handleIntent(Intent intent) {
        if (intent != null) {
            String photoPath = intent.getStringExtra("photo_path");
            long dateTakenMillis = intent.getLongExtra("date_taken", -1);

            if (photoPath != null && dateTakenMillis != -1) {
                PhotoEntity photo = new PhotoEntity();
                photo.setFilePath(photoPath);
                photo.setDateTaken(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(dateTakenMillis)));
                showBigScreen(photo);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        handleIntent(intent); // Handle the new intent
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    void updateRecyclerViewConstraints(boolean isSearchViewVisible) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout); // Sao chép các ràng buộc hiện tại

        if (isSearchViewVisible) {
            // Ràng buộc với customSearchView
            constraintSet.connect(
                    R.id.photoRecyclerView,
                    ConstraintSet.TOP,
                    R.id.customSearchView,
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