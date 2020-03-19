package com.example.attendance.models

import com.example.attendance.R

class FilterParam(
    val key: String,
    val value: String,
    val possibleValues: List<String>,
    val icon: Int
) {
    companion object {
        val filterParams = listOf(
            FilterParam(
                "from: ",
                "class",
                (1..7).map {
                    "40$it"
                },
                R.drawable.ic_baseline_group_24
            ),
            FilterParam(
                "takes: ",
                "subject",
                listOf(
                    "CS",
                    "CM",
                    "PC",
                    "BL",
                    "GE",
                    "EC",
                    "HI",
                    "MU",
                    "AR"
                ),
                R.drawable.ic_baseline_school_24
            )
        )
    }
}
