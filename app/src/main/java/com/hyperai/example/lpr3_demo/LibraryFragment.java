package com.hyperai.example.lpr3_demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LibraryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_library, container, false);
        Button btnGoLibrary = root.findViewById(R.id.btnGoLibrary);
        btnGoLibrary.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), PlateListActivity.class));
        });
        return root;
    }
}