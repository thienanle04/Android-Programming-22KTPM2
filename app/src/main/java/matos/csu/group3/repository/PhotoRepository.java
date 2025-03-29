package matos.csu.group3.repository;

import android.app.AlertDialog;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.AlbumDao;
import matos.csu.group3.data.local.dao.PhotoAlbumDao;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.notification.DeletePhotosWorker;

public class PhotoRepository {
    private final PhotoDao photoDao;
    private final PhotoAlbumDao photoAlbumDao;
    private final AlbumDao albumDao;
    private final MutableLiveData<List<PhotoEntity>> allPhotos;
    private final ExecutorService executor;
    private final Context context;
    private final Handler mainHandler;

    public PhotoRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        photoDao = database.photoDao();
        albumDao = database.albumDao();
        photoAlbumDao = database.photoAlbumDao();
        executor = Executors.newSingleThreadExecutor();  // Executor for background work
        context = application.getApplicationContext();
        allPhotos = new MutableLiveData<>();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void loadPhotos() {
        executor.execute(() -> {
            List<PhotoEntity> photoList = new ArrayList<>();

            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN
            };

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // Thêm HH:mm:ss để log thời gian chi tiết

            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC")) {

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));

                        // Lấy giá trị DATE_TAKEN từ Cursor
                        long dateTakenMillis = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
                        long dateAddedMillis = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) * 1000; // Chuyển từ giây sang mili-giây
                        long lastModifiedMillis = new File(filePath).lastModified();

                        // Kiểm tra giá trị DATE_TAKEN
                        if (dateTakenMillis <= 0 || dateTakenMillis < 946684800000L) { // 01/01/2000
                            if (lastModifiedMillis > 0) {
                                dateTakenMillis = lastModifiedMillis;
                            } else {
                                dateTakenMillis = dateAddedMillis;
                            }
                        }

                        // Sử dụng Calendar và Date để xử lý ngày tháng
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(dateTakenMillis);
                        Date date = calendar.getTime();
                        String dateTaken = sdf.format(date);

                        // Kiểm tra xem ảnh đã tồn tại trong cơ sở dữ liệu chưa
                        PhotoEntity existingPhoto = photoDao.getPhotoByFilePath(filePath);
                        if (existingPhoto == null) {
                            PhotoEntity photo = new PhotoEntity();
                            photo.setDateTaken(dateTaken);
                            photo.setName(name);
                            photo.setFilePath(filePath);
                            photoList.add(photo);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("TAG", "Error loading photos", e);
            }

            // Sắp xếp danh sách ảnh theo ngày chụp (từ mới nhất đến cũ nhất)
            photoList.sort((photo1, photo2) -> {
                try {
                    Date date1 = sdf.parse(photo1.getDateTaken());
                    Date date2 = sdf.parse(photo2.getDateTaken());
                    return Long.compare(date2.getTime(), date1.getTime()); // Sắp xếp giảm dần
                } catch (ParseException e) {
                    Log.e("TAG", "Error parsing date", e);
                    return 0;
                }
            });

            // Chỉ chèn các ảnh mới vào cơ sở dữ liệu
            if (!photoList.isEmpty()) {
                photoDao.insertPhotos(photoList);
                Log.d("TAG", "Inserted " + photoList.size() + " new photos into database");
            }

            // Cập nhật LiveData với danh sách ảnh mới nhất từ cơ sở dữ liệu
            allPhotos.postValue(photoList);
        });
    }



    public void refreshPhotos() {
//        executor.execute(this::loadPhotos);
        loadPhotos();
    }


    public void insert(PhotoEntity photoEntity) {
        executor.execute(() -> photoDao.insert(photoEntity));  // Execute database operation in background
    }

    public void update(PhotoEntity photoEntity) {
        new Thread(() -> {
            photoDao.update(photoEntity);
        }).start();
    }

    public LiveData<List<PhotoEntity>> getAllPhotos() {
        return photoDao.getAllNonDeletedPhotosSync();
    }
    public LiveData<List<PhotoEntity>> getHiddenPhotos() {
        return photoDao.getHiddenPhotos();
    }
    public void addPhotosToAlbum(int albumId, List<PhotoEntity> photos) {
        executor.execute(() -> {
            for (PhotoEntity photo : photos) {
                // Kiểm tra xem ảnh đã tồn tại trong album chưa
                int count = photoAlbumDao.countPhotoInAlbum(photo.getId(), albumId);
                if (count == 0) { // Nếu ảnh chưa tồn tại trong album
                    PhotoAlbum photoAlbum = new PhotoAlbum(photo.getId(), albumId);
                    photoAlbumDao.insert(photoAlbum);
                }
            }
        });
    }
    public List<PhotoEntity> getPhotosNotInAlbum(int currentAlbumId) throws InterruptedException, ExecutionException {
        Future<List<PhotoEntity>> future = executor.submit(() -> {
            List<PhotoEntity> allPhotosList = photoDao.getPhotosNotInAlbum(currentAlbumId);

            // Sắp xếp danh sách theo ngày
            allPhotosList.sort((photo1, photo2) -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                try {
                    Date date1 = sdf.parse(photo1.getDateTaken());
                    Date date2 = sdf.parse(photo2.getDateTaken());
                    return date2.compareTo(date1); // Sắp xếp từ mới nhất đến cũ nhất
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0; // Giữ nguyên thứ tự nếu có lỗi
                }
            });

            return allPhotosList;
        });

        // Đợi kết quả từ Future và trả về
        return future.get();
    }
    public LiveData<List<PhotoEntity>> getPhotosByAlbumId(int albumId) {
        // First get the album name to determine which query to use
        LiveData<String> albumNameLiveData = albumDao.getAlbumNameById(albumId);

        return Transformations.switchMap(albumNameLiveData, albumName -> {
            LiveData<List<PhotoEntity>> photosLiveData;

            if ("Trash".equals(albumName)) {
                // For Trash album, get all photos including deleted ones
                photosLiveData = photoDao.getPhotosByAlbumId(albumId);
            } else {
                // For other albums, get only non-deleted photos
                photosLiveData = photoDao.getNonDeletedPhotosByAlbumId(albumId);
            }

            // Sort the photos by date taken (newest first)
            return Transformations.map(photosLiveData, photosList -> {
                if (photosList == null) {
                    return new ArrayList<>();
                }

                photosList.sort((photo1, photo2) -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    try {
                        Date date1 = sdf.parse(photo1.getDateTaken());
                        Date date2 = sdf.parse(photo2.getDateTaken());
                        return date2.compareTo(date1); // Sort descending (newest first)
                    } catch (ParseException e) {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "Lỗi khi parse ngày: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                        return 0;
                    }
                });
                return photosList;
            });
        });
    }
    public LiveData<PhotoEntity> getPhotoById (int photoId) {
        return photoDao.getPhotoById(photoId);
    }
    public void addPhotosToFavourite(List<PhotoEntity> selectedPhotos) {
        executor.execute(() -> {
            // Lấy hoặc tạo album "Favourite"
            String favouriteAlbumName = "Favourite";
            AlbumEntity favAlbum = albumDao.getAlbumByNameSync(favouriteAlbumName);
            if (favAlbum == null) {
                favAlbum = new AlbumEntity();
                favAlbum.setName(favouriteAlbumName);
                long favAlbumId = albumDao.insert(favAlbum);
                Log.d("FavouriteAlbum", "Tạo album Favourite ID: " + favAlbumId);
            }

            int albumId = favAlbum.getId();

            // Duyệt qua từng ảnh được chọn
            for (PhotoEntity photo : selectedPhotos) {
                // Đánh dấu ảnh là yêu thích
                photo.setFavorite(true);

                // Cập nhật ảnh trong cơ sở dữ liệu
                photoDao.update(photo);

                // Thêm ảnh vào album "Favourite" nếu chưa tồn tại
                int photoId = photo.getId();
                int count = photoAlbumDao.countPhotoInAlbum(photoId, albumId);
                if (count == 0) {
                    PhotoAlbum photoAlbum = new PhotoAlbum(photoId, albumId);
                    photoAlbumDao.insert(photoAlbum);
                    Log.d("FavouriteAlbum", "Thêm ảnh ID " + photoId + " vào album Favourite ID " + albumId);
                }
            }
        });
    }
    public void updateFavoriteStatus(PhotoEntity photo, boolean isFavorite) {
        executor.execute(() -> {
            try {
                // Cập nhật trạng thái yêu thích của ảnh trong cơ sở dữ liệu
                photoDao.updateFavoriteStatusDirectly(photo.getId(), isFavorite);

                // Lấy hoặc tạo album "Favourite"
                String favouriteAlbumName = "Favourite";
                AlbumEntity favAlbum = albumDao.getAlbumByNameSync(favouriteAlbumName);

                if (favAlbum == null) {
                    // Nếu album "Favourite" chưa tồn tại, tạo mới
                    favAlbum = new AlbumEntity();
                    favAlbum.setName(favouriteAlbumName);
                    long favAlbumId = albumDao.insert(favAlbum);
                    Log.d("FavouriteAlbum", "Tạo album Favourite ID: " + favAlbumId);
                }

                int photoId = photo.getId();
                int albumId = favAlbum.getId();

                if (isFavorite) {
                    // Thêm ảnh vào album "Favourite" nếu chưa tồn tại
                    int count = photoAlbumDao.countPhotoInAlbum(photoId, albumId);
                    if (count == 0) {
                        PhotoAlbum photoAlbum = new PhotoAlbum(photoId, albumId);
                        photoAlbumDao.insert(photoAlbum);
                        Log.d("FavouriteAlbum", "Thêm ảnh ID " + photoId + " vào album Favourite ID " + albumId);
                    }
                } else {
                    // Xóa ảnh khỏi album "Favourite" nếu tồn tại
                    photoAlbumDao.deletePhotoFromAlbum(photoId, albumId);
                    Log.d("FavouriteAlbum", "Xóa ảnh ID " + photoId + " khỏi album Favourite ID " + albumId);
                }
            } catch (Exception e) {
                Log.e("FavoriteUpdateError", "Lỗi khi cập nhật trạng thái yêu thích: " + e.getMessage());
            }
        });
    }

    public void updateIsHidden(PhotoEntity photo, boolean isHidden) {
        executor.execute(() -> {
            try {
                photoDao.updateHiddenStatusDirectly(photo.getId(), isHidden);
            } catch (Exception e) {
                Log.e("HiddenUpdateError", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
            }
        });
    }
    public void deleteFileUsingMediaStore(Context context, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e("PhotoRepository", "File does not exist: " + filePath);
            return;
        }

        // Check for MANAGE_EXTERNAL_STORAGE permission (Android 11 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Log.e("PhotoRepository", "MANAGE_EXTERNAL_STORAGE permission not granted");
            return;
        }

        // Scan file into MediaStore if not indexed
        MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);

        Uri contentUri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns._ID};
        String selection = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = new String[]{file.getAbsolutePath()};

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
            Uri fileUri = ContentUris.withAppendedId(contentUri, id);

            int rowsDeleted = contentResolver.delete(fileUri, null, null);
            if (rowsDeleted > 0) {
                Log.d("PhotoRepository", "File deleted successfully: " + filePath);
            } else {
                Log.e("PhotoRepository", "Failed to delete file: " + filePath);
            }
            cursor.close();
        } else {
            Log.e("PhotoRepository", "File not found in MediaStore: " + filePath);
        }

        // Fallback to direct deletion if MediaStore fails
        if (file.exists() && !file.delete()) {
            Log.e("PhotoRepository", "File deletion failed using File.delete()");
        } else {
            Log.d("PhotoRepository", "File deleted using File.delete()");
        }
    }


    // Delete a photo by ID (including the file)
    public void deletePhotoById(int photoId) {
        executor.execute(() -> {
            // Get the photo entity synchronously
            PhotoEntity photo = photoDao.getPhotoByIdSync(photoId);
            if (photo != null) {
                // Delete the file from storage using MediaStore
                deleteFileUsingMediaStore(context, photo.getFilePath());

                // Delete the photo from the database
                photoDao.deletePhotoById(photoId);
            }
        });
    }


    public void movePhotosToTrash(List<PhotoEntity> photos) {
        executor.execute(() -> {
            try {
                for (PhotoEntity photo : photos) {
                    // Cập nhật trạng thái yêu thích của ảnh trong cơ sở dữ liệu
                    photo.setDeleted(true);
                    photoDao.update(photo);
                    Log.d("IsDeleted: ", "PhotoId: " + photo.getId() + " State: " + photo.isDeleted());
                    Log.d("AlbumId: ", "PhotoId: " + photo.getId() + " State: " + photo.isDeleted());

                    // Lấy hoặc tạo album "Favourite"
                    String trashAlbumName = "Trash";
                    AlbumEntity trashAlbum = albumDao.getAlbumByNameSync(trashAlbumName);

                    if (trashAlbum == null) {
                        // Nếu album "Trash" chưa tồn tại, tạo mới
                        trashAlbum = new AlbumEntity();
                        trashAlbum.setName(trashAlbumName);
                        long trashAlbumId = albumDao.insert(trashAlbum);
                        Log.d("TrashAlbum", "Tạo album Trash ID: " + trashAlbumId);
                    }

                    int photoId = photo.getId();
                    int albumId = trashAlbum.getId();

                    // Thêm ảnh vào album "Trash" nếu chưa tồn tại
                    int count = photoAlbumDao.countPhotoInAlbum(photoId, albumId);
                    if (count == 0) {
                        PhotoAlbum photoAlbum = new PhotoAlbum(photoId, albumId);
                        photoAlbumDao.insert(photoAlbum);
                        Log.d("TrashAlbum", "Thêm ảnh ID " + photoId + " vào album Trash ID " + albumId);
                    }
                }
            } catch (Exception e) {
                Log.e("TrashUpdateError", "Lỗi khi cập nhật trạng thái xoa: " + e.getMessage());
            }
        });
    }

    public void schedulePermanentDeletion(List<PhotoEntity> photos, Context context) {
        for (PhotoEntity photo : photos) {
            Data inputData = new Data.Builder()
                    .putInt("photo_id", photo.getId())
                    .build();

            String workTag = "delete_photo_" + photo.getId();

            OneTimeWorkRequest deleteWorkRequest = new OneTimeWorkRequest.Builder(DeletePhotosWorker.class)
                    .setInputData(inputData)
                    .addTag(workTag)  // Unique tag for each photo
                    .setInitialDelay(3, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(context).enqueue(deleteWorkRequest);
            Log.d("WorkScheduling", "Scheduled deletion for photo: " + photo.getId());
        }
    }

    //Restore photos
    public void cancelScheduledDeletion(List<PhotoEntity> photos, Context context) {
        WorkManager workManager = WorkManager.getInstance(context);

        // Create unique tags for each photo's deletion work
        for (PhotoEntity photo : photos) {
            String workTag = "delete_photo_" + photo.getId();
            // Cancel all work with this tag
            workManager.cancelAllWorkByTag(workTag);

            // Cancel by unique work name
            String workName = "delete_photo_" + photo.getId();
            workManager.cancelUniqueWork(workName);

            Log.d("WorkCancellation", "Cancelled work for photo: " + photo.getId());
        }
    }

    public void restoreFromTrash(List<PhotoEntity> photos, int trashAlbumId, Runnable onComplete) {
        executor.execute(() -> {
            try {
                for (PhotoEntity photo : photos) {
                    // 1. Cancel scheduled deletion FIRST
                    cancelScheduledDeletion(Collections.singletonList(photo), context);

                    // 2. Remove from Trash album
                    photoAlbumDao.deletePhotoFromAlbum(photo.getId(), trashAlbumId);

                    // 3. Set isDeleted to false
                    photo.setDeleted(false);
                    photoDao.update(photo);

                    Log.d("PhotoRestore", "Restored photo ID: " + photo.getId());
                }
                new Handler(Looper.getMainLooper()).post(onComplete);
            } catch (Exception e) {
                Log.e("PhotoRestore", "Error restoring photo", e);
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }
}
