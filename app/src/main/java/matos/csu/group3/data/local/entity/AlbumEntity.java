package matos.csu.group3.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "albums")  // Đảm bảo tên bảng là "albums"
public class AlbumEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String createdAt;
    private Boolean isSelected = false;

    public AlbumEntity(){}
    public AlbumEntity(String name, String createdAt){
        this.name = name;
        this.createdAt = createdAt;
    }
    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public void setSelected(Boolean isSelected){
        this.isSelected = isSelected;
    }
    public Boolean isSelected(){
        return this.isSelected;
    }
}