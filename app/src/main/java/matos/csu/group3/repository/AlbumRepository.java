package matos.csu.group3.repository;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.AlbumDao;
import matos.csu.group3.data.local.dao.PhotoAlbumDao;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class AlbumRepository {
    private final AlbumDao albumDao;
    private final PhotoAlbumDao photoAlbumDao;
    private final PhotoDao photoDao;
    private final MutableLiveData<List<AlbumEntity>> allAlbums;
    private final Executor executor;
    private final Application application;
    private PhotoRepository photoRepository;

    public AlbumRepository(Application application) {
        this.application = application;
        photoRepository = new PhotoRepository(application);
        AppDatabase database = AppDatabase.getInstance(application);
        albumDao = database.albumDao();
        photoDao = database.photoDao();
        photoAlbumDao = database.photoAlbumDao();
        executor = Executors.newSingleThreadExecutor();  // Executor for background work
        allAlbums = new MutableLiveData<>();
        loadAlbums();  // Load albums when repository is created
    }

    // Load albums and photos from MediaStore
    private void loadAlbums() {
        executor.execute(() -> {
            // 1. Load và xử lý các album từ MediaStore như bình thường
            List<PhotoEntity> photos = photoDao.getAllNonDeletedPhotos();

            Map<String, List<PhotoEntity>> albumPhotoMap = new HashMap<>();
            for (PhotoEntity photo : photos) {
                String path = photo.getFilePath().toLowerCase();
                String albumName = extractAlbumNameFromPath(path);
                if (albumName != null) {
                    albumPhotoMap.computeIfAbsent(albumName, k -> new ArrayList<>()).add(photo);
                }
            }

            for (Map.Entry<String, List<PhotoEntity>> entry : albumPhotoMap.entrySet()) {
                String albumName = entry.getKey();
                List<PhotoEntity> albumPhotos = entry.getValue();

                // Get or create the album
                AlbumEntity album = albumDao.getAlbumByNameSync(albumName);
                int albumId;
                if (album == null) {
                    album = new AlbumEntity();
                    album.setName(albumName);
                    albumId = (int) albumDao.insert(album);

                    if (albumId == -1) {
                        album = albumDao.getAlbumByNameSync(albumName);
                        if (album != null) {
                            albumId = album.getId();
                        } else {
                            Log.e("AlbumInsert", "Lỗi: Không thể lấy ID album.");
                            continue;
                        }
                    }
                } else {
                    albumId = album.getId();
                }

                if (albumId <= 0) {
                    Log.e("PhotoInsertError", "Album ID không hợp lệ: " + albumId);
                    continue;
                }

                for (PhotoEntity photo : albumPhotos) {
                    int photoId = photo.getId();

                    if (photoId <= 0) {
                        Log.e("PhotoInsertError", "Photo ID không hợp lệ: " + photoId);
                        continue;
                    }

                    int count = photoAlbumDao.countPhotoInAlbum(photoId, albumId);
                    if (count == 0) {
                        PhotoAlbum photoAlbum = new PhotoAlbum(photoId, albumId);
                        try {
                            photoAlbumDao.insert(photoAlbum);
                        } catch (Exception e) {
                            Log.e("PhotoAlbumInsertError", "Lỗi khi chèn ảnh vào album: " + e.getMessage());
                        }
                    }
                }
            }

            // 2. Kiểm tra và tạo album "Favourite" sau khi đã load xong các album khác
            String favouriteAlbumName = "Favourite";
            AlbumEntity favAlbum = albumDao.getAlbumByNameSync(favouriteAlbumName);
            if (favAlbum == null) {
                favAlbum = new AlbumEntity();
                favAlbum.setName(favouriteAlbumName);
                long favAlbumId = albumDao.insert(favAlbum);
            } else {
                Log.d("FavouriteAlbum", "Album Favourite đã tồn tại ID: " + favAlbum.getId());
            }

            // Lấy danh sách ảnh yêu thích
            List<PhotoEntity> favouritePhotos = new ArrayList<>();
            for (PhotoEntity photo : photos) {
                if (photo.isFavorite()) { // Kiểm tra ảnh có phải là yêu thích không
                    favouritePhotos.add(photo);
                }
            }

            // Thêm ảnh yêu thích vào album "Favourite"
            for (PhotoEntity photo : favouritePhotos) {
                int photoId = photo.getId();
                int albumId = favAlbum.getId();

                if (photoId <= 0 || albumId <= 0) {
                    Log.e("FavouriteAlbumError", "Photo ID hoặc Album ID không hợp lệ: Photo ID = " + photoId + ", Album ID = " + albumId);
                    continue;
                }

                // Kiểm tra xem ảnh đã tồn tại trong album "Favourite" chưa
                int count = photoAlbumDao.countPhotoInAlbum(photoId, albumId);
                if (count == 0) {
                    PhotoAlbum photoAlbum = new PhotoAlbum(photoId, albumId);
                    try {
                        photoAlbumDao.insert(photoAlbum);
                    } catch (Exception e) {
                        Log.e("FavouriteAlbumError", "Lỗi khi thêm ảnh vào album Favourite: " + e.getMessage());
                    }
                }
            }

            // 3. Cập nhật lên LiveData
            List<AlbumEntity> albumList = albumDao.getAllAlbumsSync();
            allAlbums.postValue(albumList);
        });
    }



    private String extractAlbumNameFromPath(String path) {
        File file = new File(path);
        File parent = file.getParentFile(); // Lấy thư mục cha
        return (parent != null) ? parent.getName() : null;
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
    public LiveData<PhotoEntity> getFirstPhotoOfAlbum(int albumId) {
        return photoAlbumDao.getFirstPhotoOfAlbum(albumId);
    }
    public LiveData<List<PhotoAlbum>> getPhotosByAlbumId(int albumId){
        return photoAlbumDao.getPhotosByAlbumId(albumId);
    }

    public LiveData<List<PhotoAlbum>> getNonDeletedPhotosByAlbumId(int albumId){
        return photoAlbumDao.getNonDeletedPhotosByAlbumId(albumId);
    }

    public LiveData<String> getNameByAlbumId(int albumId){
        return albumDao.getAlbumNameById(albumId);
    }
    public void getTrashAlbum(Consumer<AlbumEntity> callback) {
        executor.execute(() -> {
            AlbumEntity trashAlbum = albumDao.getAlbumByNameSync("Trash");
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(trashAlbum));
        });
    }
    public void toggleAlbumLock(int albumId, boolean isLocked) {
        executor.execute(() -> {
            albumDao.toggleAlbumLock(albumId, isLocked);
        });
    }
}