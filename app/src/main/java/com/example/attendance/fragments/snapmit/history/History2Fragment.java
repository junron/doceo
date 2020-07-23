package com.example.attendance.fragments.snapmit.history;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager2.widget.ViewPager2;

import com.example.attendance.adapters.snapmit.ImagesAdapter;
import com.example.attendance.R;

public class History2Fragment extends Fragment {

    static HistoryViewModel historyViewModel;
    static History2Fragment history2Fragment;

    public History2Fragment() {
        // Required empty public constructor
    }

    public static History2Fragment newInstance() {
        History2Fragment fragment = new History2Fragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyViewModel = ViewModelProviders.of(this.getActivity()).get(HistoryViewModel.class);
        history2Fragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_assignment_views, container, false);

        Submission submission = historyViewModel.submission.getValue();
        Log.d("HISTORY", String.valueOf(submission));

        root.findViewById(R.id.email_button).setVisibility(View.GONE);
        ((TextView) root.findViewById(R.id.name)).setText(submission.name);
        ((TextView) root.findViewById(R.id.time)).setText(submission.email);
        ((TextView) root.findViewById(R.id.email)).setText(submission.formatCode());
        ((TextView) root.findViewById(R.id.email)).setTypeface(Typeface.MONOSPACE);

        ViewPager2 viewPager = root.findViewById(R.id.images);
        viewPager.setAdapter(new ImagesAdapter(submission.images, this));
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

        String comment = submission.comment;
        TextView viewCommentBox = root.findViewById(R.id.view_comment_box);
        viewCommentBox.setText(comment.isEmpty() ? "Your teacher has not commented on this yet." : comment);

        root.findViewById(R.id.comment_button).setOnClickListener((v) -> {
            v.setClickable(false);
            Animation a = new Animation() {
                int initHeight = viewCommentBox.getHeight();
                int targetHeight = initHeight > 100 ? 0 : 600;

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    viewCommentBox.getLayoutParams().height = interpolatedTime == 1
                            ? targetHeight
                            : (int) ((targetHeight-initHeight) * interpolatedTime) + initHeight;
                    viewCommentBox.requestLayout();
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
            viewCommentBox.startAnimation(a);
        });
        return root;
    }
}
