package matos.csu.group3.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.repository.AlbumRepository;

public class AlbumViewModel extends AndroidViewModel {

    private final AlbumRepository albumRepository;
    private final MutableLiveData<List<AlbumEntity>> allAlbums;
    private final MutableLiveData<PhotoEntity> firstPhoto = new MutableLiveData<>();

    public AlbumViewModel(@NonNull Application application) {
        super(application);
        albumRepository = new AlbumRepository(application);
        allAlbums = albumRepository.getAllAlbums();
    }

    public void refreshAlbums() {
        albumRepository.refreshAlbums();
    }
    public LiveData<List<AlbumEntity>> getAllAlbums() {
        return allAlbums;
    }

    public void insert(AlbumEntity album) {
        albumRepository.insert(album);
    }

    public void update(AlbumEntity album) {
        albumRepository.update(album);
    }
    public void delete(int albumId) {
        albumRepository.delete(albumId);
    }
    public LiveData<PhotoEntity> getFirstPhotoOfAlbum(int albumId) {
        albumRepository.getFirstPhotoOfAlbum(albumId).observeForever(photoEntity -> {
            firstPhoto.postValue(photoEntity);
        });
        return firstPhoto;
    }
    public LiveData<List<PhotoAlbum>> getPhotosByAlbumId(int albumId){
        return albumRepository.getPhotosByAlbumId(albumId);
    }
    public LiveData<List<PhotoAlbum>> getNonDeletedPhotosByAlbumId(int albumId){
        return albumRepository.getNonDeletedPhotosByAlbumId(albumId);
    }
}