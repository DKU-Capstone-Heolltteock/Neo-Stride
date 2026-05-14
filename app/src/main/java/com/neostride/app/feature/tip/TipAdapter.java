package com.neostride.app.feature.tip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.tip.model.TipItem;

import java.util.ArrayList;

/*
 * 팁 게시글 RecyclerView 어댑터 클래스임
 */
public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    private final ArrayList<TipItem> tipList;

    public TipAdapter(ArrayList<TipItem> tipList) {
        this.tipList = tipList;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tip, parent, false);

        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TipViewHolder holder,
            int position
    ) {
        TipItem item = tipList.get(position);

        holder.tvNickname.setText(item.getNickname());
        holder.tvCategory.setText(item.getCategory());
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());
        holder.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        holder.ivBadge.setVisibility(item.isBadgeOwner() ? View.VISIBLE : View.GONE);
        holder.ivGps.setVisibility(item.isGpsVisible() ? View.VISIBLE : View.GONE);

        /*
         * 사진이 있으면 사진 카드 전체를 보여줌
         * 사진이 없으면 사진 카드 자체를 숨겨서 제목/내용 영역이 왼쪽으로 당겨지게 함
         */
        if (item.getImageUris() != null && !item.getImageUris().isEmpty()) {
            holder.cardTipPhoto.setVisibility(View.VISIBLE);
            holder.ivTipImage.setImageURI(item.getImageUris().get(0));
        } else {
            holder.cardTipPhoto.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return tipList.size();
    }

    /*
     * 팁 게시글 1개 ViewHolder 클래스임
     */
    static class TipViewHolder extends RecyclerView.ViewHolder {

        TextView tvNickname;
        TextView tvCategory;
        TextView tvTitle;
        TextView tvContent;
        TextView tvLikeCount;
        TextView tvCommentCount;

        ImageView ivBadge;
        ImageView ivGps;
        ImageView ivTipImage;

        CardView cardTipPhoto;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);

            tvNickname = itemView.findViewById(R.id.tv_tip_nickname);
            tvCategory = itemView.findViewById(R.id.tv_tip_category);
            tvTitle = itemView.findViewById(R.id.tv_tip_title);
            tvContent = itemView.findViewById(R.id.tv_tip_content);
            tvLikeCount = itemView.findViewById(R.id.tv_tip_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_tip_comment_count);

            ivBadge = itemView.findViewById(R.id.iv_tip_badge);
            ivGps = itemView.findViewById(R.id.iv_tip_gps);
            ivTipImage = itemView.findViewById(R.id.iv_tip_image);

            cardTipPhoto = itemView.findViewById(R.id.card_tip_photo);
        }
    }
}