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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Observer;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Date;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.HeaderItem;
import matos.csu.group3.data.local.entity.ListItem;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.data.local.entity.PhotoItem;
import matos.csu.group3.ui.adapter.AlbumAdapter;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.ui.editor.CropAndRotateActivity;
import matos.csu.group3.viewmodel.AlbumViewModel;
import matos.csu.group3.ui.fragment.BottomExtendedMenu;
import matos.csu.group3.viewmodel.PhotoViewModel;
import matos.csu.group3.notification.NotificationHelper;

public class MainActivity extends AppCompatActivity implements PhotoAdapter.OnItemClickListener, AlbumAdapter.OnItemClickListener {

    private PhotoViewModel photoViewModel;
    private  AlbumViewModel albumViewModel;
    private RecyclerView photoRecyclerView;
    private PhotoAdapter photoAdapter;
    private AlbumAdapter albumAdapter;
    private List<PhotoEntity> allPhotos; // Store the full list of photos
    private Map<String, List<PhotoEntity>> photosByDate; // Store photos grouped by date
    List<ListItem> groupedList;


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
                photoAdapter.notifyDataSetChanged();

            }
        });
        photoRecyclerView.setAdapter(photoAdapter);
    }

    // Method to load photos from the MediaStore
    private void loadPhotos() {
        // Fetch photos after the permission is granted
        if (photoViewModel != null) {
            photoViewModel.refreshPhotos();
        }
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
        Intent intent = new Intent(this, DisplaySinglePhotoActivity.class);
        intent.putExtra("photoEntity", photo);
        startActivity(intent);
    }

    private void initializeViews() {
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
                photoAdapter.notifyDataSetChanged();
            }
        });

        // Khởi tạo adapter cho album
        albumAdapter = new AlbumAdapter(new ArrayList<>(), this);

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
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.getAllPhotos().observe(this, new Observer<List<PhotoEntity>>() {
            @Override
            public void onChanged(List<PhotoEntity> photoEntities) {
                // Cập nhật danh sách ảnh
                allPhotos = photoEntities;

                // Nhóm ảnh theo ngày
                photosByDate = groupPhotosByDate(photoEntities);
                groupedList = convertToGroupedList(photosByDate);

                // Cập nhật adapter với danh sách ảnh mới
                photoAdapter.updateData(groupedList);
            }
        });

        // Khởi tạo ViewModel cho album
        albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
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
}