package com.example.attendance.models

enum class AccessLevel {
    OWNER, EDITOR, VIEWER, NONE
}


data class Permission(val uid: String, val accessLevel: AccessLevel, val documentId: String)
