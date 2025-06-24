package com.ellenspertus.qroster.configuration

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ellenspertus.qroster.AppException
import androidx.core.content.edit

class FieldConfigViewModel : ViewModel() {
    private val _studentFields = originalFields

    val studentFields: List<StudentField>
        get() = _studentFields.toList() // defensive copy

    fun hasConfiguration(context: Context) =
        context.getSharedPreferences(FIELD_CONFIG_KEY, Context.MODE_PRIVATE).all.isNotEmpty()

    fun getConfigurableFields() =
        _studentFields.filter {
            !(it.isMandatory && it.status == FieldStatus.REQUIRED)
        }

    fun updateFieldStatus(field: StudentField, newStatus: FieldStatus) {
        getFieldByName(field.name).status = newStatus
    }

    fun updateFieldDisplayName(field: StudentField, newDisplayName: String) {
        getFieldByName(field.name).overrideName = newDisplayName
    }

    fun getFieldByName(name: String) =
        _studentFields.find { it.name == name }
            ?: throw AppException.AppInternalException("Unable to find field ${name}")

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
        context.getSharedPreferences(FIELD_CONFIG_KEY, Context.MODE_PRIVATE).edit {
            _studentFields.forEach { field ->
                putString(field.name, field.status.name)
                if (field.isRenameable) {
                    putString("${field.name}_display", field.displayName)
                }
            }
        }
    }

    companion object {
        private const val FIELD_CONFIG_KEY = "field_config"

        private val originalFields = listOf(
            StudentField("First name", isMandatory = true),
            StudentField("Last name", isMandatory = true),
            StudentField("Selfie", isMandatory = true),
            StudentField("ID", isRenameable = true),
            StudentField("Preferred name"),
            StudentField("Pronouns"),
            StudentField("Name recording")
        )
    }
}