package matos.csu.group3.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import matos.csu.group3.data.local.dao.AlbumDao;
import matos.csu.group3.data.local.dao.PhotoAlbumDao;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.HashtagsConverter;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.data.local.entity.AlbumEntity;  // Import AlbumEntity

@Database(entities = {PhotoEntity.class, AlbumEntity.class, PhotoAlbum.class}, version = 2, exportSchema = false)  // Thêm AlbumEntity vào danh sách entities
@TypeConverters({HashtagsConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "photo_database";

    // Abstract method to get DAO
    public abstract PhotoDao photoDao();
    public abstract AlbumDao albumDao();  // Thêm phương thức trừu tượng để truy cập AlbumDao
    public abstract PhotoAlbumDao photoAlbumDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()  // Xóa và tạo lại database nếu schema thay đổi
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}