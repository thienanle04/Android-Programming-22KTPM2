package matos.csu.group3.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import matos.csu.group3.data.local.entity.PhotoEntity;

@Dao
public interface PhotoDao {

    // Insert a new photo
    @Insert
    void insert(PhotoEntity photoEntity);

    // Insert multiple photos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPhotos(List<PhotoEntity> photoEntities);

    // Update an existing photo
    @Update
    void update(PhotoEntity photoEntity);

    // Delete a photo
    @Delete
    void delete(PhotoEntity photoEntity);

    // Get all photos
    @Query("SELECT * FROM photos ORDER BY id DESC")
    LiveData<List<PhotoEntity>> getAllPhotos();

    // Get a photo by ID
    @Query("SELECT * FROM photos WHERE id = :id")
    LiveData<PhotoEntity> getPhotoById(int id);
    @Query("SELECT * FROM photos WHERE filePath = :filePath LIMIT 1")
    PhotoEntity getPhotoByFilePath(String filePath);
    // Lấy danh sách ảnh thuộc một album cụ thể
    @Query("SELECT * FROM photos WHERE id IN (SELECT photoId FROM photo_album WHERE albumId = :albumId)")
    LiveData<List<PhotoEntity>> getPhotosByAlbumId(int albumId);

    // Lấy danh sách ảnh không thuộc album cụ thể
    @Query("SELECT * FROM photos WHERE id NOT IN (SELECT photoId FROM photo_album WHERE albumId = :currentAlbumId)")
    List<PhotoEntity> getPhotosNotInAlbum(int currentAlbumId);
    @Query("SELECT * FROM photos ORDER BY id DESC")
    List<PhotoEntity> getAllPhotosSync();
    @Query("SELECT * FROM photos WHERE isDeleted != TRUE ORDER BY id DESC")
    LiveData<List<PhotoEntity>> getAllNonDeletedPhotosSync();
    // Delete a photo by ID
    @Query("DELETE FROM photos WHERE id = :photoId")
    void deletePhotoById(int photoId);

    // Get a photo by ID (synchronous)
    @Query("SELECT * FROM photos WHERE id = :id")
    PhotoEntity getPhotoByIdSync(int id);
}

