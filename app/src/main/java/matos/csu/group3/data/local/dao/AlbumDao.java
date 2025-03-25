package matos.csu.group3.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import matos.csu.group3.data.local.entity.AlbumEntity;

@Dao  // Đảm bảo có @Dao
public interface AlbumDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)  // Đảm bảo có @Insert
    long insert(AlbumEntity album);

    @Update  // Đảm bảo có @Update
    void update(AlbumEntity album);

    @Query("DELETE FROM albums WHERE id = :albumId")  // Đảm bảo có @Query
    void deleteById(int albumId);

    @Query("SELECT * FROM albums ORDER BY createdAt DESC")  // Đảm bảo có @Query
    LiveData<List<AlbumEntity>> getAllAlbums();

    @Query("SELECT * FROM albums ORDER BY createdAt DESC")  // Đảm bảo có @Query
    List<AlbumEntity> getAllAlbumsSync();

    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    LiveData<AlbumEntity> getAlbumById(int albumId);
    @Query("SELECT * FROM albums WHERE name = :albumName LIMIT 1")
    AlbumEntity getAlbumByNameSync(String albumName);

    @Query("SELECT name FROM albums WHERE id = :albumId LIMIT 1")
    LiveData<String> getAlbumNameById(int albumId);

    @Query("SELECT id FROM albums WHERE name = :albumName LIMIT 1")
    LiveData<Integer> getAlbumIdByName(String albumName);
}