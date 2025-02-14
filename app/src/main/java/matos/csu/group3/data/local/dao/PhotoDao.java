package matos.csu.group3.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
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
    @Insert
    void insertAll(List<PhotoEntity> photoEntities);

    // Update an existing photo
    @Update
    void update(PhotoEntity photoEntity);

    // Delete a photo
    @Delete
    void delete(PhotoEntity photoEntity);

    // Get all photos
    @Query("SELECT * FROM photos")
    LiveData<List<PhotoEntity>> getAllPhotos();

    // Get a photo by ID
    @Query("SELECT * FROM photos WHERE id = :id")
    PhotoEntity getPhotoById(int id);
}

