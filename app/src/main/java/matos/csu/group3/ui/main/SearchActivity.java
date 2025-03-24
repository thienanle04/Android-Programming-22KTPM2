package matos.csu.group3.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.HeaderItem;
import matos.csu.group3.data.local.entity.ListItem;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.data.local.entity.PhotoItem;
import matos.csu.group3.ui.adapter.PhotoAdapter;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class SearchActivity extends AppCompatActivity {
    private PhotoViewModel photoViewModel;
    private PhotoAdapter photoAdapter;
    private List<PhotoEntity> photoList;
    private RecyclerView searchResultsRecyclerView;
    private EditText searchEditText;
    private ImageButton btnBackFromSearch;
    private ImageButton btnClearSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchEditText = findViewById(R.id.imageSearch);
        btnBackFromSearch = findViewById(R.id.btnBackFromSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        setupRecyclerView();

        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.getAllPhotos().observe(this, photoEntities -> {
            if (photoEntities != null && !photoEntities.isEmpty()) {
                photoList = photoEntities;
            }
        });

        setupSearchFunctionality();

        btnBackFromSearch.setOnClickListener(v -> finish());
        btnClearSearch.setOnClickListener(v -> searchEditText.setText(""));
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){
            @Override
            public int getSpanSize(int position) {
                if (photoAdapter.getItemViewType(position) == PhotoAdapter.TYPE_HEADER)
                    return 3;
                return 1;
            }
        });

        searchResultsRecyclerView.setLayoutManager(layoutManager);

        photoAdapter = new PhotoAdapter(new ArrayList<>(),
                photo -> {
                    Intent intent = new Intent(SearchActivity.this, DisplaySinglePhotoActivity.class);
                    intent.putExtra("photoEntity", photo);
                    startActivity(intent);
                },
                null,
                null
        );
        searchResultsRecyclerView.setAdapter(photoAdapter);
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPhotos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            filterPhotos(searchEditText.getText().toString());
            return true;
        });
    }

    private void filterPhotos(String query) {
        if (photoList == null) return;

        List<PhotoEntity> filteredPhotos = new ArrayList<>();
        for (PhotoEntity photo : photoList) {
            boolean nameMatch = photo.getName() != null && photo.getName().toLowerCase().contains(query.toLowerCase());
            boolean hashtagMatch = false;
            if (photo.getHashtags() != null && photo.getHashtags().getHashtags() != null) {
                for (String hashtag : photo.getHashtags().getHashtags()) {
                    if (hashtag.toLowerCase().contains(query.toLowerCase())) {
                        hashtagMatch = true;
                        break;
                    }
                }
            }

            if (nameMatch || hashtagMatch) {
                filteredPhotos.add(photo);
            }
        }

        Map<String, List<PhotoEntity>> photosByDate = groupPhotosByDate(filteredPhotos);
        List<ListItem> groupedList = convertToGroupedList(photosByDate);
        photoAdapter.updateData(groupedList);

        if (filteredPhotos.isEmpty()) {
            Toast.makeText(this, "No photos found matching your search", Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, List<PhotoEntity>> groupPhotosByDate(List<PhotoEntity> photos) {
        // Sử dụng LinkedHashMap để giữ nguyên thứ tự các ngày
        Map<String, List<PhotoEntity>> photosByDate = new LinkedHashMap<>();

        // Nhóm ảnh theo ngày
        for (PhotoEntity photo : photos) {
            if (photo != null) {
                String date = photo.getDateTaken(); // Lấy ngày từ PhotoEntity
                if (!photosByDate.containsKey(date)) {
                    photosByDate.put(date, new ArrayList<>());
                }
                photosByDate.get(date).add(photo);
            }
        }

        // Sắp xếp các ngày (keys của Map) từ mới nhất đến cũ nhất
        List<String> sortedDates = new ArrayList<>(photosByDate.keySet());
        sortedDates.sort((date1, date2) -> {
            // Chuyển đổi ngày thành số để so sánh (giả sử định dạng ngày là "dd/MM/yyyy")
            long date1Millis = 0, date2Millis = 0;
            try {
                date1Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date1).getTime();
                date2Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date2).getTime();

            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            return Long.compare(date2Millis, date1Millis); // Sắp xếp từ mới nhất đến cũ nhất
        });

        // Tạo một LinkedHashMap mới với thứ tự các ngày đã được sắp xếp
        Map<String, List<PhotoEntity>> sortedPhotosByDate = new LinkedHashMap<>();
        for (String date : sortedDates) {
            sortedPhotosByDate.put(date, photosByDate.get(date));
        }

        // Sắp xếp các ảnh trong từng nhóm theo thời gian chụp (từ mới nhất đến cũ nhất)
        for (List<PhotoEntity> photoList : sortedPhotosByDate.values()) {
            photoList.sort((photo1, photo2) -> {
                // Giả sử PhotoEntity có phương thức getDateTaken() trả về định dạng "dd/MM/yyyy"
                long date1Millis = 0, date2Millis = 0;
                try {
                    date1Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(photo1.getDateTaken()).getTime();
                    date2Millis = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(photo2.getDateTaken()).getTime();

                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return Long.compare(date2Millis, date1Millis); // Sắp xếp từ mới nhất đến cũ nhất
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
