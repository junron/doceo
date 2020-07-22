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
            Map<String, Object> data = new HashMap<>();
            data.put("images", imageURLs);
            data.put("code", submitViewModel.code.getValue());
            Task<HttpsCallableResult> upload = FirebaseFunctions.getInstance().getHttpsCallable("createSubmission").call(data);
            upload.addOnSuccessListener(httpsCallableResult -> {
                if (fragment.getActivity() == null) return;
                JsonObject response = JsonParser.parseString(httpsCallableResult.getData().toString()).getAsJsonObject();
                if (!response.get("success").getAsBoolean()) {
                    new MaterialAlertDialogBuilder(fragment.getContext(), R.style.ErrorDialog)
                            .setTitle("An error occured")
                            .setMessage(response.get("message").getAsString())
                            .setIcon(R.drawable.ic_error_outline_black_24dp)
                            .setOnCancelListener(dialog -> {
                                NavHostFragment.findNavController(this).navigateUp();
                            })
                            .show();
                } else {
                    view.findViewById(R.id.loading).animate().alpha(0);
                    new MaterialAlertDialogBuilder(fragment.getContext(), R.style.SuccessDialog)
                            .setTitle("Success!")
                            .setMessage("Your assignment was handed-in successfully!")
                            .setIcon(R.drawable.ic_check_black_24dp)
                            .setOnCancelListener(dialog -> {
                                NavHostFragment.findNavController(this).navigateUp();
                                NavHostFragment.findNavController(this).navigateUp();
                                submitViewModel.imagesData.postValue(new ArrayList<>());
                                view.findViewById(R.id.loading).setAlpha(1);
                            })
                            .show();
                }
                }).addOnFailureListener(e -> {
                    if (fragment.getActivity() == null) return;
                    new MaterialAlertDialogBuilder(fragment.getContext(), R.style.ErrorDialog)
                            .setTitle("An error occured.")
                            .setIcon(R.drawable.ic_error_outline_black_24dp)
                            .setMessage(e.getMessage())
                            .setOnDismissListener(dialog -> {
                                NavHostFragment.findNavController(this).navigateUp();
                            }).show();
                setLoadingText(e.getMessage(), loadingText);
                });

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

        for (File f : images) {
            uploadTasks.add(executorService.submit(() -> Uploader.upload(f)));
        }

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

class Uploader {
    public static final String UPLOAD_API_URL = "https://api.imgur.com/3/image";
    private final static String CLIENT_ID = "636d78a9beaeedc";

    public static JsonObject upload(File file) throws IOException {
        HttpURLConnection conn = getHttpConnection(UPLOAD_API_URL);
        writeToConnection(conn, "image=" + toBase64(file));
        return (JsonObject) JsonParser.parseString(getResponse(conn));
    }

    private static String toBase64(File file) throws IOException {
        byte[] b = new byte[(int) file.length()];
        FileInputStream fs = new FileInputStream(file);
        fs.read(b);
        fs.close();
        return URLEncoder.encode(Base64.encodeToString(b, Base64.DEFAULT), "UTF-8");

    }

    private static HttpURLConnection getHttpConnection(String url) throws IOException {
        HttpURLConnection conn;
        conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Client-ID " + CLIENT_ID);
        conn.setReadTimeout(100000);
        conn.connect();
        return conn;
    }

    private static void writeToConnection(HttpURLConnection conn, String message) throws IOException {
        OutputStreamWriter writer;
        writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(message);
        writer.flush();
        writer.close();
    }

    private static String getResponse(HttpURLConnection conn) throws IOException {
        StringBuilder str = new StringBuilder();
        BufferedReader reader;

        if (conn.getResponseCode() != 200) {
            throw new IOException("Connection code: " + conn.getResponseCode());
        }
        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            str.append(line);
        }
        reader.close();

        return str.toString();
    }
}