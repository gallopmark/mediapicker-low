package com.holike.mediapicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import pony.xcode.media.MediaPicker;
import pony.xcode.media.bean.MediaBean;

public class MainActivity extends AppCompatActivity {

    private List<String> images = new ArrayList<>();
    private ImageAdapter mAdapter;

    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.MyViewHolder> {
        private Context context;
        private List<String> images;

        ImageAdapter(Context context, List<String> images) {
            this.context = context;
            this.images = images;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_image, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Glide.with(context).load(images.get(position)).centerCrop().into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return images == null ? 0 : images.size();
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.iv);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mAdapter = new ImageAdapter(this, images);
        recyclerView.setAdapter(mAdapter);
        MediaPicker.builder().maxSelectCount(10)
//                .sizeLimit(5 * 1024 * 1024)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MediaPicker.DEFAULT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<MediaBean> mediaBeans = data.getParcelableArrayListExtra(MediaPicker.SELECT_RESULT);
            if (mediaBeans != null && !mediaBeans.isEmpty()) {
                for (MediaBean bean : mediaBeans) {
                    images.add(bean.getPath());
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }
}
