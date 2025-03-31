package matos.csu.group3.data.local.entity;

import androidx.room.TypeConverter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Hashtags implements Serializable {
    private List<String> Hashtags;

    public Hashtags() {
        this.Hashtags = null;
    }
    public Hashtags(List<String> hashtags) {
        this.Hashtags = hashtags;
    }

    public List<String> getHashtags() {
        return Hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.Hashtags = hashtags;
    }
}

