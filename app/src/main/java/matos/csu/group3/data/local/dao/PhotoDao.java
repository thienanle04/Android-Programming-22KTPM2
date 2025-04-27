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

    // Get all non-hidden photos
    @Query("SELECT * FROM photos WHERE isHidden != 1 ORDER BY id DESC")
    LiveData<List<PhotoEntity>> getAllPhotos();

    // Get a non-hidden photo by ID
    @Query("SELECT * FROM photos WHERE id = :id")
    LiveData<PhotoEntity> getPhotoById(int id);

    @Query("SELECT * FROM photos WHERE filePath = :filePath LIMIT 1")
    PhotoEntity getPhotoByFilePath(String filePath);

    // Get non-hidden photos belonging to a specific album
    @Query("SELECT * FROM photos WHERE id IN (SELECT photoId FROM photo_album WHERE albumId = :albumId) " +
            "AND isHidden != 1 ORDER BY dateTimestamp DESC")
    LiveData<List<PhotoEntity>> getPhotosByAlbumId(int albumId);

    @Query("SELECT p.* FROM photos p " +
            "INNER JOIN photo_album pa ON p.id = pa.photoId " +
            "WHERE pa.albumId = :albumId AND p.isDeleted != 1 AND p.isHidden != 1 " +
            "ORDER BY p.dateTimestamp DESC")
    LiveData<List<PhotoEntity>> getNonDeletedPhotosByAlbumId(int albumId);

    // Get non-hidden photos not in a specific album
    @Query("SELECT * FROM photos WHERE id NOT IN (SELECT photoId FROM photo_album WHERE albumId = :currentAlbumId) " +
            "AND isHidden != 1")
    List<PhotoEntity> getPhotosNotInAlbum(int currentAlbumId);

    @Query("SELECT * FROM photos WHERE isHidden != 1 ORDER BY id DESC")
    List<PhotoEntity> getAllPhotosSync();

    @Query("SELECT * FROM photos WHERE isDeleted != 1 ORDER BY id DESC")
    List<PhotoEntity> getAllNonDeletedPhotos();

    @Query("SELECT * FROM photos WHERE isDeleted != 1 AND isHidden != 1 ORDER BY dateTimestamp DESC")
    LiveData<List<PhotoEntity>> getAllNonDeletedPhotosSync();

    // Delete a photo by ID
    @Query("DELETE FROM photos WHERE id = :photoId")
    void deletePhotoById(int photoId);

    // Get a photo by ID (synchronous)
    @Query("SELECT * FROM photos WHERE id = :id")
    PhotoEntity getPhotoByIdSync(int id);

    // NEW: Get all hidden photos
    @Query("SELECT * FROM photos WHERE isHidden = 1 ORDER BY dateTimestamp DESC")
    LiveData<List<PhotoEntity>> getHiddenPhotos();

    // NEW: Get all hidden photos synchronously
    @Query("SELECT * FROM photos WHERE isHidden = 1 ORDER BY dateTimestamp DESC")
    List<PhotoEntity> getHiddenPhotosSync();
    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id = :photoId")
    void updateFavoriteStatusDirectly(int photoId, boolean isFavorite);
    @Query("UPDATE photos SET isHidden = :isHidden WHERE id = :photoId")
    void updateHiddenStatusDirectly(int photoId, boolean isHidden);
    @Query("UPDATE photos SET isUploaded = :isUpload WHERE id = :photoId")
    void updateUploadStatus(int photoId, boolean isUpload);
}