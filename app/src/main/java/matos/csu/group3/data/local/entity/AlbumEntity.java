package matos.csu.group3.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "albums")
public class AlbumEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String createdAt;

    // Getters & Setters
}
