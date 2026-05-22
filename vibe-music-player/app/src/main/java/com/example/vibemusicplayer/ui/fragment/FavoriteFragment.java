package com.example.vibemusicplayer.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibemusicplayer.R;
import com.example.vibemusicplayer.ui.adapter.AlbumDetailAdapter;
import com.example.vibemusicplayer.ui.viewmodel.FavoriteViewModel;

import java.util.ArrayList;

public class FavoriteFragment extends Fragment {

    private FavoriteViewModel mViewModel;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_favorite);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);

        mViewModel.getFavoriteLiveData().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null && !songs.isEmpty()) {
                recyclerView.setAdapter(new AlbumDetailAdapter("我的喜欢", null, songs));
            } else {
                recyclerView.setAdapter(new AlbumDetailAdapter("我的喜欢", null, new ArrayList<>()));
            }
        });

        return view;
    }

    @Override public void onResume() {
        super.onResume();
        if (mViewModel != null) mViewModel.loadAllData(requireContext());
    }
}

