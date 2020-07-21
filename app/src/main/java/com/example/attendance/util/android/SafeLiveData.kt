package com.example.attendance.util.android

import androidx.lifecycle.LiveData

@Suppress("UNCHECKED_CAST")
class SafeLiveData<T>(value: T) : LiveData<T>(value) {

    override fun getValue(): T = super.getValue() as T
    public override fun setValue(value: T) = super.setValue(value)
    public override fun postValue(value: T) = super.postValue(value)
}
