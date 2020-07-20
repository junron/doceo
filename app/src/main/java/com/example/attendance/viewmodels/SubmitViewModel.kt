package com.example.attendance.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

class SubmitViewModel : ViewModel() {
    var imagesData: MutableLiveData<List<File>> = MutableLiveData()
    var versionMap: MutableLiveData<Map<String, Int>>
    var code: MutableLiveData<String>

    init {
        imagesData.value = ArrayList()
        versionMap = MutableLiveData()
        versionMap.value = HashMap()
        code = MutableLiveData()
        code.value = ""
    }
}
