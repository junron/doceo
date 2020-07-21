package com.example.attendance.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class SubmitViewModel : ViewModel() {
    var imagesData: MutableLiveData<List<File>> = MutableLiveData(listOf())
    var versionMap: MutableLiveData<Map<String, Int>> = MutableLiveData(mapOf())
    var code: MutableLiveData<String> = MutableLiveData("")
}
