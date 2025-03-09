package matos.csu.group3.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.AlbumDao;
import matos.csu.group3.data.local.dao.PhotoAlbumDao;
import matos.csu.group3.data.local.entity.AlbumEntity;

public class AlbumRepository {
    private final AlbumDao albumDao;
    private final PhotoAlbumDao photoAlbumDao;
    private final MutableLiveData<List<AlbumEntity>> allAlbums;
    private final Executor executor;

    public AlbumRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        albumDao = database.albumDao();
        photoAlbumDao = database.photoAlbumDao();
        executor = Executors.newSingleThreadExecutor();  // Executor for background work
        allAlbums = new MutableLiveData<>();
        loadAlbums();  // Load albums when repository is created
    }

    // Load albums from the database
    private void loadAlbums() {
        executor.execute(() -> {
            List<AlbumEntity> albumList = albumDao.getAllAlbumsSync();  // Synchronous call
            allAlbums.postValue(albumList);  // Update LiveData on the main thread
        });
    }

    // Refresh the list of albums
    public void refreshAlbums() {
        loadAlbums();
    }

    // Insert a new album
    public void insert(AlbumEntity album) {
        executor.execute(() -> {
            albumDao.insert(album);
            loadAlbums();  // Reload albums after insertion
        });
    }

    // Update an existing album
    public void update(AlbumEntity album) {
        executor.execute(() -> {
            albumDao.update(album);
            loadAlbums();  // Reload albums after update
        });
    }

    // Delete an album by ID
    public void delete(int albumId) {
        executor.execute(() -> {
            albumDao.deleteById(albumId);
            loadAlbums();  // Reload albums after deletion
        });
    }

    // Get all albums as LiveData
    public MutableLiveData<List<AlbumEntity>> getAllAlbums() {
        return allAlbums;
    }
    public LiveData<AlbumEntity> getAlbumById(int albumId) {
        return albumDao.getAlbumById(albumId);
    }
    public void deletePhotoFromAlbum(int photoId, int albumId) {
        executor.execute(() -> {
            // Xóa ảnh khỏi album (ví dụ: cập nhật albumId của ảnh về null hoặc xóa khỏi bảng trung gian)
            photoAlbumDao.deletePhotoFromAlbum(photoId, albumId);
        });
    }
}