package com.ellenspertus.rostercapture.configuration

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.ellenspertus.rostercapture.AppException
import com.ellenspertus.rostercapture.usertest.Analytics

class FieldConfigViewModel : ViewModel() {
    private val _studentFields = originalFields

    @Suppress("unused")
    val studentFields: List<StudentField>
        get() = _studentFields.map { it.copy() } // defensive copy

    fun hasConfiguration(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).all.isNotEmpty()

    fun getConfigurableFields() =
        _studentFields.filter {
            !it.isMandatory || it.isRenameable
        }

    fun updateFieldStatus(field: StudentField, newStatus: FieldStatus) {
        getFieldByName(field.name).status = newStatus
    }

    fun updateFieldDisplayName(field: StudentField, newDisplayName: String) {
        getFieldByName(field.name).overrideName = newDisplayName
    }

    private fun getFieldByName(name: String) =
        _studentFields.find { it.name == name }
            ?: throw AppException.AppInternalException("Unable to find field $name")

    fun getIdField() = getFieldByName(ID_FIELD)
    fun getFirstNameField() = getFieldByName(FIRST_NAME)
    fun getLastNameField() = getFieldByName(LAST_NAME)
    fun getPreferredNameField() = getFieldByName(PREFERRED_NAME_FIELD)
    fun getPronounsField() = getFieldByName(PRONOUNS_FIELD_NAME)
    fun getSelfieField() = getFieldByName(SELFIE_NAME)
    fun getRecordingField() = getFieldByName(RECORDING_FIELD_NAME)

    fun loadConfiguration(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        Analytics.logFirstNTimes(10, "configuration_saved")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            _studentFields.forEach { field ->
                putString(field.name, field.status.name)
                if (field.isRenameable) {
                    putString("${field.name}_display", field.displayName)
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "field_config"

        private const val ID_FIELD = "ID"
        private const val FIRST_NAME = "First name"
        private const val LAST_NAME = "Last name"
        private const val SELFIE_NAME = "Take selfie"
        private const val PREFERRED_NAME_FIELD = "Preferred name"
        private const val PRONOUNS_FIELD_NAME = "Pronouns"
        private const val RECORDING_FIELD_NAME = "Record name"

        private val originalFields = listOf(
            StudentField(FIRST_NAME, isMandatory = true, isRenameable = true),
            StudentField(LAST_NAME, isMandatory = true, isRenameable = true),
            StudentField(SELFIE_NAME, isMandatory = true, isRenameable = true),
            StudentField(ID_FIELD, isRenameable = true),
            StudentField(PREFERRED_NAME_FIELD, isRenameable = true),
            StudentField(PRONOUNS_FIELD_NAME, isRenameable = true),
            StudentField(RECORDING_FIELD_NAME, isRenameable = true)
        )
    }
}