package com.example.vibemusicplayer.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibemusicplayer.R;
import com.example.vibemusicplayer.ui.adapter.AlbumDetailAdapter;
import com.example.vibemusicplayer.ui.viewmodel.AlbumDetailViewModel;

public class AlbumDetailFragment extends Fragment {

    private AlbumDetailViewModel mViewModel;
    private RecyclerView recyclerView;
    private String albumName, albumArt;

    public static AlbumDetailFragment newInstance() {
        return new AlbumDetailFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_detail, container, false);

        albumArt = getArguments().getString("logo");
        ImageView bannerImageView = view.findViewById(R.id.banner);

        if (albumArt != null && !albumArt.isEmpty()) {
            Glide.with(view.getContext())
                    .load(albumArt)
                    .error(R.drawable.nav_logo)
                    .into(bannerImageView);
        } else {
            bannerImageView.setImageResource(R.drawable.nav_logo);
        }

        albumName = getArguments().getString("album");
        TextView txtTitle = view.findViewById(R.id.album_title);
        txtTitle.setText(albumName);

        recyclerView = view.findViewById(R.id.recyclerView_library);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mViewModel = new ViewModelProvider(this).get(AlbumDetailViewModel.class);
        mViewModel.loadSongs(requireContext(), albumName);

        mViewModel.songsLiveData.observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                recyclerView.setAdapter(new AlbumDetailAdapter(albumName, albumArt, songs));
            }
        });

        return view;
    }
}
