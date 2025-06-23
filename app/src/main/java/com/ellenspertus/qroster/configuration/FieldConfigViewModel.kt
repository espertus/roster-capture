package com.ellenspertus.qroster.configuration

import android.content.Context
import androidx.lifecycle.ViewModel

class FieldConfigViewModel : ViewModel() {
    private val _studentFields = listOf(
        StudentField("First name", isMandatory = true),
        StudentField("Last name", isMandatory = true),
        StudentField("Selfie", isMandatory = true),
        StudentField("ID", isRenameable = true),
        StudentField("Preferred name"),
        StudentField("Pronouns"),
        StudentField("Name recording")
    )
    val studentFields: List<StudentField>
        get() = _studentFields.toList() // defensive copy

    fun updateFieldStatus(fieldName: String, newStatus: FieldStatus) {
        _studentFields.find { it.name == fieldName }?.status = newStatus
    }

    fun updateFieldDisplayName(fieldName: String, newDisplayName: String) {
        _studentFields.find { it.name == fieldName }?.overrideName = newDisplayName
    }

    fun getConfigurableFields() =
         _studentFields.filter {
            !(it.isMandatory && it.status == FieldStatus.REQUIRED)
        }

    fun loadConfiguration(context: Context) {
        val sharedPrefs = context.getSharedPreferences(FIELD_CONFIG_KEY, Context.MODE_PRIVATE)

        _studentFields.forEach { field ->
            sharedPrefs.getString(field.name, null)?.let {
                field.status = FieldStatus.valueOf(it)
            }
            if (field.isRenameable) {
                field.overrideName = sharedPrefs.getString("${field.name}_display", null)
            }
        }
    }

    fun saveConfiguration(context: Context) {
        val sharedPrefs =
            context.getSharedPreferences(FIELD_CONFIG_KEY, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        _studentFields.forEach { field ->
            editor.putString(field.name, field.status.name)
            if (field.isRenameable) {
                editor.putString("${field.name}_display", field.displayName)
            }
        }

        editor.apply()
    }

    companion object {
        private const val FIELD_CONFIG_KEY = "field_config"
    }
}