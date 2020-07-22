package com.example.attendance.fragments.snapmit.history;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HistoryViewModel extends ViewModel {

    public MutableLiveData<Submission> submission;

    public HistoryViewModel() {
        submission = new MutableLiveData<>();
    }
}