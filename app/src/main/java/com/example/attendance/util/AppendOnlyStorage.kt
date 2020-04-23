package com.example.attendance.util

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.list

class AppendOnlyStorage<T>(
    name: String,
    private val serializer: KSerializer<T>,
    private val items: MutableList<T> = mutableListOf()
) : List<T> by items {
    companion object {
        private lateinit var context: Context
        fun init(context: Context) {
            this.context = context
            this.context.filesDir.resolve("data").mkdir()
        }
    }

    private val storageFile = context.filesDir.resolve("data/$name.json")

    init {
        if (!storageFile.exists()) {
            storageFile.createNewFile()
            storageFile.writeText("[]")
        }
        val items = Json.parse(serializer.list, storageFile.readText())
        this.items.removeAll { true }
        items.forEach {
            this.items += it
        }
    }

    private fun write() {
        storageFile.writeText(Json.stringify(serializer.list, items))
    }


    operator fun plusAssign(item: T) {
        this.items += item
        write()
    }

}
