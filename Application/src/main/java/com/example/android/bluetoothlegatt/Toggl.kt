package com.example.android.bluetoothlegatt

import ch.simas.jtoggl.JToggl
import ch.simas.jtoggl.domain.Project
import ch.simas.jtoggl.domain.TimeEntry

class Toggl {
    private val jToggl = JToggl("6810e8a336cf50c5cac4befe6ffd7a6a")

    init {
        jToggl.throttlePeriod = 500L
        jToggl.switchLoggingOn()
    }

    fun start(description: String) {
        stop()

        val project = Project()
        project.id = 134612296

        val timeEntry = TimeEntry()
        timeEntry.project = project
        timeEntry.description = description
        timeEntry.createdWith = "Time Trackahedron"
        jToggl.startTimeEntry(timeEntry)
    }

    fun stop() {
        val timeEntry = jToggl.currentTimeEntry
        if (timeEntry != null) {
            jToggl.stopTimeEntry(timeEntry)
        }
    }
}