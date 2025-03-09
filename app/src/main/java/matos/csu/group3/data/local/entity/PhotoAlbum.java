package matos.csu.group3.data.local.entity;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "photo_album",
        primaryKeys = {"photoId", "albumId"},
        foreignKeys = {
                @ForeignKey(entity = PhotoEntity.class,
                        parentColumns = "id",
                        childColumns = "photoId",
                        onDelete = CASCADE),
                @ForeignKey(entity = AlbumEntity.class,
                        parentColumns = "id",
                        childColumns = "albumId",
                        onDelete = CASCADE)
        })
public class PhotoAlbum {
    private final int photoId;
    private final int albumId;

    public PhotoAlbum(int photoId, int albumId) {
        this.photoId = photoId;
        this.albumId = albumId;
    }

    public int getPhotoId() {
        return photoId;
    }

    public int getAlbumId() {
        return albumId;
    }
}