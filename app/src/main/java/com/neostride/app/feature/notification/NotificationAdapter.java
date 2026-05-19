package com.neostride.app.feature.notification;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;

import java.util.List;

/*
 * 알림 목록 RecyclerView 어댑터임
 * 읽음 여부에 따라 색상이 달라짐
 *   미확인 (read=false) : 형광 #CCFF00
 *   확인   (read=true)  : 회색  #555555
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private static final int COLOR_UNREAD = Color.parseColor("#CCFF00");
    private static final int COLOR_READ   = Color.parseColor("#555555");

    public interface OnItemClickListener {
        void onItemClick(NotificationItem item);
    }

    private final List<NotificationItem> list;
    private OnItemClickListener listener;

    public NotificationAdapter(List<NotificationItem> list) {
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = list.get(position);

        int color = item.read ? COLOR_READ : COLOR_UNREAD;

        holder.viewAccentBar.setBackgroundColor(color);
        holder.tvTypeLabel.setText(getLabelText(item.type));
        holder.tvTypeLabel.setTextColor(color);
        holder.tvTime.setText(item.time);
        holder.tvMessage.setText(item.message);
        holder.tvMessage.setTextColor(item.read
                ? Color.parseColor("#555555")
                : Color.parseColor("#CCCCCC"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String getLabelText(String type) {
        switch (type) {
            case NotificationItem.TYPE_FEED_TAG:       return "피드 태그";
            case NotificationItem.TYPE_FRIEND_REQUEST: return "친구 요청";
            case NotificationItem.TYPE_GRADE:          return "등급 달성";
            case NotificationItem.TYPE_FEED_COMMENT:    return "댓글";
            case NotificationItem.TYPE_TIP_COMMENT:    return "팁 댓글";
            case NotificationItem.TYPE_FEED_LIKE:       return "좋아요";
            case NotificationItem.TYPE_TIP_LIKE:        return "팁 좋아요";
            default:                                   return "알림";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View     viewAccentBar;
        TextView tvTypeLabel;
        TextView tvTime;
        TextView tvMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewAccentBar = itemView.findViewById(R.id.view_accent_bar);
            tvTypeLabel   = itemView.findViewById(R.id.tv_type_label);
            tvTime        = itemView.findViewById(R.id.tv_time);
            tvMessage     = itemView.findViewById(R.id.tv_message);
        }
    }
}
