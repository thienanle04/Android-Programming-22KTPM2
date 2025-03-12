package matos.csu.group3.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import matos.csu.group3.data.local.entity.PhotoAlbum;

@Dao
public interface PhotoAlbumDao {
    @Insert
    void insert(PhotoAlbum photoAlbum);

    @Query("DELETE FROM photo_album WHERE photoId = :photoId AND albumId = :albumId")
    void deletePhotoFromAlbum(int photoId, int albumId);

    @Query("SELECT * FROM photo_album WHERE albumId = :albumId")
    List<PhotoAlbum> getPhotosByAlbumId(int albumId);

    @Query("SELECT * FROM photo_album WHERE photoId = :photoId")
    List<PhotoAlbum> getAlbumsByPhotoId(int photoId);
    @Query("SELECT COUNT(*) FROM photo_album WHERE photoId = :photoId AND albumId = :albumId")
    int countPhotoInAlbum(int photoId, int albumId);
}