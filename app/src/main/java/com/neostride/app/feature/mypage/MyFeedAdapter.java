package com.neostride.app.feature.mypage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

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
        // R.layout.item_my_feed가 빨간색이면 Alt + Enter로 임포트하세요.
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_feed, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityContentResponse item = feedList.get(position);

        holder.tvContent.setText(item.contentText);
        holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1fkm", item.totalDistance));

        // 시간(초)을 분:초(MM:SS) 형식으로 변환하는 간단한 로직
        int minutes = item.duration / 60;
        int seconds = item.duration % 60;
        holder.tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));

        // 페이스(초)를 분'초" 형식으로 변환
        int paceMin = item.pace / 60;
        int paceSec = item.pace % 60;
        holder.tvPace.setText(String.format(Locale.getDefault(), "%d:%02d/km", paceMin, paceSec));
    }

    @Override
    public int getItemCount() {
        return feedList != null ? feedList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvDistance, tvDuration, tvPace;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content_text);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvPace = itemView.findViewById(R.id.tv_pace);
        }
    }
}
