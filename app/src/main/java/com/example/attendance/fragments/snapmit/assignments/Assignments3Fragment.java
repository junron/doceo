package com.example.attendance.fragments.snapmit.assignments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.attendance.MainActivity;
import com.example.attendance.R;
import com.example.attendance.models.snapmit.Submission;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

/**
 * A simple {@link Fragment} subclass.
 */
public class    Assignments3Fragment extends Fragment {
    AssignmentsViewModel assignmentsViewModel;
    static Assignments3Fragment assignments3Fragment;
    String comment = "";
    String commentInit = "";
    boolean edited = false;

    public Assignments3Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assignmentsViewModel = ViewModelProviders.of(this.getActivity()).get(AssignmentsViewModel.class);
        assignments3Fragment = this;
    }

    @SuppressLint("CheckResult")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_assignment_view, container, false);
        Submission submission = assignmentsViewModel.submission.getValue();
        comment = comment.isEmpty() ? "" : comment;
        comment = submission.comment;
        commentInit = comment;
        ViewPager2 viewPager = root.findViewById(R.id.images);
        viewPager.setAdapter(new ImagesAdapter(submission.images, this));
        ((TextView) root.findViewById(R.id.name)).setText(submission.name);
        ((TextView) root.findViewById(R.id.email)).setText(submission.email);
        SimpleDateFormat sfd = new SimpleDateFormat("d MMMM yyyy  HH:mm");
        String time = sfd.format(new Date(submission.time*1000));
        ((TextView) root.findViewById(R.id.time)).setText(time);

        ((TextView) root.findViewById(R.id.numPages)).setText("1/"+viewPager.getAdapter().getItemCount());

        View back = root.findViewById(R.id.backButton);
        View next = root.findViewById(R.id.nextButton);

        back.setOnClickListener((v) -> {
            int curr = viewPager.getCurrentItem();
            if (curr == 0) return;
            viewPager.setCurrentItem(curr-1, true);
            ((TextView) root.findViewById(R.id.numPages)).setText(viewPager.getCurrentItem()+1+"/"+viewPager.getAdapter().getItemCount());
        });

        next.setOnClickListener((v) -> {
            int curr = viewPager.getCurrentItem();
            if (curr == viewPager.getAdapter().getItemCount()-1) return;
            viewPager.setCurrentItem(curr+1, true);
            ((TextView) root.findViewById(R.id.numPages)).setText(viewPager.getCurrentItem()+1+"/"+viewPager.getAdapter().getItemCount());
        });

        EditText commentBox = root.findViewById(R.id.edit_comment_box);
        commentBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {comment = s.toString();}
        });
        commentBox.setHint("Write a comment here! "+submission.name+" will be able to see it!");
        commentBox.setText(comment);
        root.findViewById(R.id.comment_button).setOnClickListener((v) -> {
            v.setClickable(false);
            Animation a = new Animation() {
                int initHeight = commentBox.getHeight();
                int targetHeight = initHeight > 100 ? 0 : 600;

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    commentBox.getLayoutParams().height = interpolatedTime == 1
                            ? targetHeight
                            : (int) ((targetHeight-initHeight) * interpolatedTime) + initHeight;
                    commentBox.requestLayout();
                    root.findViewWithTag(viewPager.getCurrentItem()).requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            a.setDuration(600);
            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {v.setClickable(true);}
                @Override
                public void onAnimationRepeat(Animation animation) { }
            });
            commentBox.startAnimation(a);
        });

        root.findViewById(R.id.email_button).setOnClickListener((v) -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {submission.email});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding " + assignmentsViewModel.assignment.getValue().name);
            if (intent.resolveActivity(assignments3Fragment.getActivity().getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        root.findViewById(R.id.export_button).setOnClickListener((v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.activity, READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                MultiplePermissionsListener customListener = new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted())
                            root.findViewById(R.id.export_button).callOnClick();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                };
                MultiplePermissionsListener failureListener = DialogOnAnyDeniedMultiplePermissionsListener.Builder
                        .withContext(MainActivity.activity)
                        .withTitle("Storage permissions are needed")
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.drawable.ic_menu_camera)
                        .build();

                MultiplePermissionsListener listener = new CompositeMultiplePermissionsListener(customListener, failureListener);
                Dexter.withActivity(MainActivity.activity)
                        .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
                        .withListener(listener)
                        .check();
                return;
            }

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            Assignment assignment = assignmentsViewModel.assignment.getValue();
            String date = new SimpleDateFormat("d MMMM yyyy  HH:mm").format(new Date(submission.time*1000));
            String cutName = assignment.name.replaceAll("[^a-zA-Z0-9]", "");
            if (cutName.length() > 10) cutName = cutName.substring(0, 10);

            View parent = LayoutInflater.from(getContext()).inflate(R.layout.styled_textview, null);
            TextView edit = parent.findViewById(R.id.textView2);
            edit.setText(submission.name+"_"+cutName);

            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(v.getContext(), R.style.SuccessDialog)
                    .setIcon(R.drawable.ic_picture_as_pdf_black_24dp)
                    .setTitle("Export as PDF")
                    .setMessage("Export to a PDF file, and open in another app. The file will be called ")
                    .setView(parent )
                    .setPositiveButton("OK", (dialog1, which) -> {
                        String pdfname = edit.getText().toString();
                        if (pdfname.isEmpty()) {
                            Toast.makeText(getContext(), "pdf name cannot be empty!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File file = new File(dir, pdfname+".pdf");
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        ProgressBar spinner = new ProgressBar(v.getContext());
                        spinner.setPadding(16, 16, 16,16);
                        AlertDialog dialogLoad = new MaterialAlertDialogBuilder(v.getContext(), R.style.MyDialog)
                                .setTitle("Please wait")
                                .setView(spinner)
                                .create();
                        dialogLoad.setCancelable(false);
                        dialogLoad.setCanceledOnTouchOutside(false);
                        dialogLoad.show();
                        new Thread(() -> {
                            Document document = new Document();
                            try {
                                PdfWriter.getInstance(document, new FileOutputStream(file));
                            } catch (DocumentException | FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            try {
                                document.open();
                                document.newPage();
                                document.setPageSize(PageSize.A4);
                                document.addAuthor(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                                document.addCreator("Snapmit");
                                String comment = submission.comment;
                                String text = submission.name+"'s submission\n";
                                text += submission.email + "\n";
                                text += "Submitted at " + date + "\n";
                                text += "Assignment title: " + assignment.name + "\n";
                                text += "Assignment code: " + assignment.code + "\n";
                                text += "Teacher's comment: " + (comment.isEmpty() ? "None" :comment);
                                document.add(new Paragraph(text));
                                AtomicInteger pages = new AtomicInteger(0);
                                for (String url : submission.images) {
                                    Log.d("PDF", url);
                                    Glide.with(v).downloadOnly().load(url).into(new SimpleTarget<File>() {
                                        @Override
                                        public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                                            document.newPage();
                                            try {
                                                synchronized (document) {
                                                    Image image = Image.getInstance(resource.getAbsolutePath());
                                                    image.scaleToFit(PageSize.A4);
                                                    document.setPageSize(PageSize.A4);
                                                    document.add(image);
                                                }
                                            } catch (DocumentException | IOException e) {
                                                e.printStackTrace();
                                            }
                                            pages.incrementAndGet();
                                        }
                                    });
                                }
                                while (pages.get() != submission.images.size()) {
                                    if (Thread.interrupted()) throw new InterruptedException();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            document.close();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(FileProvider.getUriForFile(
                                    getContext(),
                                    "com.example.Snapmit.provider", //(use your app signature + ".provider" )
                                    file), "application/pdf");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                            dialogLoad.dismiss();
                        }).start();
                    });

            dialog.show();
        }));

        root.findViewById(R.id.oasButton).setOnClickListener((v) -> {
            String curr = ((ImagesAdapter) viewPager.getAdapter()).images.get(viewPager.getCurrentItem());
            Glide.with(getContext()).asFile().load(curr).downloadOnly(new SimpleTarget<File>() {
                @Override
                public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                    Mat image = Imgcodecs.imread(resource.getAbsolutePath());
                    TextView textview = new TextView(getContext());
                    SpannableStringBuilder spannable = new SpannableStringBuilder("");
                    String title;
                    try {
                        String[] scans = MyImageProcessing.readOASPage(image);
                        for (int q = 0; q<20;q++) {
                            String get = scans[q]; String col = null;   
                            if (get.contains("✔")) col = "3dd47a";
                            if (get.contains("✖")) col = "d4484a";
                            if (col == null) spannable.append("Q"+(q+1)+": "+get);
                            else spannable.append(Html.fromHtml("<font color=#"+col+">"+"Q"+(q+1)+": "+get.substring(1)+"</font>"));
                            spannable.append("\n");
                        }
                        title =  (scans[20].equals("0/0")) ? "OAS scan" : "OAS score: " + scans[20];
                        textview.setText(spannable);
                        new MaterialAlertDialogBuilder(getContext(), R.style.SuccessDialog)
                                .setTitle(title)
                                .setView(textview)
                                .setIcon(R.drawable.ic_oas)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        new MaterialAlertDialogBuilder(getContext(), R.style.ErrorDialog)
                                .setTitle("No OAS found")
                                .setIcon(R.drawable.ic_oas)
                                .show();
                    }
                }
            });
        });

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (commentInit == null) return;
        if (commentInit.equals(comment)) return;
        Map<String, Object> data = new HashMap<>();
        data.put("comment", comment.isEmpty() ? " " : comment);
        data.put("id", assignmentsViewModel.submission.getValue().id);
        data.put("code", assignmentsViewModel.assignment.getValue().code);

        FirebaseFunctions.getInstance().getHttpsCallable("writeComment")
            .call(data)
            .continueWith(task -> {
                Log.d("COMMENT", task.getResult().toString());
                if (!task.isSuccessful()) return "An error occurred";
                JsonObject response = JsonParser.parseString((String)task.getResult().getData()).getAsJsonObject();
                if (!response.get("success").getAsBoolean()) return response.get("message").getAsString();
                return 1;
            }).continueWith(task -> {
                if (task.getResult() instanceof Integer)  return 1;
                new MaterialAlertDialogBuilder(MainActivity.main, R.style.ErrorDialog)
                        .setTitle("Error while commenting")
                        .setIcon(R.drawable.ic_chat_black_24dp)
                        .setMessage((String)task.getResult())
                        .show();
                return 0;
            });
    }
}

