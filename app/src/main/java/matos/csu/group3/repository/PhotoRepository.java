package matos.csu.group3.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoRepository {
    private final PhotoDao photoDao;
    private final LiveData<List<PhotoEntity>> allPhotos;
    private final Executor executor;

    public PhotoRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        photoDao = database.photoDao();
        allPhotos = photoDao.getAllPhotos();
        executor = Executors.newSingleThreadExecutor();  // Executor for background work
    }

    public void insert(PhotoEntity photoEntity) {
        executor.execute(() -> photoDao.insert(photoEntity));  // Execute database operation in background
    }

    public LiveData<List<PhotoEntity>> getAllPhotos() {
        return allPhotos;
    }
}
