package com.example.vibemusicplayer.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibemusicplayer.R;
import com.example.vibemusicplayer.ui.adapter.ArtistDetailAdapter;
import com.example.vibemusicplayer.ui.viewmodel.ArtistDetailViewModel;

public class ArtistDetailFragment extends Fragment {

    private ArtistDetailViewModel mViewModel;
    private RecyclerView recyclerView;

    public static ArtistDetailFragment newInstance() {
        return new ArtistDetailFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_detail, container, false);

        String artistName = getArguments().getString("artist");
        TextView txtTitle = view.findViewById(R.id.artist_title);
        txtTitle.setText(artistName);

        recyclerView = view.findViewById(R.id.recyclerView_library);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mViewModel = new ViewModelProvider(this).get(ArtistDetailViewModel.class);
        mViewModel.loadSongs(requireContext(), artistName);

        mViewModel.songsLiveData.observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                recyclerView.setAdapter(new ArtistDetailAdapter(songs));
            }
        });

        return view;
    }
}
