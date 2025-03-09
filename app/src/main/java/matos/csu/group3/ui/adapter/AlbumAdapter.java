package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.AlbumEntity;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<AlbumEntity> albumList;
    private OnItemClickListener listener;

    // Constructor
    public AlbumAdapter(List<AlbumEntity> albumList, OnItemClickListener listener) {
        this.albumList = albumList;
        this.listener = listener;
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
        holder.bind(album, listener);
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    // Phương thức cập nhật dữ liệu
    public void updateData(List<AlbumEntity> newAlbumList) {
        this.albumList = newAlbumList;  // Cập nhật danh sách mới
        notifyDataSetChanged();  // Thông báo cho RecyclerView biết dữ liệu đã thay đổi
    }

    // ViewHolder
    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        private TextView albumNameTextView;
        private TextView createdAtTextView;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumNameTextView = itemView.findViewById(R.id.albumNameTextView);
        }

        public void bind(final AlbumEntity album, final OnItemClickListener listener) {
            albumNameTextView.setText(album.getName());

            // Xử lý sự kiện click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(album);
                }
            });
        }
    }
}