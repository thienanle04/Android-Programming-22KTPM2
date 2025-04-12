package matos.csu.group3.data.local.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Hashtags implements Serializable {
    private List<String> hashtags;

    public Hashtags() {
        this.hashtags = new ArrayList<>();
    }

    public Hashtags(List<String> hashtags) {
        this.hashtags = hashtags != null ? new ArrayList<>(hashtags) : new ArrayList<>(); // Ensure a mutable list
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags != null ? new ArrayList<>(hashtags) : new ArrayList<>();
    }
}