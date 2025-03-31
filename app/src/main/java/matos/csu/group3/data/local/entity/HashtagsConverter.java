package matos.csu.group3.data.local.entity;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HashtagsConverter {
    @TypeConverter
    public Hashtags storedStringToHashtags(String value) {
        if (value == null) {
            return new Hashtags(new ArrayList<>()); // Asserting empty hashtags for null value
        }

        List<String> hashtags = Arrays.asList(value.split("\\s*,\\s*"));
        return new Hashtags(hashtags);
    }

    @TypeConverter
    public String hashtagsToStoredString(Hashtags hashtags) {
        if (hashtags == null || hashtags.getHashtags() == null || hashtags.getHashtags().isEmpty()) {
            return null; // Asserting null for empty hashtags
        }

        StringBuilder value = new StringBuilder();
        for (String hashtag : hashtags.getHashtags()) {
            value.append(hashtag).append(",");
        }
        return value.toString();
    }
}
