package com.example.attendance.fragments.snapmit.assignments;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snapmit.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentsFragment extends Fragment {
    static AssignmentsFragment assignmentsFragment;
    AssignmentsViewModel assignmentsViewModel;

    public AssignmentsFragment() {
    }

    public static AssignmentsFragment newInstance() {
        AssignmentsFragment fragment = new AssignmentsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assignmentsViewModel = ViewModelProviders.of(this.getActivity()).get(AssignmentsViewModel.class);
        assignmentsFragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_assignments, container, false);

        RecyclerView recyclerView =  root.findViewById(R.id.recycler);
        recyclerView.setAdapter(new AssignmentAdapter(root.findViewById(R.id.loading), root.findViewById(R.id.fab), root.findViewById(R.id.no_items)));
        LinearLayoutManager llm = new LinearLayoutManager(container.getContext());
        llm.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.addItemDecoration(new SpacesItemDecoration(64));

        root.findViewById(R.id.fab).setOnClickListener((v) -> {
            EditText editText = new EditText(v.getContext());
            editText.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                editText.setFocusedByDefault(true);
            }

            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(v.getContext(), R.style.SuccessDialog)
                .setIcon(R.drawable.ic_paper)
                .setTitle("New Assignment")
                .setMessage("Create a new assignment; But first, give it a name! This can't be changed later.")
                .setView(editText);
            dialog.setPositiveButton("Ok", (dialogg, which) -> {
                InputMethodManager imm = (InputMethodManager) this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);

                ProgressBar spinner = new ProgressBar(v.getContext());
                spinner.setPadding(16, 16, 16,16);
                AlertDialog dialog1 = new MaterialAlertDialogBuilder(v.getContext(), R.style.MyDialog)
                        .setTitle("Please wait")
                        .setView(spinner)
                        .show();
                dialog1.setCancelable(false);
                dialog1.setCanceledOnTouchOutside(false);
                String name = editText.getText().toString();
                if (name.isEmpty()) name = "Unnamed assignment";
                Map<String, Object> data = new HashMap<>();
                data.put("name", name);

                Log.d("CALL", "START");
                String finalName = name;
                FirebaseFunctions
                    .getInstance()
                    .getHttpsCallable("createAssignment")
                    .call(data)
                    .continueWith(task -> {
                        Log.d("CALL", "END");
                        dialog1.dismiss();
                        MaterialAlertDialogBuilder dialogBuilder;
                        if (task.isSuccessful()) {
                            JsonObject response = (JsonObject) JsonParser.parseString((String)task.getResult().getData());
                            String code = response.get("message").getAsString();
                            dialogBuilder = new MaterialAlertDialogBuilder(v.getContext(), R.style.SuccessDialog);
                            dialogBuilder.setIcon(R.drawable.ic_check_black_24dp);
                            dialogBuilder.setTitle("Success!");
                            dialogBuilder.setOnDismissListener(dialog2 -> {
                                ((AssignmentAdapter) recyclerView.getAdapter()).assignments.add(
                                        0, new Assignment(code, finalName, new ArrayList<>())
                                );
                                root.findViewById(R.id.no_items).animate().alpha(0);
                                recyclerView.getAdapter().notifyItemInserted(0);
                                recyclerView.getLayoutManager().scrollToPosition(0);
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
            dialog.show();
        });

        return root;
    }
}

class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.Card>{
    public List<Assignment> assignments = Collections.synchronizedList(new ArrayList<>());

    public AssignmentAdapter(View hideOnInflate, View showOnInflate, View noItems) {
        showOnInflate.setAlpha(0);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        db.collection("users").document(auth.getUid()).get().continueWith(task ->  {
            hideOnInflate.animate().alpha(0);
            showOnInflate.animate().alpha(1);
            Log.d("ASSIGNMENT", task.getResult().get("assignments").toString());
            List<String> assignmentIDs;
            if (task.getResult() == null) assignmentIDs = new ArrayList<>();
            else assignmentIDs = (List) task.getResult().get("assignments");

            assignments = Collections.synchronizedList(new ArrayList<>());
            for (int i=0;i<assignmentIDs.size();i++) assignments.add(new Assignment());
            ArrayList<Task<DocumentSnapshot>> gets = new ArrayList<>();
            int pos = 0;
            for (String assignmentID : assignmentIDs){
                Log.d("ASSIGNMENT", assignmentID);
                Task<DocumentSnapshot> get = db.collection("assignments").document(assignmentID).get();
                int finalPos = pos;
                get.continueWith(task1 -> {
                    Log.d("ASSIGNMENT", "asss: "+assignments);
                    Assignment assignment = new Assignment(assignmentID);
                    if (task1.getResult() != null) {
                        assignment.name = (String) task1.getResult().get("name");
                        assignment.submissions = (List) task1.getResult().get("submissions");
                    }
                    assignments.set(assignmentIDs.size()-1- finalPos, assignment);
                    notifyDataSetChanged();
                    return 1;
                });
                gets.add(get);
                pos++;
            }
            return 1;
        }).continueWith((Continuation<Integer, Object>) task12 -> {
            if (getItemCount() == 0) noItems.animate().alpha(1);
            return 1;
        });;
    }

    @NonNull
    @Override
    public AssignmentAdapter.Card onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.assignment_card, parent, false);
        return new Card(viewGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentAdapter.Card holder, int position) {
        Assignment assignment = assignments.get(position);
        String code = assignment.code;
        ((TextView)holder.view.findViewById(R.id.name)).setText(assignment.name);
        ((TextView)holder.view.findViewById(R.id.code)).setText(assignment.formatCode());
        ((TextView)holder.view.findViewById(R.id.numSubmissions)).setText(""+assignment.submissions.size());

        holder.view.setOnClickListener(v -> {
            AssignmentsFragment.assignmentsFragment.assignmentsViewModel.assignment.postValue(assignment);
            Map<View, String> map = new HashMap<>();
            FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(v.findViewById(R.id.name), "name")
                    .addSharedElement(v.findViewById(R.id.code), "code")
                    .build();
            NavHostFragment.findNavController(AssignmentsFragment.assignmentsFragment)
                    .navigate(R.id.action_nav_tassignments_to_assignments2Fragment, null, null, extras);
        });
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    public class Card extends RecyclerView.ViewHolder{
        ViewGroup view;
        public Card(@NonNull ViewGroup itemView) {
            super(itemView);
            view = itemView;
        }
    }
}

class Assignment{
    public Assignment(){}
    public Assignment(String code) {this.code = code;}

    public Assignment(String code, String name, List<String> submissions) {
        this.code = code;
        this.name = name;
        this.submissions = submissions;
    }

    public String code = "ABC123XYZ";
    public String name = "Unknown assignment";
    public List<String> submissions = new ArrayList<>();

    @Override
    public String toString() {
        return "Assignment{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", submissions=" + submissions +
                '}';
    }

    public String formatCode() {
        if (code.equals("DELETED")) return "DELETED";
        return code.substring(0, 3) + " " + code.substring(3, 6) + " "+code.substring(6, 9);
    }
}