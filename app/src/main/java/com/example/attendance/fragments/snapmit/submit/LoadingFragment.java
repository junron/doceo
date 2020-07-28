package com.example.attendance.fragments.snapmit.submit;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import com.example.attendance.R;
import com.example.attendance.util.android.CloudStorage;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kotlinx.coroutines.GlobalScope;

public class LoadingFragment extends Fragment {

    private SubmitViewModel submitViewModel;
    private ArrayList<File> images;
    private Thread loadingThread;

    public LoadingFragment() {
        // Required empty public constructor
    }

    public static LoadingFragment newInstance() {
        LoadingFragment fragment = new LoadingFragment();
        return fragment;
    }
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        submitViewModel = ViewModelProviders.of(this.getActivity()).get(SubmitViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_loading, container, false);
        final TextView loadingText = view.findViewById(R.id.loading_text);

        images = new ArrayList<>();
        for (File f : submitViewModel.imagesData.getValue()) images.add(0, f);

        Fragment fragment = this;

        loadingThread = new Thread(() -> {
            setLoadingText("Uploading images", loadingText);
            List<String> imageURLs = new ArrayList<>();
            for (String s : uploadImages()) imageURLs.add(0, s);
            if (imageURLs.size() == 0) return;
            if (Thread.interrupted()) return;

            setLoadingText("Sending to Firebase", loadingText);

        });
        loadingThread.start();

        return view;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        loadingThread.interrupt();
    }

    private ArrayList<String> uploadImages() {
        ExecutorService executorService = Executors.newFixedThreadPool(10   );
        ArrayList<Future<JsonElement>> uploadTasks = new ArrayList<>();



        ArrayList<String> titles = new ArrayList<>();
        for (Future<JsonElement> future : uploadTasks) {
            try {
                JsonObject obj = (JsonObject) future.get();
                titles.add(obj.get("data").getAsJsonObject().get("link").getAsString());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        return titles;
    }

    void setLoadingText(String s, TextView loadingTextView) {
        runOnUIThread(() -> loadingTextView.setText(s));
    }

    void runOnUIThread(Runnable r) {
        try {
            this.getActivity().runOnUiThread(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}