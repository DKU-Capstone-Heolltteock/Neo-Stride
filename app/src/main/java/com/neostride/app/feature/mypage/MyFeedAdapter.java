package com.neostride.app.feature.mypage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.mypage.model.CommunityContentResponse;

import java.util.List;
import java.util.Locale;

public class MyFeedAdapter extends RecyclerView.Adapter<MyFeedAdapter.ViewHolder> {

    private List<CommunityContentResponse> feedList;

    public MyFeedAdapter(List<CommunityContentResponse> list) {
        this.feedList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityContentResponse item = feedList.get(position);

        // 본문 내용
        if (holder.tvContent != null) holder.tvContent.setText(item.contentText);

        // 거리 (km)
        if (holder.tvDistance != null)
            holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1fkm", item.totalDistance));

        // 시간(초) → MM:SS
        if (holder.tvDuration != null) {
            int minutes = item.duration / 60;
            int seconds = item.duration % 60;
            holder.tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
        }

        // 페이스(초/km) → M'SS"
        if (holder.tvPace != null) {
            int paceMin = item.pace / 60;
            int paceSec = item.pace % 60;
            holder.tvPace.setText(String.format(Locale.getDefault(), "%d'%02d\"/km", paceMin, paceSec));
        }

        // 작성 시간
        if (holder.tvTime != null && item.createdAt != null) {
            String date = item.createdAt.length() >= 10
                    ? item.createdAt.substring(0, 10).replace("-", ".")
                    : item.createdAt;
            holder.tvTime.setText(date);
        }

        // 아직 서버에서 안 내려오는 값들은 기본값 처리
        if (holder.tvTitle != null)      holder.tvTitle.setVisibility(View.GONE);
        if (holder.tvTagCount != null)   holder.tvTagCount.setText("0");
        if (holder.tvLikeCount != null)  holder.tvLikeCount.setText("0");
        if (holder.tvCommentCount != null) holder.tvCommentCount.setText("0");
    }

    @Override
    public int getItemCount() {
        return feedList != null ? feedList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvDistance, tvDuration, tvPace, tvTime;
        TextView tvTitle, tvTagCount, tvLikeCount, tvCommentCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent      = itemView.findViewById(R.id.tv_feed_content);
            tvDistance     = itemView.findViewById(R.id.tv_feed_distance);
            tvDuration     = itemView.findViewById(R.id.tv_feed_duration);
            tvPace         = itemView.findViewById(R.id.tv_feed_pace);
            tvTime         = itemView.findViewById(R.id.tv_feed_time);
            tvTitle        = itemView.findViewById(R.id.tv_feed_title);
            tvTagCount     = itemView.findViewById(R.id.tv_feed_tag_count);
            tvLikeCount    = itemView.findViewById(R.id.tv_feed_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_feed_comment_count);
        }
    }
}
