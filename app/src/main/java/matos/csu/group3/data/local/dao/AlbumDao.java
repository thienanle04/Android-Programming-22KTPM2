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

    @Insert(onConflict = OnConflictStrategy.REPLACE)  // Đảm bảo có @Insert
    void insert(AlbumEntity album);

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
}