package com.example.attendance.fragments.snapmit.history;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snapmit.R;
import com.example.snapmit.ui.assignments.SpacesItemDecoration;
import com.google.android.gms.tasks.Continuation;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryFragment extends Fragment {

    static HistoryViewModel historyViewModel;
    static HistoryFragment historyFragment;
    View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyViewModel = ViewModelProviders.of(this.getActivity()).get(HistoryViewModel.class);
        historyFragment = this;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        historyViewModel = ViewModelProviders.of(this.getActivity()).get(HistoryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        view = root;
        UserSubmissionAdapter usa = new UserSubmissionAdapter(root.findViewById(R.id.loading), root.findViewById(R.id.no_items));
        RecyclerView recyclerView = root.findViewById(R.id.recycler);
        recyclerView.setAdapter(usa);
        LinearLayoutManager llm = new LinearLayoutManager(container.getContext());
        llm.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.addItemDecoration(new SpacesItemDecoration(64));

        return root;
    }
}

class UserSubmissionAdapter extends RecyclerView.Adapter<UserSubmissionAdapter.Card>{
    ArrayList<Submission> submissions;

    UserSubmissionAdapter(View hideOnLoad, View noItems) {
        submissions = new ArrayList<>();
        FirebaseFunctions.getInstance().getHttpsCallable("getSubmissionsByUser")
                .call()
                .continueWith((Continuation<HttpsCallableResult, Object>) task -> {
                    if (!task.isSuccessful()) return 0;
                    hideOnLoad.animate().alpha(0);
                    Log.d("HISTORY", task.getResult().getData().toString());
                    JsonObject response = (JsonObject) JsonParser.parseString(task.getResult().getData().toString());
                    if (!response.get("success").getAsBoolean()) return 0;
                    JsonObject submissions = response.get("message").getAsJsonObject();
                    for (String key : submissions.keySet()) {
                        JsonObject submission = submissions.get(key).getAsJsonObject();
                        Submission submission1 = new Submission();
                        submission1.images = new ArrayList<>(); 
                        for (JsonElement image : submission.get("images").getAsJsonArray()){
                            submission1.images.add(image.getAsString());
                        }
                        if (submission.get("time") != null ) {
                            SimpleDateFormat sfd = new SimpleDateFormat("d MMMM yyyy  HH:mm");
                            String time = sfd.format(new Date(submission.get("time").getAsJsonObject().get("_seconds").getAsLong()*1000));
                            submission1.email = time;
                        }
                        if (submission.get("comment") != null) {
                            submission1.comment = submission.get("comment").getAsString();
                        }
                        submission1.code = submission.get("code").getAsString();
                        submission1.name = submission.get("name").getAsString();
                        this.submissions.add(0, submission1);
                        notifyItemInserted(0);
                    }
                    return 1;
                }).continueWith(task -> {
                    if ((int) task.getResult() == 0) noItems.animate().alpha(1);
                    return 1;
                });
    }

    @NonNull
    @Override
    public UserSubmissionAdapter.Card onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.submission_card, parent, false);
        return new UserSubmissionAdapter.Card(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserSubmissionAdapter.Card holder, int position) {
        Submission submission = submissions.get(position);
        String name = submission.name;
        if (name == null) name = "";
        String email = submission.email;
        if (email == null) email = "";
        String num = "" + submission.images.size();

        holder.view.setOnClickListener((v) -> {
            HistoryFragment.historyViewModel.submission.setValue(submission);
            Log.d("HISTORY", String.valueOf(submission));
            Log.d("HISTORY", String.valueOf(HistoryFragment.historyViewModel.submission.getValue()));
            NavHostFragment.findNavController(HistoryFragment.historyFragment).navigate(R.id.action_nav_sgallery_to_history2Fragment);
        });

        ((TextView)holder.view.findViewById(R.id.name)).setText(name);
        ((TextView)holder.view.findViewById(R.id.email)).setText(email);
        ((TextView)holder.view.findViewById(R.id.numPages)).setText(num);

    }

    @Override
    public int getItemCount() {
        return submissions.size();
    }

    public class Card extends RecyclerView.ViewHolder {
        View view;
        public Card(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }
    }
}

class Submission {
    public String id;
    public List<String> images;
    public String email;
    public String name;
    public String comment = "";
    public String code;

    @Override
    public String toString() {
        return "Submission{" +
                "id='" + id + '\'' +
                ", images=" + images +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String formatCode() {
        if (code.equals("DELETED")) return "DELETED";
        return code.substring(0, 3) + " " + code.substring(3, 6) + " "+code.substring(6, 9);
    }
}