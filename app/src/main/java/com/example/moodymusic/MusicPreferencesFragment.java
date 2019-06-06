package com.example.moodymusic;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;


public class MusicPreferencesFragment<rockBox> extends Fragment {


    private static final String ARG_PARAM1 = "param1";
    private String mParam1 = Integer.toString(R.id.etUsername);
    private CheckBox rockBox, rapBox, hnrBox, popBox, countryBox, classicalBox;
    private CheckBox.OnClickListener cbListener;
    final ArrayList<String> musicPreference = new ArrayList<String>();


    public MusicPreferencesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment MusicPreferencesFragment.
     */
    public static MusicPreferencesFragment newInstance(String param1) {
        MusicPreferencesFragment fragment = new MusicPreferencesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View viewer = inflater.inflate(R.layout.fragment_music_preferences, container, false);
        TextView textView = viewer.findViewById(R.id.text_music_preferences_fragment);
        rockBox = viewer.findViewById(R.id.rockBox);
        rapBox = viewer.findViewById(R.id.rapBox);
        hnrBox = viewer.findViewById(R.id.hnrBox);
        popBox = viewer.findViewById(R.id.popBox);
        countryBox = viewer.findViewById(R.id.countryBox);
        classicalBox = viewer.findViewById(R.id.classicalBox);
        cbListener = (new View.OnClickListener() {
            @Override
            public void onClick(View viewer) {
                if (rockBox.isChecked())
                    musicPreference.add("rock");
                if (rapBox.isChecked())
                    musicPreference.add("rap");
                if (hnrBox.isChecked())
                    musicPreference.add("hip hop and r&b");
                if (popBox.isChecked())
                    musicPreference.add("pop");
                if (countryBox.isChecked())
                    musicPreference.add("country");
                if (classicalBox.isChecked())
                    musicPreference.add("classical");
            }
        });
        return viewer;
    }

    private ArrayList<String> getMusicPrefrences(){
        return musicPreference;
    }


}
