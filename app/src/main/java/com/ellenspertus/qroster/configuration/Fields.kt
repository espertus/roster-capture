package com.ellenspertus.qroster.configuration

data class StudentField(
    val name: String,
    var overrideName: String? = null,
    val description: String? = null,
    val isMandatory: Boolean = false,
    val isRenameable: Boolean = false,
    var status: FieldStatus = if (isMandatory) FieldStatus.REQUIRED else FieldStatus.NOT_SOLICITED
) {
    val displayName: String
        get() = overrideName ?: name
}

enum class FieldStatus {
    REQUIRED,
    OPTIONAL,
    NOT_SOLICITED
}

val studentFields = listOf(
    StudentField("First name", isMandatory = true),
    StudentField("Last name", isMandatory = true),
    StudentField("Selfie", isMandatory = true),
    StudentField("ID", isRenameable = true),
    StudentField("Preferred name"),
    StudentField("Pronouns"),
    StudentField("Name recording")
)