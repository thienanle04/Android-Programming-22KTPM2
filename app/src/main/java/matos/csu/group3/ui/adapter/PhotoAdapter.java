package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.HeaderItem;
import matos.csu.group3.data.local.entity.ListItem;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.data.local.entity.PhotoItem;

public class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0; // Loại view cho tiêu đề ngày
    public static final int TYPE_PHOTO = 1;  // Loại view cho ảnh

    private List<ListItem> items = new ArrayList<>(); // Danh sách các mục (ngày hoặc ảnh)
    private final OnItemClickListener listener; // Listener cho sự kiện click

    public PhotoAdapter(List<ListItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof HeaderItem) {
            return TYPE_HEADER;
        } else {
            return TYPE_PHOTO;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_photo, parent, false);
            return new PhotoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            HeaderItem headerItem = (HeaderItem) items.get(position);
            ((DateHeaderViewHolder) holder).bind(headerItem.getDate());
        } else if (holder instanceof PhotoViewHolder) {
            PhotoItem photoItem = (PhotoItem) items.get(position);
            PhotoEntity photo = photoItem.getPhoto();
            if (photo != null) {
                ((PhotoViewHolder) holder).bind(photo, listener);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateTextView;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }

        public void bind(String date) {
            dateTextView.setText(date);
        }
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }

        public void bind(PhotoEntity photo, OnItemClickListener listener) {
            Glide.with(itemView.getContext())
                    .load(photo.getFilePath())
                    .into(imageView);
            itemView.setOnClickListener(v -> listener.onItemClick(photo));
        }
    }

    public interface OnItemClickListener {
        void onItemClick(PhotoEntity photo);
    }

    public void updateData(List<ListItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
}
