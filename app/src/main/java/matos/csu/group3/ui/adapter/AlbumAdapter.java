package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.AlbumEntity;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<AlbumEntity> albumList;
    private OnItemClickListener listener;
    private boolean isSelectionMode = false; // Mặc định là false

    // Constructor
    public AlbumAdapter(List<AlbumEntity> albumList, OnItemClickListener listener) {
        this.albumList = albumList;
        this.listener = listener;
    }

    // Phương thức để cập nhật trạng thái isSelectionMode
    public void setSelectionMode(boolean isSelectionMode) {
        this.isSelectionMode = isSelectionMode;
        notifyDataSetChanged(); // Cập nhật lại toàn bộ RecyclerView
    }

    // Phương thức để lấy danh sách các album đã được chọn
    public List<AlbumEntity> getSelected() {
        List<AlbumEntity> selectedAlbums = new ArrayList<>();
        for (AlbumEntity album : albumList) {
            if (album.isSelected()) {
                selectedAlbums.add(album);
            }
        }
        return selectedAlbums;
    }

    // Interface để xử lý sự kiện click
    public interface OnItemClickListener {
        void onItemClick(AlbumEntity album);
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        AlbumEntity album = albumList.get(position);
        holder.bind(album, listener, isSelectionMode); // Truyền isSelectionMode vào bind
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    // Phương thức cập nhật dữ liệu
    public void updateData(List<AlbumEntity> newAlbumList) {
        this.albumList = newAlbumList;
        notifyDataSetChanged();
    }

    // ViewHolder
    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        private TextView albumNameTextView;
        private CheckBox checkBox; // Thêm CheckBox

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumNameTextView = itemView.findViewById(R.id.albumNameTextView);
            checkBox = itemView.findViewById(R.id.checkBox); // Ánh xạ CheckBox
        }

        public void bind(final AlbumEntity album, final OnItemClickListener listener, boolean isSelectionMode) {
            albumNameTextView.setText(album.getName());

            // Ẩn/hiện CheckBox dựa trên isSelectionMode
            if (isSelectionMode) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(album.isSelected()); // Cập nhật trạng thái chọn
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    album.setSelected(isChecked); // Cập nhật trạng thái chọn của album
                });
            } else {
                checkBox.setVisibility(View.GONE);
            }

            // Xử lý sự kiện click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(album);
                }
            });
        }
    }
    public List<AlbumEntity> getSelectedAlbums() {
        List<AlbumEntity> selectedAlbums = new ArrayList<>();
        for (AlbumEntity album : albumList) {
            if (album.isSelected()) {
                selectedAlbums.add(album);
            }
        }
        return selectedAlbums;
    }
}