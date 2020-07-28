package com.example.attendance.viewmodels

import androidx.lifecycle.ViewModel
import com.example.attendance.util.android.SafeLiveData
import java.io.File

class SubmitViewModel : ViewModel() {
    var imagesData: SafeLiveData<List<File>> = SafeLiveData(arrayListOf())
    var versionMap: SafeLiveData<Map<String, Int>> = SafeLiveData(mutableMapOf())
    var assignmentUUID: SafeLiveData<String> = SafeLiveData("")
}
