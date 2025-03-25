package matos.csu.group3.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;

@Dao
public interface PhotoAlbumDao {
    @Insert
    void insert(PhotoAlbum photoAlbum);

    @Query("DELETE FROM photo_album WHERE photoId = :photoId AND albumId = :albumId")
    void deletePhotoFromAlbum(int photoId, int albumId);

    @Query("SELECT * FROM photo_album WHERE albumId = :albumId")
    LiveData<List<PhotoAlbum>> getPhotosByAlbumId(int albumId);

    @Query("SELECT pa.* FROM photo_album pa " +
            "INNER JOIN photos p ON pa.photoId = p.id " +
            "WHERE pa.albumId = :albumId AND p.isDeleted != TRUE")
    LiveData<List<PhotoAlbum>> getNonDeletedPhotosByAlbumId(int albumId);

    @Query("SELECT * FROM photo_album WHERE photoId = :photoId")
    List<PhotoAlbum> getAlbumsByPhotoId(int photoId);
    @Query("SELECT COUNT(*) FROM photo_album WHERE photoId = :photoId AND albumId = :albumId")
    int countPhotoInAlbum(int photoId, int albumId);
    @Query("SELECT p.* FROM photos p " +
            "INNER JOIN photo_album pa ON p.id = pa.photoId " +
            "WHERE pa.albumId = :albumId " +
            "ORDER BY p.id ASC LIMIT 1")
    LiveData<PhotoEntity> getFirstPhotoOfAlbum(int albumId);
}