package matos.csu.group3.notification;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import matos.csu.group3.repository.PhotoRepository;

public class DeletePhotosWorker extends Worker {

    public DeletePhotosWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve photo IDs from input data
        int[] photoIds = getInputData().getIntArray("photo_ids");
        if (photoIds == null || photoIds.length == 0) {
            return Result.failure();
        }

        // Delete photos from the database
        PhotoRepository photoRepository = new PhotoRepository((Application) getApplicationContext());
        for (int id : photoIds) {
            photoRepository.deletePhotoById(id);
        }

        Log.d("DeletePhotosWorker", "Permanently deleted photos from trash");
        return Result.success();
    }
}