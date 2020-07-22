package com.example.attendance.fragments.snapmit.assignments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
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
