package com.neostride.app.feature.main.running;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;
import java.util.List;


//  러닝 모드 선택 카드 RecyclerView 어댑터
//  <p>
//  - 일반 러닝과 AI 코칭 모드 카드를 표시하며 클릭 이벤트를 콜백으로 전달한다.

public class RunningModeAdapter extends RecyclerView.Adapter<RunningModeAdapter.ViewHolder> {
    private List<RunningModeItem> items;
    private OnItemClickListener listener;

    // 모드 카드 클릭 콜백 인터페이스
    public interface OnItemClickListener {
        void onItemClick(RunningModeItem item, int position);
    }

    public RunningModeAdapter(List<RunningModeItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_running_mode, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RunningModeItem item = items.get(position);
        holder.tvTitle.setText(item.title);

        if (item.isEnabled) {
            holder.tvSubtitle.setText(item.subtitle);
            holder.cardMode.setCardBackgroundColor(item.bgColor);
            holder.tvTitle.setTextColor(0xFF000000);
            holder.tvSubtitle.setTextColor(0xFF000000);
            holder.cardMode.setOnClickListener(v -> listener.onItemClick(item, position));
        } else {
            holder.tvSubtitle.setText("GPS 확인 중...");
            holder.cardMode.setCardBackgroundColor(0xFF3A3A3A);
            holder.tvTitle.setTextColor(0xFF888888);
            holder.tvSubtitle.setTextColor(0xFF666666);
            holder.cardMode.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardMode;
        TextView tvTitle, tvSubtitle;
        ViewHolder(View v) {
            super(v);
            cardMode = v.findViewById(R.id.card_mode);
            tvTitle = v.findViewById(R.id.tv_mode_title);
            tvSubtitle = v.findViewById(R.id.tv_mode_subtitle);
        }
    }
}