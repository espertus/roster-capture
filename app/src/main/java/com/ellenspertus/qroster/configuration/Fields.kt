package com.ellenspertus.qroster.configuration

data class StudentField(
    val name: String,
    var overrideName: String? = null,
    val isMandatory: Boolean = false,
    val isRenameable: Boolean = false,
    var status: FieldStatus = if (isMandatory) FieldStatus.REQUIRED else FieldStatus.OPTIONAL
) {
    val displayName: String
        get() = overrideName ?: name

    val displayNameWithIndicator: String
        get() = if (status == FieldStatus.REQUIRED)
            displayName + REQUIRED_INDICATOR
        else
            displayName


    companion object {
        const val REQUIRED_INDICATOR = " *"
    }
}

enum class FieldStatus {
    REQUIRED,
    OPTIONAL,
    NOT_SOLICITED
}
