package matos.csu.group3.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.repository.PhotoRepository;
public class PhotoViewModel extends AndroidViewModel {
    private final PhotoRepository repository;
    private final LiveData<List<PhotoEntity>> allPhotos;

    public PhotoViewModel(@NonNull Application application) {
        super(application);
        repository = new PhotoRepository(application);
        allPhotos = repository.getAllPhotos();
    }

    public void refreshPhotos() {
        repository.refreshPhotos();
    }


    public void insert(PhotoEntity photoEntity) {
        repository.insert(photoEntity);
    }

    public LiveData<List<PhotoEntity>> getAllPhotos() {
        return allPhotos;
    }
}
