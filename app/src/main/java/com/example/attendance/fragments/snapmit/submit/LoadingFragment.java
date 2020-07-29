package com.example.attendance.fragments.snapmit.submit;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import com.example.attendance.R;
import com.example.attendance.util.android.SafeLiveData;
import com.example.attendance.viewmodels.SubmitViewModel;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
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
        for (File f : submitViewModel.getImagesData().getValue()) images.add(0, f);

        Fragment fragment = this;

        loadingThread = new Thread(() -> {
            setLoadingText("Uploading images", loadingText);
            List<String> imageURLs = new ArrayList<>();
            for (String s : uploadImages()) imageURLs.add(0, s);
            if (imageURLs.size() == 0) return;
            if (Thread.interrupted()) return;

            setLoadingText("Submitting", loadingText);

            Map<String, Object> data = new HashMap<>();
            String assID = submitViewModel.getAssignmentUUID().getValue();
            data.put("images", imageURLs);
            data.put("assignmentId", assID);
            data.put("comment", "");
            data.put("owner", FirebaseAuth.getInstance().getCurrentUser().getEmail());
            data.put("submissionTime", Timestamp.now());
            data.put("deleted", false);


            Task<DocumentReference> upload = FirebaseFirestore.getInstance().collection("submissions").add(data);
            Task<Void> up = upload.continueWithTask(task -> {
                DocumentReference doc = task.getResult();
                doc.update("id", doc.getId());
                DocumentSnapshot docSubmit = FirebaseFirestore.getInstance().document("assignments/"+assID).get().getResult();
                List<String> submissions = (List<String>) docSubmit.get("submissions");
                submissions.add(doc.getId());
                return docSubmit.getReference().update("submissions", submissions);
            });

            up.addOnSuccessListener(d -> {
                if (fragment.getActivity() == null) return;
                view.findViewById(R.id.loading).animate().alpha(0);
                new MaterialAlertDialogBuilder(fragment.getContext(), R.style.SuccessDialog)
                        .setTitle("Success!")
                        .setMessage("Your assignment was handed in successfully!")
                        .setIcon(R.drawable.ic_check_black_24dp)
                        .setOnCancelListener(dialog -> {
                            NavHostFragment.findNavController(this).navigateUp();
                            NavHostFragment.findNavController(this).navigateUp();
                            submitViewModel.setImagesData(new SafeLiveData<>(new ArrayList<>()));
                            view.findViewById(R.id.loading).setAlpha(1);
                        })
                        .show();
                });
            up.addOnFailureListener(e -> {
                Log.e("UPLOAD", e.toString());
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
        return (ArrayList<String>) UploaderKt.upload(images);
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
