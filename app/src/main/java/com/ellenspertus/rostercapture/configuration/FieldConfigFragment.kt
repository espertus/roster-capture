package com.ellenspertus.rostercapture.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.databinding.FragmentFieldConfigBinding
import com.ellenspertus.rostercapture.extensions.navigateSafe

class FieldConfigFragment : Fragment() {
    private var _binding: FragmentFieldConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FieldConfigAdapter
    private val fieldConfigViewModel: FieldConfigViewModel by activityViewModels()

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
        setupCancelButton()
    }

    private fun setupRecyclerView() {
        adapter = FieldConfigAdapter(
            fieldConfigViewModel.getConfigurableFields(),
            onFieldStatusChanged = { field, status ->
                fieldConfigViewModel.updateFieldStatus(field, status)
            },
            onFieldRename = { field, name ->
                fieldConfigViewModel.updateFieldDisplayName(field, name)
            }
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

    private fun setupCancelButton() {
        if (fieldConfigViewModel.hasConfiguration(requireContext())) {
            binding.cancelButton.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun saveConfiguration() {
        fieldConfigViewModel.saveConfiguration(requireContext())
        Toast.makeText(requireContext(), getString(R.string.configuration_saved), Toast.LENGTH_SHORT).show()
        findNavController().navigateSafe(FieldConfigFragmentDirections.actionFieldConfigFragmentToStartFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
