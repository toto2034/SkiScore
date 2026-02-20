package it.unisa.skiscore.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import it.unisa.skiscore.R;
import it.unisa.skiscore.model.WebcamModel;

/**
 * RecyclerView adapter for webcam cards.
 *
 * Behaviour by StreamType:
 *   IMAGE      → Glide loads image directly
 *   YOUTUBE    → Glide loads YouTube thumbnail; click opens YouTube app
 *   VIDEO_MP4  → shows placeholder + play icon; click opens system video player
 */
public class WebcamAdapter extends RecyclerView.Adapter<WebcamAdapter.WebcamViewHolder> {

    private List<WebcamModel> webcams = new ArrayList<>();

    public void setWebcams(List<WebcamModel> webcams) {
        this.webcams = webcams != null ? webcams : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WebcamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_webcam, parent, false);
        return new WebcamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WebcamViewHolder holder, int position) {
        holder.bind(webcams.get(position));
    }

    @Override
    public int getItemCount() { return webcams.size(); }

    // ---- ViewHolder ----

    static class WebcamViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivWebcam, ivPlayOverlay;
        private final ProgressBar pbLoading;
        private final TextView tvWebcamName;

        WebcamViewHolder(@NonNull View itemView) {
            super(itemView);
            ivWebcam     = itemView.findViewById(R.id.iv_webcam);
            ivPlayOverlay= itemView.findViewById(R.id.iv_play_overlay);
            pbLoading    = itemView.findViewById(R.id.pb_webcam_loading);
            tvWebcamName = itemView.findViewById(R.id.tv_webcam_name);
        }

        void bind(WebcamModel webcam) {
            tvWebcamName.setText(webcam.getName());
            pbLoading.setVisibility(View.VISIBLE);
            ivWebcam.setImageDrawable(null);

            String loadUrl = webcam.getThumbnailUrl() != null
                    ? webcam.getThumbnailUrl()
                    : null;

            boolean isVideo = webcam.getStreamType() == WebcamModel.StreamType.YOUTUBE
                    || webcam.getStreamType() == WebcamModel.StreamType.VIDEO_MP4;

            // Show play icon overlay for video types
            if (ivPlayOverlay != null) {
                ivPlayOverlay.setVisibility(isVideo ? View.VISIBLE : View.GONE);
            }

            if (loadUrl != null) {
                Glide.with(itemView.getContext())
                        .load(loadUrl)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .placeholder(R.drawable.ic_webcam_placeholder)
                        .error(R.drawable.ic_no_signal)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                    Target<Drawable> target, boolean isFirstResource) {
                                pbLoading.setVisibility(View.GONE);
                                return false;
                            }
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                    Target<Drawable> target, DataSource dataSource,
                                    boolean isFirstResource) {
                                pbLoading.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ivWebcam);
            } else {
                // No thumbnail (feratel MP4) — just show placeholder
                pbLoading.setVisibility(View.GONE);
                ivWebcam.setImageResource(R.drawable.ic_webcam_placeholder);
            }

            // Click → open stream in system app
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(webcam.getStreamUrl()));
                // Prefer YouTube app for YOUTUBE type
                if (webcam.getStreamType() == WebcamModel.StreamType.YOUTUBE) {
                    intent.setPackage("com.google.android.youtube");
                    try {
                        v.getContext().startActivity(intent);
                    } catch (Exception ex) {
                        // Fallback: open in browser if YouTube app not installed
                        intent.setPackage(null);
                        v.getContext().startActivity(intent);
                    }
                } else {
                    v.getContext().startActivity(intent);
                }
            });
        }
    }
}
