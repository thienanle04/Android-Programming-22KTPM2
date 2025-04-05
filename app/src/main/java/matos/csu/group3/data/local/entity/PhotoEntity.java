package matos.csu.group3.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

@Entity(tableName = "photos")
public class PhotoEntity implements Serializable{

    private static final long serialVersionUID = 1L;
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String filePath;          // File path to the photo

    private String name;              // Name of the photo (e.g., image name)

    private String dateTaken;         // Date the photo was taken
    private long dateTimestamp;

    private String location;
    private Double latitude;
    private Double longitude;
    private String address;
    private int albumId;              // ID of the album to which the photo belongs
    private boolean isFavorite;       // Flag to mark the photo as a favorite
    private long size;                // Size of the photo in bytes
    private String fileFormat;        // File format (e.g., "jpg", "png")
    private String description;       // Description of the photo (optional)
    private String googleDriveId;     // Google Drive ID (optional, for syncing with Google Drive)
    private Hashtags hashtags;
    private boolean isUploaded;       // Whether the photo is uploaded to Google Drive
    private boolean isSynced;         // Whether the photo is fully synced
    private boolean isDeleted;        // Flag to mark the photo as deleted
    private boolean isHidden;
    private boolean selected;
    // Getters and Setters

    // Getter and Setter for id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getter and Setter for name
    public String getName() {
        return name != null ? name : filePath != null ? filePath.substring(filePath.lastIndexOf("/") + 1) : "Unknown";
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter and Setter for filePath
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    // Getter and Setter for dateTaken
    public String getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(String dateTaken) {
        this.dateTaken = dateTaken;
        this.dateTimestamp = convertDateToTimestamp(dateTaken);
    }
    private long convertDateToTimestamp(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            return 0; // Hoặc xử lý lỗi phù hợp
        }
    }

    // Getter and Setter for location
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Hashtags getHashtags() { return hashtags; }

    public void setHashtags(Hashtags hashtags) { this.hashtags = hashtags; }

    // Getter and Setter for albumId
    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    // Getter and Setter for isFavorite
    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    // Getter and Setter for size
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    // Getter and Setter for fileFormat
    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    // Getter and Setter for description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Getter and Setter for googleDriveId
    public String getGoogleDriveId() {
        return googleDriveId;
    }

    public void setGoogleDriveId(String googleDriveId) {
        this.googleDriveId = googleDriveId;
    }

    // Getter and Setter for isUploaded
    public boolean isUploaded() {
        return isUploaded;
    }

    public void setUploaded(boolean uploaded) {
        isUploaded = uploaded;
    }

    // Getter and Setter for isSynced
    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    // Getter and Setter for isFavorite
    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoEntity that = (PhotoEntity) o;
        return id == that.id && Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filePath);
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean isChecked) {
        this.selected = isChecked;
    }

    public long getDateTimestamp() {
        return dateTimestamp;
    }

    public void setDateTimestamp(long dateTimestamp) {
        this.dateTimestamp = dateTimestamp;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}


