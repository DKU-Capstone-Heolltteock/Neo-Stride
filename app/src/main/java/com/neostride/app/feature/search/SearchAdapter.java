package com.neostride.app.feature.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.search.model.SearchItem;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private List<SearchItem> searchItemList;

    public SearchAdapter(List<SearchItem> searchItemList) {
        this.searchItemList = searchItemList;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search, parent, false);

        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SearchViewHolder holder,
            int position
    ) {
        SearchItem item = searchItemList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvSub.setText(item.getSubText());
    }

    @Override
    public int getItemCount() {
        return searchItemList.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvSub;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tv_search_title);
            tvSub = itemView.findViewById(R.id.tv_search_sub);
        }
    }
}