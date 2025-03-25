package matos.csu.group3.utils;

import java.util.List;
import java.util.stream.Collectors;

import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoCache {
    private static PhotoCache instance;
    private List<Integer> photoListIds;

    private PhotoCache() {}

    public static synchronized PhotoCache getInstance() {
        if (instance == null) {
            instance = new PhotoCache();
        }
        return instance;
    }

    public void setPhotoList(List<PhotoEntity> photos) {
        this.photoListIds = photos.stream().map(PhotoEntity::getId).collect(Collectors.toList());
    }

    public List<Integer> getPhotoListIds() {
        return photoListIds;
    }

    public void clear() {
        photoListIds = null;
    }
}