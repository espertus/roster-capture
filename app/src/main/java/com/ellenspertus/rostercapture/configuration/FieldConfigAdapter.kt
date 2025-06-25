package com.ellenspertus.rostercapture.configuration

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.ellenspertus.rostercapture.databinding.ItemFieldConfigBinding
import com.ellenspertus.rostercapture.R

class FieldConfigAdapter(
    private val fields: List<StudentField>,
    private val onFieldStatusChanged: (StudentField, FieldStatus) -> Unit,
    private val onFieldRename: (StudentField, String) -> Unit
) : RecyclerView.Adapter<FieldConfigAdapter.FieldViewHolder>() {

    inner class FieldViewHolder(private val binding: ItemFieldConfigBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(field: StudentField) {
            with(binding) {
                // Hide the entire card if field is mandatory and can't be renamed.
                if (field.isMandatory && !field.isRenameable) {
                    root.visibility = View.GONE
                    root.layoutParams = RecyclerView.LayoutParams(0, 0)
                    return
                } else {
                    root.visibility = View.VISIBLE
                    root.layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 4, 0, 4) // Re-apply margins in dp
                    }
                }

                fieldNameText.text = field.displayName

                if (field.isRenameable) {
                    renameButton.visibility = View.VISIBLE
                    renameButton.setOnClickListener {
                        showRenameDialog(field)
                    }
                } else {
                    renameButton.visibility = View.GONE
                }

                // Disable radio buttons for mandatory fields.
                if (field.isMandatory) {
                    requiredRadio.isEnabled = false
                    optionalRadio.isEnabled = false
                    notSolicitedRadio.isEnabled = false
                    fieldStatusGroup.alpha = 0.6f
                } else {
                    requiredRadio.isEnabled = true
                    optionalRadio.isEnabled = true
                    notSolicitedRadio.isEnabled = true
                    fieldStatusGroup.alpha = 1.0f
                }

                // Set current status
                when (field.status) {
                    FieldStatus.REQUIRED -> requiredRadio.isChecked = true
                    FieldStatus.OPTIONAL -> optionalRadio.isChecked = true
                    FieldStatus.NOT_SOLICITED -> notSolicitedRadio.isChecked = true
                }

                // Handle status changes
                fieldStatusGroup.setOnCheckedChangeListener { _, checkedId ->
                    if (!field.isMandatory) {
                        val newStatus = when (checkedId) {
                            R.id.requiredRadio -> FieldStatus.REQUIRED
                            R.id.optionalRadio -> FieldStatus.OPTIONAL
                            R.id.notSolicitedRadio -> FieldStatus.NOT_SOLICITED
                            else -> field.status
                        }
                        field.status = newStatus
                        onFieldStatusChanged(field, newStatus)
                    }
                }
            }
        }

        private fun showRenameDialog(field: StudentField) {
            val context = binding.root.context
            val editText = EditText(context).apply {
                setText(field.displayName)
                setSelection(field.displayName.length)
            }

            val dialog = AlertDialog.Builder(context)
                .setTitle("Rename Field")
                .setView(editText)
                .setPositiveButton("Save") { _, _ ->
                    val newName = editText.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        field.overrideName = newName
                        onFieldRename(field, newName)
                        notifyItemChanged(bindingAdapterPosition)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()

            editText.apply {
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_NEXT ||
                        actionId == EditorInfo.IME_ACTION_GO) {
                        val newName = editText.text.toString().trim()
                        if (newName.isNotEmpty()) {
                            field.overrideName = newName
                            onFieldRename(field, newName)
                            notifyItemChanged(bindingAdapterPosition)
                            dialog.dismiss()
                        }
                        true
                    } else {
                        false
                    }
                }

                imeOptions = EditorInfo.IME_ACTION_DONE
                setSingleLine(true)
            }

            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val binding = ItemFieldConfigBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FieldViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        holder.bind(fields[position])
    }

    override fun getItemCount() = fields.size
}
