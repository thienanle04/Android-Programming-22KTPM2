package matos.csu.group3.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.HeaderItem;
import matos.csu.group3.data.local.entity.ListItem;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.data.local.entity.PhotoItem;
import matos.csu.group3.ui.adapter.HashtagChecklistAdapter;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class SearchActivity extends AppCompatActivity implements HashtagChecklistAdapter.OnHashtagSelectionChanged {
    private PhotoViewModel photoViewModel;
    private PhotoAdapter photoAdapter;
    private HashtagChecklistAdapter hashtagAdapter;

    private List<PhotoEntity> photoList;
    private Set<String> selectedHashtags = new HashSet<>();

    private RecyclerView hashtagRecyclerView;
    private RecyclerView searchResultsRecyclerView;
    private EditText searchEditText;
    private ImageButton btnBackFromSearch;
    private ImageButton btnClearSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Find views
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        hashtagRecyclerView = findViewById(R.id.hashtagsChecklist);
        searchEditText = findViewById(R.id.imageSearch);
        btnBackFromSearch = findViewById(R.id.btnBackFromSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        // Setup RecyclerViews
        setupRecyclerView();

        // Setup ViewModel
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.getAllPhotos().observe(this, photoEntities -> {
            if (photoEntities != null && !photoEntities.isEmpty()) {
                photoList = photoEntities;
                updateHashtagsRecommendation();
                // Perform an initial filter
                filterPhotos(searchEditText.getText().toString());
            }
        });

        setupSearchFunctionality();

        btnBackFromSearch.setOnClickListener(v -> finish());
        btnClearSearch.setOnClickListener(v -> searchEditText.setText(""));
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (photoAdapter.getItemViewType(position) == PhotoAdapter.TYPE_HEADER) {
                    return 3;
                }
                return 1;
            }
        });
        searchResultsRecyclerView.setLayoutManager(layoutManager);

        photoAdapter = new PhotoAdapter(
                new ArrayList<>(),
                photo -> {
                    Intent intent = new Intent(SearchActivity.this, DisplaySinglePhotoActivity.class);
                    intent.putExtra("photoEntity", photo);
                    startActivity(intent);
                },
                null,
                null
        );
        searchResultsRecyclerView.setAdapter(photoAdapter);
        LinearLayoutManager hashtagLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        hashtagRecyclerView.setLayoutManager(hashtagLayoutManager);

        hashtagAdapter = new HashtagChecklistAdapter(new ArrayList<>(), this);
        hashtagRecyclerView.setAdapter(hashtagAdapter);
    }

    private void updateHashtagsRecommendation() {
        if (photoList == null) return;

        Set<String> uniqueHashtags = new HashSet<>();
        for (PhotoEntity photo : photoList) {
            if (photo.getHashtags() != null && photo.getHashtags().getHashtags() != null) {
                uniqueHashtags.addAll(photo.getHashtags().getHashtags());
            }
        }

        List<String> hashtagList = new ArrayList<>(uniqueHashtags);
        hashtagAdapter.updateData(hashtagList);
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPhotos(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            filterPhotos(searchEditText.getText().toString());
            return true;
        });
    }

    private void filterPhotos(String query) {
        if (photoList == null) return;

        String lowerQuery = query.toLowerCase(Locale.getDefault());
        List<PhotoEntity> filteredPhotos = new ArrayList<>();

        Log.i("FilterPhoto", selectedHashtags.toString());
        for (PhotoEntity photo : photoList) {
            boolean nameMatch = false;
            if (photo.getName() != null && !photo.getName().isEmpty()) {
                nameMatch = photo.getName().toLowerCase().contains(lowerQuery);
            }
            boolean hashtagsMatch = false;
            if (photo.getHashtags() != null && photo.getHashtags().getHashtags() != null) {
                List<String> photoHashtags = photo.getHashtags().getHashtags();
                if (selectedHashtags.isEmpty()) {
                    hashtagsMatch = true;
                } else {
                    hashtagsMatch = photoHashtags.containsAll(selectedHashtags);
                }
            }

            if (hashtagsMatch)
                Log.i("FilterPhoto", "Status for: " + photo + ": " + hashtagsMatch);

            if (nameMatch && hashtagsMatch) {
                filteredPhotos.add(photo);
            }
        }

        Map<String, List<PhotoEntity>> photosByDate = groupPhotosByDate(filteredPhotos);
        List<ListItem> groupedList = convertToGroupedList(photosByDate);
        photoAdapter.updateData(groupedList);

        if (filteredPhotos.isEmpty()) {
            Toast.makeText(this, "No photos found matching your criteria", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterPhotos() {
        filterPhotos(searchEditText.getText().toString());
    }

    @Override
    public void onSelectionChanged(Set<String> newSelection) {
        Log.i("SearchActivity", "User changed hashtag selection: " + newSelection);
        selectedHashtags = newSelection;
        filterPhotos();
    }

    private Map<String, List<PhotoEntity>> groupPhotosByDate(List<PhotoEntity> photos) {
        Map<String, List<PhotoEntity>> photosByDate = new LinkedHashMap<>();

        for (PhotoEntity photo : photos) {
            if (photo != null) {
                String date = photo.getDateTaken();
                if (!photosByDate.containsKey(date)) {
                    photosByDate.put(date, new ArrayList<>());
                }
                photosByDate.get(date).add(photo);
            }
        }

        List<String> sortedDates = new ArrayList<>(photosByDate.keySet());
        sortedDates.sort((date1, date2) -> {
            try {
                long date1Millis =
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date1).getTime();
                long date2Millis =
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date2).getTime();
                return Long.compare(date2Millis, date1Millis); // descending
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });

        Map<String, List<PhotoEntity>> sortedPhotosByDate = new LinkedHashMap<>();
        for (String date : sortedDates) {
            sortedPhotosByDate.put(date, photosByDate.get(date));
        }

        for (List<PhotoEntity> group : sortedPhotosByDate.values()) {
            group.sort((p1, p2) -> {
                try {
                    long time1 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .parse(p1.getDateTaken()).getTime();
                    long time2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .parse(p2.getDateTaken()).getTime();
                    return Long.compare(time2, time1);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            });
        }

        return sortedPhotosByDate;
    }

    private List<ListItem> convertToGroupedList(Map<String, List<PhotoEntity>> photosByDate) {
        List<ListItem> groupedList = new ArrayList<>();
        for (Map.Entry<String, List<PhotoEntity>> entry : photosByDate.entrySet()) {
            groupedList.add(new HeaderItem(entry.getKey()));
            for (PhotoEntity photo : entry.getValue()) {
                groupedList.add(new PhotoItem(photo));
            }
        }
        return groupedList;
    }
}