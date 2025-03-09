package matos.csu.group3.data.local.entity;

import java.util.Objects;

public class PhotoItem extends ListItem {
    private PhotoEntity photo;

    public PhotoItem(PhotoEntity photo) {
        this.photo = photo;
    }

    public PhotoEntity getPhoto() {
        return photo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoItem photoItem = (PhotoItem) o;
        return Objects.equals(photo, photoItem.photo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(photo);
    }
}
