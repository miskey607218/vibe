package com.example.vibemusicplayer.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.vibemusicplayer.ui.fragment.AlbumFragment;
import com.example.vibemusicplayer.ui.fragment.ArtistFragment;
import com.example.vibemusicplayer.ui.fragment.FavoriteFragment;
import com.example.vibemusicplayer.ui.fragment.LibraryFragment;
import com.example.vibemusicplayer.ui.fragment.PlaylistFragment;
import com.example.vibemusicplayer.ui.fragment.RecommendFragment;


public class HomeAdapter extends FragmentStateAdapter {

    public HomeAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new RecommendFragment();
            case 1:
                return new LibraryFragment();
            case 2:
                return new PlaylistFragment();
            case 3:
                return new ArtistFragment();
            case 4:
                return new FavoriteFragment();
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
