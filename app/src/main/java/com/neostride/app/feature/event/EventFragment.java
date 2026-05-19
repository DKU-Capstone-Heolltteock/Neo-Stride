package com.neostride.app.feature.event;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;

public class EventFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event, container, false);

        RecyclerView rv = root.findViewById(R.id.rv_patch_notes);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        PatchNoteAdapter adapter = new PatchNoteAdapter(PatchNote.getAll());
        adapter.setOnItemClickListener(note -> {
            Intent intent = new Intent(getContext(), PatchNoteDetailActivity.class);
            intent.putExtra(PatchNoteDetailActivity.EXTRA_VERSION, note.version);
            startActivity(intent);
        });

        rv.setAdapter(adapter);

        return root;
    }
}
