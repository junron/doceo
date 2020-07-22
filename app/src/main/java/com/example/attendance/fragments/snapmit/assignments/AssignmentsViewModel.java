package com.example.attendance.fragments.snapmit.assignments;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class AssignmentsViewModel extends ViewModel {
    MutableLiveData<Assignment> assignment;
    MutableLiveData<Submission> submission;
    MutableLiveData<List<Submission>> submissions;


    public AssignmentsViewModel() {
        assignment = new MutableLiveData<>();
        submission = new MutableLiveData<>();
        submissions = new MutableLiveData<>();
        submissions.setValue(new ArrayList<>());
    }

}