package matos.csu.group3.repository;

import android.app.Application;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.PhotoAlbumDao;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoRepository {
    private final PhotoDao photoDao;
    private final PhotoAlbumDao photoAlbumDao;
    private final MutableLiveData<List<PhotoEntity>> allPhotos;
    private final ExecutorService executor;
    private final Context context;

    public PhotoRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        photoDao = database.photoDao();
        photoAlbumDao = database.photoAlbumDao();
        executor = Executors.newSingleThreadExecutor();  // Executor for background work
        context = application.getApplicationContext();
        allPhotos = new MutableLiveData<>();
    }

    private void loadPhotos() {
        executor.execute(() -> {
            List<PhotoEntity> photoList = new ArrayList<>();

            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN
            };

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC")) {

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                        long dateTakenMillis = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
                        String dateTaken = sdf.format(new Date(dateTakenMillis));

                        // Kiểm tra xem ảnh đã tồn tại trong cơ sở dữ liệu chưa
                        PhotoEntity existingPhoto = photoDao.getPhotoByFilePath(filePath);
                        if (existingPhoto == null) {
                            PhotoEntity photo = new PhotoEntity();
                            photo.setDateTaken(dateTaken);
                            photo.setName(name);
                            photo.setFilePath(filePath);
                            photoList.add(photo);
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Sắp xếp danh sách ảnh theo ngày chụp (từ mới nhất đến cũ nhất)
            photoList.sort((photo1, photo2) -> Long.compare(
                    Long.parseLong(photo2.getDateTaken().replace("/", "")), // Chuyển đổi ngày thành số để so sánh
                    Long.parseLong(photo1.getDateTaken().replace("/", ""))
            ));

            // Chỉ chèn các ảnh mới vào cơ sở dữ liệu
            if (!photoList.isEmpty()) {
                photoDao.insertPhotos(photoList);
            }

            // Cập nhật LiveData với danh sách ảnh mới nhất từ cơ sở dữ liệu
            allPhotos.postValue(photoDao.getAllPhotos().getValue());
        });
    }

    public void refreshPhotos() {
//        executor.execute(this::loadPhotos);
        loadPhotos();
    }


    public void insert(PhotoEntity photoEntity) {
        executor.execute(() -> photoDao.insert(photoEntity));  // Execute database operation in background
    }

    public void update(PhotoEntity photoEntity) {
        new Thread(() -> {
            photoDao.update(photoEntity);
        }).start();
    }

    public LiveData<List<PhotoEntity>> getAllPhotos() {
        return photoDao.getAllPhotos();
    }
    public void addPhotosToAlbum(int albumId, List<PhotoEntity> photos) {
        executor.execute(() -> {
            for (PhotoEntity photo : photos) {
                PhotoAlbum photoAlbum = new PhotoAlbum(photo.getId(), albumId);
                photoAlbumDao.insert(photoAlbum);
            }
        });
    }
    public List<PhotoEntity> getPhotosNotInAlbum(int currentAlbumId) throws InterruptedException, ExecutionException {
        Future<List<PhotoEntity>> future = executor.submit(() -> {
            List<PhotoEntity> allPhotosList = photoDao.getPhotosNotInAlbum(currentAlbumId);

            // Sắp xếp danh sách theo ngày
            allPhotosList.sort((photo1, photo2) -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                try {
                    Date date1 = sdf.parse(photo1.getDateTaken());
                    Date date2 = sdf.parse(photo2.getDateTaken());
                    return date2.compareTo(date1); // Sắp xếp từ mới nhất đến cũ nhất
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0; // Giữ nguyên thứ tự nếu có lỗi
                }
            });

            return allPhotosList;
        });

        // Đợi kết quả từ Future và trả về
        return future.get();
    }
    public LiveData<List<PhotoEntity>> getPhotosByAlbumId(int albumId) {
        LiveData<List<PhotoEntity>> photosLiveData = photoDao.getPhotosByAlbumId(albumId);

        // Sắp xếp danh sách ảnh theo ngày chụp (từ mới nhất đến cũ nhất)
        return Transformations.map(photosLiveData, photosList -> {
            photosList.sort((photo1, photo2) -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                try {
                    Date date1 = sdf.parse(photo1.getDateTaken());
                    Date date2 = sdf.parse(photo2.getDateTaken());
                    return date2.compareTo(date1); // Sắp xếp giảm dần (mới nhất trước)
                } catch (ParseException e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, "Lỗi khi parse ngày: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                    return 0;
                }
            });
            return photosList;
        });
    }

}
