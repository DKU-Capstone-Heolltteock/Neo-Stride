package com.neostride.app.feature.search;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.runnerpage.RunnerPageActivity;
import com.neostride.app.feature.search.model.SearchUserResponse;

import java.util.List;

/*
 * 프로필/친구 검색 결과 RecyclerView 어댑터 클래스임
 * 피드/팁은 기존 FeedAdapter, TipAdapter를 사용하고,
 * 프로필/친구 검색 결과만 이 어댑터에서 처리함
 */
public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder> {

    /*
     * 프로필/친구 검색 결과 목록임
     */
    private final List<SearchUserResponse> userList;

    /*
     * Activity 이동에 사용할 Context임
     */
    private Context context;

    /*
     * 현재 검색 탭 타입임
     * PROFILE 또는 FRIEND 값으로 사용함
     */
    private final String type;

    /*
     * SearchUserAdapter 생성자임
     */
    public SearchUserAdapter(
            List<SearchUserResponse> userList,
            String type
    ) {
        this.userList = userList;
        this.type = type;
    }

    @NonNull
    @Override
    public SearchUserViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        context = parent.getContext();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_search, parent, false);

        return new SearchUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SearchUserViewHolder holder,
            int position
    ) {
        SearchUserResponse item = userList.get(position);

        holder.tvTitle.setText(getSafeText(item.getNickname(), "알 수 없음"));

        String typeText = type.equals("FRIEND") ? "친구" : "프로필";
        String statusText = getSafeText(item.getStatusMessage(), "");
        String badgeText = getSafeText(item.getBadgeTier(), "NONE");

        holder.tvSub.setText(
                typeText
                        + " · 친구 "
                        + item.getFriendCount()
                        + "명 · "
                        + badgeText
                        + (statusText.isEmpty() ? "" : " · " + statusText)
        );

        /*
         * 프로필/친구 검색 결과 클릭 시 러너페이지로 이동함
         */
        holder.itemView.setOnClickListener(v -> {
            if (item.getUserId() == null) {
                return;
            }

            Intent intent = new Intent(context, RunnerPageActivity.class);
            intent.putExtra("user_id", item.getUserId().intValue());
            intent.putExtra("nickname", item.getNickname());
            intent.putExtra("is_friend", item.isFriend());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    /*
     * null 또는 빈 문자열일 때 기본값을 반환하는 함수임
     */
    private String getSafeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    /*
     * 프로필/친구 검색 결과 ViewHolder 클래스임
     */
    static class SearchUserViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvSub;

        public SearchUserViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tv_search_title);
            tvSub = itemView.findViewById(R.id.tv_search_sub);
        }
    }
}