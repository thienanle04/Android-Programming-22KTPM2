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
        int photoId = getInputData().getInt("photo_id", -1);
        if (photoId == -1) {
            return Result.failure();
        }

        PhotoRepository photoRepository = new PhotoRepository((Application) getApplicationContext());
        photoRepository.deletePhotoById(photoId);

        Log.d("DeletePhotosWorker", "Deleted photo ID: " + photoId);
        return Result.success();
    }
}