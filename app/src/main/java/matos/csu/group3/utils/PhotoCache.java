package matos.csu.group3.utils;

import java.util.List;

import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoCache {
    private static PhotoCache instance;
    private List<PhotoEntity> photoList;

    private PhotoCache() {}

    public static synchronized PhotoCache getInstance() {
        if (instance == null) {
            instance = new PhotoCache();
        }
        return instance;
    }

    public void setPhotoList(List<PhotoEntity> photos) {
        this.photoList = photos;
    }

    public List<PhotoEntity> getPhotoList() {
        return photoList;
    }

    public void clear() {
        photoList = null;
    }
}