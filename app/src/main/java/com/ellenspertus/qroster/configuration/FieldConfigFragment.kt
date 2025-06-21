package com.ellenspertus.qroster.configuration

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ellenspertus.qroster.databinding.FragmentFieldConfigBinding
import com.google.android.material.snackbar.Snackbar

class FieldConfigFragment : Fragment() {
    private var _binding: FragmentFieldConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FieldConfigAdapter
    private val studentFields = mutableListOf<StudentField>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSaveButton()
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = FieldConfigAdapter(
            studentFields,
            onFieldStatusChanged = { _, _ -> },
            onFieldRename = { _, _ -> }
        )

        binding.fieldsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FieldConfigFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveConfiguration()
        }
    }

    private fun saveConfiguration() {
        // Save the configuration to SharedPreferences or database
        val sharedPrefs =
            requireContext().getSharedPreferences("field_config", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        studentFields.forEach { field ->
            editor.putString(field.name, field.status.name)
            if (field.isRenameable) {
                editor.putString("${field.name}_display", field.displayName)
            }
        }

        editor.apply()

        Snackbar.make(binding.root, "Configuration saved", Snackbar.LENGTH_SHORT).show()

        // Navigate back using Navigation Component
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}