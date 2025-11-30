package com.example.tiktok_publish_demo_202511;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_ADD = 2;

    private final Context context;
    private final List<Uri> images = new ArrayList<>();
    private final int MAX_COUNT = 9;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAddClick();
        void onImageClick(int position, Uri uri);
    }

    public ImageAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void addImages(List<Uri> uris) {
        if (images.size() + uris.size() > MAX_COUNT) {
            return;
        }
        images.addAll(uris);
        notifyDataSetChanged();
    }

    // 核心修改：拖拽交换数据
    public void onItemMove(int fromPosition, int toPosition) {
        // 安全检查：防止越界
        if (fromPosition < images.size() && toPosition < images.size()) {
            Collections.swap(images, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }
    }

    public List<Uri> getData() {
        return images;
    }

    @Override
    public int getItemViewType(int position) {
        // 如果当前位置等于图片数量，说明是“+”号按钮
        if (position == images.size() && images.size() < MAX_COUNT) {
            return TYPE_ADD;
        }
        return TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD) {
            AddViewHolder addHolder = (AddViewHolder) holder;
            addHolder.ivThumb.setImageDrawable(null);
            addHolder.tvAddIcon.setVisibility(View.VISIBLE);
            addHolder.btnDelete.setVisibility(View.GONE);
            addHolder.itemView.setOnClickListener(v -> listener.onAddClick());
        } else {
            ImageViewHolder imgHolder = (ImageViewHolder) holder;
            Uri uri = images.get(position);

            imgHolder.tvAddIcon.setVisibility(View.GONE);
            imgHolder.btnDelete.setVisibility(View.VISIBLE);
            Glide.with(context).load(uri).centerCrop().into(imgHolder.ivThumb);

            imgHolder.btnDelete.setOnClickListener(v -> {
                int pos = imgHolder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    images.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, getItemCount());
                }
            });
            imgHolder.itemView.setOnClickListener(v -> listener.onImageClick(imgHolder.getAdapterPosition(), uri));
        }
    }

    @Override
    public int getItemCount() {
        return images.size() < MAX_COUNT ? images.size() + 1 : images.size();
    }

    // ViewHolder for Images
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        ImageView btnDelete;
        TextView tvAddIcon;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvAddIcon = itemView.findViewById(R.id.tv_add_icon);
        }
    }

    // ViewHolder for Add Button
    static class AddViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        ImageView btnDelete;
        TextView tvAddIcon;

        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvAddIcon = itemView.findViewById(R.id.tv_add_icon);
        }
    }
}