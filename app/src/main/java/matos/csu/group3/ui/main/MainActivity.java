package matos.csu.group3.ui.main;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Observer;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.provider.MediaStore;
import android.database.Cursor;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.Date;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.HeaderItem;
import matos.csu.group3.data.local.entity.ListItem;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.data.local.entity.PhotoItem;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.ui.editor.CropAndRotateActivity;
import matos.csu.group3.ui.fragment.BottomExtendedMenu;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class MainActivity extends AppCompatActivity implements PhotoAdapter.OnItemClickListener {

    private PhotoViewModel photoViewModel;
    private RecyclerView photoRecyclerView;
    private PhotoAdapter photoAdapter;
    private List<PhotoEntity> allPhotos; // Store the full list of photos
    private Map<String, List<PhotoEntity>> photosByDate; // Store photos grouped by date
    List<ListItem> groupedList;

    private static final String CHANNEL_ID = "photo_reminder_channel";
    private static final int NOTIFICATION_ID = 1;

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

        createNotificationChannel();

        initializeViews();

        // Handle the photo path from the notification
        handleIntent(getIntent());

        // Check for permissions using ActivityResultContracts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                loadPhotos(); // Permission granted, load photos
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                checkPhotosAndNotify();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // For older versions, handle `READ_EXTERNAL_STORAGE` permission
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadPhotos();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onItemClick(PhotoEntity photo) {
        // Handle item click events here
        // For example, open a detailed view of the photo
        showBigScreen(photo);
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
        photoAdapter = new PhotoAdapter(groupedList, this);
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
//        // Hiển thị layout chứa ảnh lớn
//        setContentView(R.layout.activity_display_single_photo);
//
//        // Ánh xạ các view trong layout solo_picture
//        TextView txtSoloMsg = findViewById(R.id.txtSoloMsg);
//        ImageView imgSoloPhoto = findViewById(R.id.imgSoloPhoto);
//        Button btnSoloBack = findViewById(R.id.btnSoloBack);
//        Button btnEdit = findViewById(R.id.btnEdit);
//
//        // Đặt caption và ảnh lớn
//        txtSoloMsg.setText(photo.getDateTaken() + "X");
//        Glide.with(this) // "this" là Context (Activity hoặc Fragment)
//                .load(new File(photo.getFilePath())) // Load ảnh từ đường dẫn tệp
//                .into(imgSoloPhoto);
//
//        btnEdit.setOnClickListener(v -> {
//            Intent cropIntent = new Intent(this, CropAndRotateActivity.class);
//            Log.d("CropActivity", "Image URI: " + Uri.parse(photo.getFilePath()));
//            cropIntent.setData(Uri.fromFile(new File(photo.getFilePath())));
//            cropIntent.putExtra("photoEntity", photo);
//
//            startActivity(cropIntent);
//        });
//
//        // Xử lý sự kiện nút "GO BACK"
//        btnSoloBack.setOnClickListener(v -> {
//            // Quay lại layout chính (activity_main)
//            setContentView(R.layout.activity_main);
//
//            // Khởi tạo lại các view và RecyclerView
//            initializeViews();
//        });
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
        photoAdapter = new PhotoAdapter(groupedList, this);

        // Kiểm tra hướng màn hình
        int orientation = getResources().getConfiguration().orientation;

        // Thiết lập số cột dựa trên hướng màn hình
        int spanCount = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 6 : 3;

        // Khởi tạo GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
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

        // Thiết lập LayoutManager cho RecyclerView
        photoRecyclerView.setLayoutManager(layoutManager);
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
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Xử lý sự kiện khi chọn một mục trong BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_photos) {
                    // Xử lý khi chọn "Ảnh"
                    return true;
                } else if (id == R.id.nav_albums) {
                    // Xử lý khi chọn "Album"
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
                                new String[]{ updatedPhoto.getFilePath() },
                                new String[]{ "image/jpeg" },
                                (path, uri) -> Log.d("MainActivity", "Rescanned edited file: " + path)
                        );
                    }
                }
            }
        }
    }

    //Handle Notification
    
    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("photo_path")) {
            String photoPath = intent.getStringExtra("photo_path");
            long dateTakenMillis = intent.getLongExtra("date_taken", -1);

            if (photoPath != null && dateTakenMillis != -1) {
                // Create a PhotoEntity object using the default constructor
                PhotoEntity photo = new PhotoEntity();
                photo.setFilePath(photoPath);
                photo.setDateTaken(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(dateTakenMillis)));
                showBigScreen(photo);
            }
        }
    }

    private void checkPhotosAndNotify() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date()); // Today's date in full format (yyyy-MM-dd)

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATA};
        String selection = MediaStore.Images.Media.DATE_TAKEN + " IS NOT NULL";
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        Cursor cursor = getContentResolver().query(uri, projection, selection, null, sortOrder);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long dateTakenMillis = cursor.getLong(0);
                String photoDate = sdf.format(new Date(dateTakenMillis)); // Convert timestamp to yyyy-MM-dd
                String photoPath = cursor.getString(1); // Get the file path of the photo

                if (photoDate.substring(5).equals(todayDate.substring(5))) { // Compare MM-dd only
                    // Calculate the number of years ago the photo was taken
                    int yearsAgo = calculateYearsAgo(dateTakenMillis);
                    cursor.close();
                    sendNotification(yearsAgo, photoPath, dateTakenMillis); // Pass the photo path to the notification
                    return;
                }
            }
            cursor.close();
        }
    }

    private int calculateYearsAgo(long dateTakenMillis) {
        Calendar photoDate = Calendar.getInstance();
        photoDate.setTimeInMillis(dateTakenMillis);

        Calendar today = Calendar.getInstance();

        int yearsAgo = today.get(Calendar.YEAR) - photoDate.get(Calendar.YEAR);

        // Adjust if the photo date is later in the year than today's date
        if (today.get(Calendar.MONTH) < photoDate.get(Calendar.MONTH)) {
            yearsAgo--;
        } else if (today.get(Calendar.MONTH) == photoDate.get(Calendar.MONTH)) {
            if (today.get(Calendar.DAY_OF_MONTH) < photoDate.get(Calendar.DAY_OF_MONTH)) {
                yearsAgo--;
            }
        }

        return yearsAgo;
    }

    private void sendNotification(int yearsAgo, String photoPath, long dateTakenMillis) {
        // Create an intent to open the MainActivity with the photo path
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("photo_path", photoPath); // Pass the photo path as an extra
        intent.putExtra("date_taken", dateTakenMillis); // Pass the date taken
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String notificationText;
        if (yearsAgo == 1) {
            notificationText = "You took a photo on this date 1 year ago!";
        } else {
            notificationText = "You took a photo on this date " + yearsAgo + " years ago!";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_photo)
                .setContentTitle("Photo Reminder")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CharSequence name = "Photo Reminder";
            String description = "Reminds you of photos taken on the same date in previous years";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}