package matos.csu.group3.data.local.entity;

public class PhotoItem extends ListItem {
    private PhotoEntity photo;

    public PhotoItem(PhotoEntity photo) {
        this.photo = photo;
    }

    public PhotoEntity getPhoto() {
        return photo;
    }
}
