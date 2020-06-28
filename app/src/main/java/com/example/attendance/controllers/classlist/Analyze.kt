package com.example.attendance.controllers.classlist

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import com.example.attendance.controllers.ClasslistController
import com.example.attendance.models.*
import com.example.attendance.util.android.Sharing
import com.example.attendance.util.suffix
import com.example.attendance.util.toStringValue
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_main_content.*

fun renderChart(context: Fragment, classlistGroup: ClasslistGroup, classlist: Classlist): Boolean {
    return with(context) {
        val (entries, colors) = getPieChartData(classlistGroup, classlist)
            ?: return false
        val pieData = PieData(
            PieDataSet(
                entries,
                classlistGroup.name
            ).apply {
                this.colors = colors
            }
        )
        pieData.setValueTextSize(13f)
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                return value.toInt().suffix("student")
            }
        })
        pieChart.isDrawHoleEnabled = false
        pieChart.data = pieData
        pieChart.legend.isEnabled = false
        pieChart.description.text = ""
        pieChartContainer.visibility = View.VISIBLE
        true
    }
}

fun export(context: Context, classlistGroup: ClasslistGroup, classlist: Classlist) {
    val file = context.filesDir.resolve(
        "${classlistGroup.name}_${classlistGroup.created.toDate()
            .toStringValue()}.csv"
    )
    val data =
        getStudentStates(classlistGroup, classlist)?.entries?.sortedBy { (k, _) -> k.id }
            ?.map { (student, tag) ->
                listOf(student.name, tag.name)
            } ?: return
    val rows =
        listOf(listOf("Name", "Status")) + data
    csvWriter().open(file) {
        writeAll(rows)
    }
    MaterialAlertDialogBuilder(context)
        .setTitle("Export classlist")
        .setMessage("Classlist ${ClasslistController.classlistGroup.name} has been exported!")
        .setPositiveButton("Share") { _, _ ->
            startActivity(
                context,
                Intent.createChooser(
                    Sharing.shareCSV(
                        context,
                        Intent.ACTION_SEND,
                        file
                    ), "Share CSV"
                ), null
            )
        }
        .setNeutralButton("Open") { _, _ ->
            startActivity(
                context,
                Intent.createChooser(
                    Sharing.shareCSV(
                        context,
                        Intent.ACTION_VIEW,
                        file
                    ), "Open CSV"
                ), null
            )
        }.show()
}

private fun getPieChartData(
    classlistGroup: ClasslistGroup,
    classlist: Classlist
): Pair<List<PieEntry>, List<Int>>? {
    val studentData = getStudentStates(classlistGroup, classlist) ?: return null
    val data = studentData.entries.groupBy { it.value }
        .map { (tag, students) -> tag to students.size }.toMap()
    val colors = mutableListOf<Int>()
    val entries = data.mapNotNull { (tag, v) ->
        if (v == 0) return@mapNotNull null
        colors += tag.color
        PieEntry(v.toFloat(), tag.name)
    }
    return (entries to colors.toList())
}

private fun getStudentStates(
    classlistGroup: ClasslistGroup,
    classlist: Classlist
): Map<Student, Tag>? {
    val data = classlist
        .studentState.mapNotNull { (k, v) ->
            val student = classlistGroup.students.find { it.id == k }
                ?: return@mapNotNull null
            val tag = classlistGroup.getParsedTags()
                .find { it.id == v } ?: return@mapNotNull null
            student to tag
        }.toMap().toMutableMap()
    val defaultTag =
        classlistGroup.getParsedTags().find { it.id == Tags.absent }!!
    classlistGroup.students.forEach {
        if (data[it] == null) data[it] = defaultTag
    }
    return data
}
