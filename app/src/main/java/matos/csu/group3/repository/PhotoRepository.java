package matos.csu.group3.repository;

import android.app.Application;
import android.database.Cursor;
import android.provider.MediaStore;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
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

        loadPhotos();
    }

    private void loadPhotos() {
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DISPLAY_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC")) {

            List<PhotoEntity> photoList = new ArrayList<>();

            // Ensure that the cursor is valid and process it
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));

                    PhotoEntity photo = new PhotoEntity();
                    photo.setName(name);
                    photo.setFilePath(filePath);

                    // Add the photo to the list
                    photoList.add(photo);
                }
                cursor.close();
            }

            // Update the LiveData with the populated photo list
            allPhotos.postValue(photoList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insert(PhotoEntity photoEntity) {
        executor.execute(() -> photoDao.insert(photoEntity));  // Execute database operation in background
    }

    public MutableLiveData<List<PhotoEntity>> getAllPhotos() {
        return allPhotos;
    }
}
