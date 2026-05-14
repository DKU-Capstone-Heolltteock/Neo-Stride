package com.neostride.app.feature.tip;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
 * 팁 목록 데이터를 item_tip.xml 화면에 연결하고,
 * 각 팁 게시글 클릭 시 TipDetailActivity로 이동하도록 처리함
 */
public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    // 팁 게시글 목록을 저장하는 리스트임
    private final ArrayList<TipItem> tipList;

    // Activity 이동에 사용할 Context 객체임
    private Context context;

    /*
     * TipAdapter 생성자임
     * Fragment 또는 Activity에서 전달받은 팁 목록을 저장함
     */
    public TipAdapter(ArrayList<TipItem> tipList) {
        this.tipList = tipList;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        // parent에서 Context를 가져와 Activity 이동 시 사용함
        context = parent.getContext();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_tip, parent, false);

        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TipViewHolder holder,
            int position
    ) {
        TipItem item = tipList.get(position);

        // 팁 게시글 기본 정보를 화면에 표시함
        holder.tvNickname.setText(item.getNickname());
        holder.tvCategory.setText(item.getCategory());
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());
        holder.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        // 배지 표시 여부를 설정함
        holder.ivBadge.setVisibility(item.isBadgeOwner() ? View.VISIBLE : View.GONE);

        // GPS 아이콘 표시 여부를 설정함
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

        /*
         * 팁 게시글 카드 클릭 시 상세 화면으로 이동함
         * 현재는 리스트 아이템에 있는 데이터를 Intent로 넘겨서 상세 화면에 표시함
         */
        holder.itemView.setOnClickListener(v -> openTipDetail(item));
    }

    @Override
    public int getItemCount() {
        return tipList.size();
    }

    /*
     * 팁 상세 화면으로 이동하는 함수임
     * TipItem의 데이터를 Intent에 담아 TipDetailActivity로 전달함
     */
    private void openTipDetail(TipItem item) {
        Intent intent = new Intent(context, TipDetailActivity.class);

        intent.putExtra("nickname", item.getNickname());
        intent.putExtra("category", item.getCategory());
        intent.putExtra("title", item.getTitle());
        intent.putExtra("content", item.getContent());
        intent.putExtra("likeCount", item.getLikeCount());
        intent.putExtra("commentCount", item.getCommentCount());
        intent.putExtra("badgeOwner", item.isBadgeOwner());
        intent.putExtra("gpsVisible", item.isGpsVisible());

        /*
         * 이미지 URI 목록이 있으면 상세 화면으로 전달함
         * Uri는 Parcelable이라 Intent로 전달 가능함
         */
        if (item.getImageUris() != null) {
            intent.putParcelableArrayListExtra(
                    "imageUris",
                    new ArrayList<Uri>(item.getImageUris())
            );
        }

        context.startActivity(intent);
    }

    /*
     * 팁 게시글 1개 ViewHolder 클래스임
     * item_tip.xml 안에 있는 View들을 Java 변수와 연결함
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