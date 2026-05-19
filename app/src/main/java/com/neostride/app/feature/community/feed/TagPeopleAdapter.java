package com.neostride.app.feature.community.feed;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.community.feed.model.TagUser;

import java.util.List;

public class TagPeopleAdapter extends RecyclerView.Adapter<TagPeopleAdapter.TagPeopleViewHolder> {

    public interface OnTagClickListener {
        void onTagClick(TagUser user);
    }

    private final List<TagUser> userList;
    private final List<TagUser> selectedUserList;
    private final OnTagClickListener listener;

    public TagPeopleAdapter(
            List<TagUser> userList,
            List<TagUser> selectedUserList,
            OnTagClickListener listener
    ) {
        this.userList = userList;
        this.selectedUserList = selectedUserList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TagPeopleViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_people, parent, false);

        return new TagPeopleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TagPeopleViewHolder holder,
            int position
    ) {
        TagUser user = userList.get(position);

        holder.tvUserName.setText(user.getNickname());

        boolean selected = isSelected(user);

        holder.btnTag.setText("태그");
        holder.btnTag.setTypeface(null, Typeface.BOLD);

        if (selected) {
            holder.btnTag.setTextColor(Color.BLACK);
            holder.btnTag.setBackgroundResource(R.drawable.bg_badge_btn);
        } else {
            holder.btnTag.setTextColor(Color.WHITE);
            holder.btnTag.setBackgroundResource(R.drawable.bg_tag_neon_outline);
        }

        holder.btnTag.setOnClickListener(v -> listener.onTagClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private boolean isSelected(TagUser user) {
        for (TagUser selectedUser : selectedUserList) {
            if (selectedUser.getUserId().equals(user.getUserId())) {
                return true;
            }
        }

        return false;
    }

    static class TagPeopleViewHolder extends RecyclerView.ViewHolder {

        TextView tvUserName;
        TextView btnTag;

        public TagPeopleViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUserName = itemView.findViewById(R.id.tv_tag_user_name);
            btnTag = itemView.findViewById(R.id.btn_tag_user);
        }
    }
}