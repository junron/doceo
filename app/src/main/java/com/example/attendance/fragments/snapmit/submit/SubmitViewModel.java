package com.example.attendance.fragments.snapmit.submit;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SubmitViewModel extends ViewModel {
    MutableLiveData<ArrayList<File>> imagesData;
    MutableLiveData<Map<String, Integer>> versionMap;
    MutableLiveData<String> code;


    public SubmitViewModel() {
        imagesData = new MutableLiveData<>();
        imagesData.setValue(new ArrayList<>());
        versionMap = new MutableLiveData<>();
        versionMap.setValue(new HashMap<>());
        code = new MutableLiveData<>();
        code.setValue("");
    }

}