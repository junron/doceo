package com.example.attendance.fragments.snapmit.assignments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snapmit.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class   Assignments2Fragment extends Fragment {
    AssignmentsViewModel assignmentsViewModel;
    static Assignments2Fragment assignments2Fragment;
    boolean isCommentOpen = false;

    public Assignments2Fragment() {
    }

    public static Assignments2Fragment newInstance() {
        Assignments2Fragment fragment = new Assignments2Fragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assignmentsViewModel = ViewModelProviders.of(this.getActivity()).get(AssignmentsViewModel.class);
        assignments2Fragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_assignments2, container, false);
        Assignment assignment = assignmentsViewModel.assignment.getValue();

        ((TextView) root.findViewById(R.id.name)).setText(assignment.name);
        ((TextView) root.findViewById(R.id.code)).setText(assignment.formatCode());

        RecyclerView recycler = root.findViewById(R.id.recycler);
        LinearLayoutManager llm = new LinearLayoutManager(root.getContext());
        llm.setOrientation(RecyclerView.VERTICAL);
        recycler.setLayoutManager(llm);
        recycler.setAdapter(new SubmissionAdapter(assignmentsViewModel, root.findViewById(R.id.loading), root.findViewById(R.id.no_items)));
        recycler.addItemDecoration(new SpacesItemDecoration(64));

        root.findViewById(R.id.delete_button).setOnClickListener((v) -> {
            View view = LayoutInflater.from(v.getContext()).inflate(R.layout.delete_dialog, null);
            AlertDialog confirmDialog = new MaterialAlertDialogBuilder(v.getContext(), R.style.ErrorDialog)
                    .setTitle("Delete assignment")
                    .setIcon(R.drawable.ic_delete_black_24dp)
                    .setView(view)
                    .create();
            String name = assignment.name;
            ((TextView) view.findViewById(R.id.assignment_name)).setText(name);
            MaterialButton button = view.findViewById(R.id.delete_button);
            button.setOnClickListener((v1) -> {
                confirmDialog.dismiss();
                if (!v1.isClickable()) return;
                InputMethodManager imm = (InputMethodManager) this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v1.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                ProgressBar spinner = new ProgressBar(v.getContext());
                spinner.setPadding(16, 16, 16,16);
                AlertDialog dialog = new MaterialAlertDialogBuilder(v.getContext(), R.style.MyDialog)
                        .setTitle("Please wait")
                        .setView(spinner)
                        .show();
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();

                Map<String, Object> data = new HashMap<>();
                data.put("code", assignment.code);
                FirebaseFunctions.getInstance().getHttpsCallable("deleteAssignment")
                    .call(data)
                    .continueWith(task -> {
                       dialog.dismiss();
                       MaterialAlertDialogBuilder dialogBuilder;
                       if (task.isSuccessful()) {
                            dialogBuilder = new MaterialAlertDialogBuilder(v.getContext(), R.style.SuccessDialog)
                                .setIcon(R.drawable.ic_check_black_24dp)
                                .setTitle("Success!")
                                .setOnCancelListener(dialog1 -> {
                                    NavHostFragment.findNavController(this).navigateUp();
                                });
                       }
                       else {
                            dialogBuilder = new MaterialAlertDialogBuilder(v.getContext(), R.style.ErrorDialog);
                            dialogBuilder.setIcon(R.drawable.ic_error_outline_black_24dp);
                            dialogBuilder.setTitle("Something went wrong.");
                       }
                       dialogBuilder.show();
                       return 1;
                    });
            });
            button.setClickable(false);
            ((EditText) view.findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().equals(name)) {
                        button.setClickable(true);
                        button.setBackgroundColor(getResources().getColor(R.color.errorRed));
                    } else {
                        button.setClickable(false);
                        button.setBackgroundColor(6447714);
                    }
                }
            });
            confirmDialog.show();
        });

        root.findViewById(R.id.copy_button).setOnClickListener((v) -> {
            final ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("Code", assignment.code);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(getContext(), "Code copied to clipboard!", Toast.LENGTH_SHORT).show();
            View code = root.findViewById(R.id.code);
            code.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction(() -> {
                code.animate().scaleY(1).scaleX(1).setDuration(200);
            });
        });

        return root;
    }
}

class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.Card>{
    ArrayList<Submission> submissions;
    AssignmentsViewModel assignmentsViewModel;

    SubmissionAdapter(AssignmentsViewModel assignmentsViewModel, View hideOnLoad, View noItems) {
        this.assignmentsViewModel = assignmentsViewModel;
        submissions = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        data.put("code", assignmentsViewModel.assignment.getValue().code);
        FirebaseFunctions.getInstance().getHttpsCallable("getSubmissionsByAssignment")
            .call(data)
            .continueWith((Continuation<HttpsCallableResult, Object>) task -> {
                if (!task.isSuccessful()) return 0;
                hideOnLoad.animate().alpha(0);
                Log.d("SUBMISSION", task.getResult().toString());
                JsonObject response = (JsonObject) JsonParser.parseString((String)task.getResult().getData());
                if (!response.get("success").getAsBoolean()) return 0;
                Log.d("SUBMISSION", response.toString());
                JsonObject submissions = response.get("message").getAsJsonObject();
                if (submissions.keySet().size() == 0) return 0;
                for (String id : submissions.keySet()) {
                    Submission submission = (new Gson()).fromJson(submissions.get(id), Submission.class);
                    submission.id = id;
                    submission.time = submissions.get(id).getAsJsonObject().get("time").getAsJsonObject().get("_seconds").getAsLong();
                    Log.d("SUBMISSION", submissions.get(id).toString());
                    Log.d("SUBMISSION", submission.toString());
                    assignmentsViewModel.submissions.getValue().add(submission);
                    this.submissions.add(submission);
                    notifyDataSetChanged();
                }
                return 1;
            }).continueWith(task -> {
                if ((int) task.getResult() == 0) noItems.animate().alpha(1);
                return 1;
        });
    }

    @NonNull
    @Override
    public Card onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.submission_card, parent, false);
        return new Card(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Card holder, int position) {
        String name = submissions.get(position).name;
        if (name == null) name = "";
        String email = submissions.get(position).email;
        if (email == null) email = "";
        String num = "" + submissions.get(position).images.size();

        ((TextView)holder.view.findViewById(R.id.name)).setText(name);
        ((TextView)holder.view.findViewById(R.id.email)).setText(email);
        ((TextView)holder.view.findViewById(R.id.numPages)).setText(num);

        holder.view.setOnClickListener((v) -> {
            assignmentsViewModel.submission.postValue(submissions.get(position));
            NavHostFragment.findNavController(Assignments2Fragment.assignments2Fragment).navigate(R.id.action_nav_tassignments2_to_assignments3Fragment);
        });
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
    public String assignmentName;
    public String id;
    public List<String> images;
    public String email;
    public String name;
    //prevent gson unpacking failure
    @SerializedName("'nothin")
    public long time;
    public String comment;


    @Override
    public String toString() {
        return "Submission{" +
                "id='" + id + '\'' +
                ", images=" + images +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}