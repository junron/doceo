package com.example.attendance.fragments.snapmit.submit;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.attendance.MainActivity;
import com.example.attendance.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Submit2Fragment extends Fragment {

    private SubmitViewModel submitViewModel;
    private DragListView board;

    public Submit2Fragment() {
        // Required empty public constructor
    }

    public static Submit2Fragment newInstance() {
        Submit2Fragment fragment = new Submit2Fragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        submitViewModel = ViewModelProviders.of(this.getActivity()).get(SubmitViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_submit2, container, false);

        board = root.findViewById(R.id.image_scroll);
        board.setLayoutManager(new LinearLayoutManager(inflater.getContext(), RecyclerView.VERTICAL, false));
        board.setCanNotDragAboveTopItem(false);
        board.setCanNotDragBelowBottomItem(false);
        board.setSnapDragItemToTouch(false);
        board.setDragEnabled(true);

        ArrayList<Pair<Long, String>> itemArr = new ArrayList<>();
        for (File f : submitViewModel.imagesData.getValue()){
            Log.d("image", f.getAbsolutePath());
            itemArr.add(new Pair<>((long)f.getAbsolutePath().hashCode(), f.getAbsolutePath()));
        }

        board.setAdapter(new ItemAdapter(itemArr, R.id.grab_handle, false, submitViewModel.versionMap.getValue()), false);

        root.findViewById(R.id.next_button).setOnClickListener(v -> {
            if (board.getAdapter().getItemList().size() == 0) {
                new MaterialAlertDialogBuilder(getContext(), R.style.ErrorDialog)
                    .setTitle("No pages")
                    .setIcon(R.drawable.ic_paper)
                    .setMessage("You deleted all your pages. Good job. :/")
                    .show();
                return;
            }

            AlertDialog builder = new MaterialAlertDialogBuilder(getContext(), R.style.SuccessDialog)
                    .setTitle("Assignment code").create();

            final ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            View view = LayoutInflater.from(getContext()).inflate(R.layout.code_input, null);

            TextInputEditText editText = view.findViewById(R.id.code_input);
            view.findViewById(R.id.button_done).setOnClickListener((v1) -> {
                String code = editText.getText().toString();
                if (!code.matches("[a-zA-Z0-9]{9}")) {
                    editText.setError("Invalid code");
                } else {
                    editText.setError(null);
                    submitViewModel.code.setValue(editText.getText().toString());
                    NavHostFragment.findNavController(this).navigate(R.id.loadingFragment);
                    builder.dismiss();
                }
            });

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().matches("[a-zA-Z0-9]*")) return;
                    s.replace(0, s.length(), s.toString().replaceAll("[^a-zA-Z0-9]", ""));
                }
            });

            String clipboard = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
            View pasteButton = view.findViewById(R.id.button_paste);
            if (clipboard.matches("[a-zA-Z0-9]{9}")) pasteButton.setVisibility(View.VISIBLE);
            pasteButton.setOnClickListener((v1) -> {
                editText.setText(clipboard);
            });

            builder.setView(view);
            builder.show();

        });

        return root;
    }

    @Override
    public void onPause(){
        super.onPause();
        updateData(board.getAdapter().getItemList(), ((ItemAdapter) board.getAdapter()).versionMaps);
    }


    private ArrayList<File> updateData(List dataList, Map<String, Integer> versionMaps) {
        ArrayList<File> data = new ArrayList<>();
        for (Object o : dataList) data.add(new File(((Pair<Long, String>) o).second));
        submitViewModel.imagesData.postValue(data);
        submitViewModel.versionMap.postValue(versionMaps);
        return data;
    }
}


class ItemAdapter extends DragItemAdapter<Pair<Long, String>, ItemAdapter.ViewHolder> {

    private int mGrabHandleId;
    private boolean mDragOnLongPress;
    Map<String, Integer> versionMaps;

    ItemAdapter(ArrayList<Pair<Long, String>> list, int grabHandleId, boolean dragOnLongPress, Map<String, Integer> map) {
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        versionMaps = map;
        setItemList(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_holder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        String file = mItemList.get(position).second;
        if (!versionMaps.containsKey(file)) versionMaps.put(file, 0);
        long id = mItemList.get(position).first;
        View card = holder.card;

        Glide.with(MainActivity.activity)
                .load(new File(file))
                .placeholder(R.drawable.ic_placeholder)
                .signature(new ObjectKey(versionMaps.get(file)))
                .into((ImageView) (card.findViewById(R.id.image)));

        card.findViewById(R.id.image).setOnClickListener((v) -> {
            View image = LayoutInflater.from(v.getContext()).inflate(R.layout.zoomable_image, null);
            Glide.with(MainActivity.activity)
                    .load(new File(file))
                    .placeholder(R.drawable.ic_placeholder)
                    .signature(new ObjectKey(versionMaps.get(file)))
                    .into((ImageView) image.findViewById(R.id.image));

            new MaterialAlertDialogBuilder(v.getContext())
                    .setView(image)
                    .show();
        });

        holder.itemView.findViewById(R.id.proc_left).setOnClickListener((v) -> {
            new Thread(() -> {
                Mat image = Imgcodecs.imread(file);
                Core.rotate(image, image, Core.ROTATE_90_COUNTERCLOCKWISE);
                Imgcodecs.imwrite(file, image);
                versionMaps.put(file, versionMaps.get(file)+1);
                MainActivity.activity.runOnUiThread(() -> notifyItemChanged(getPositionForItemId(id)));
            }).start();
        });

        holder.itemView.findViewById(R.id.proc_right).setOnClickListener((v) -> {
            new Thread(() -> {
                Mat image = Imgcodecs.imread(file);
                Mat rotate = new Mat();
                Core.rotate(image, rotate, Core.ROTATE_90_CLOCKWISE);
                Imgcodecs.imwrite(file, rotate);
                versionMaps.put(file, versionMaps.get(file)+1);
                MainActivity.activity.runOnUiThread(() -> notifyItemChanged(getPositionForItemId(id)));
            }).start();
        });

        holder.itemView.findViewById(R.id.proc_hori).setOnClickListener((v) -> {
            new Thread(() -> {
                Mat image = Imgcodecs.imread(file);
                Mat flip = new Mat();
                Core.flip(image, flip, 1);
                Imgcodecs.imwrite(file, flip);
                versionMaps.put(file, versionMaps.get(file)+1);
                MainActivity.activity.runOnUiThread(() -> notifyItemChanged(getPositionForItemId(id)));
            }).start();
        });

        holder.itemView.findViewById(R.id.proc_vert).setOnClickListener((v) -> {
            new Thread(() -> {
                Mat image = Imgcodecs.imread(file);
                Mat flip = new Mat();
                Core.flip(image, flip, 0);
                Imgcodecs.imwrite(file, flip);
                versionMaps.put(file, versionMaps.get(file)+1);
                MainActivity.activity.runOnUiThread(() -> notifyItemChanged(getPositionForItemId(id)));
            }).start();
        });

        holder.itemView.findViewById(R.id.assignment_delete_button).setOnClickListener((v) -> {
            new MaterialAlertDialogBuilder(v.getContext(), R.style.ErrorDialog)
                    .setTitle("Delete page")
                    .setIcon(R.drawable.ic_delete_black_24dp)
                    .setMessage("Delete this page? You can't undo this.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        mItemList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(0, getItemCount());
                    })
                    .show();
        });

        holder.itemView.setTag(mItemList.get(position));
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).first;
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        View card;

        ViewHolder(final View card) {
            super(card, mGrabHandleId, mDragOnLongPress);
            this.card = card;
        }
    }
}
