package matos.csu.group3.repository;

import android.app.Application;
import android.database.Cursor;
import android.provider.MediaStore;
import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoRepository {
    private final PhotoDao photoDao;
    private final MutableLiveData<List<PhotoEntity>> allPhotos;
    private final Executor executor;
    private final Context context;

    public PhotoRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        photoDao = database.photoDao();
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

                        PhotoEntity photo = new PhotoEntity();
                        photo.setDateTaken(dateTaken);
                        photo.setName(name);
                        photo.setFilePath(filePath);
                        photoList.add(photo);
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

            allPhotos.postValue(photoList);
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

    public MutableLiveData<List<PhotoEntity>> getAllPhotos() {
        return allPhotos;
    }
}
