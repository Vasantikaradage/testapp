package com.example.testapp.ui.home
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.testapp.AddActivity
import com.example.testapp.MainActivity
import com.example.testapp.databinding.FragmentHomeBinding
import com.example.testapp.ui.ItemsViewModel
import com.example.testapp.ui.adapter.CustomAdapter
import com.example.testapp.ui.home.HomeViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var db: FirebaseFirestore? = null
    private lateinit var searchView: SearchView

    var dataModalArrayList: ArrayList<ItemsViewModel>? = null
    var filteredDataArrayList: ArrayList<ItemsViewModel>? = null

    private val binding get() = _binding!!
    lateinit var recylerview: RecyclerView

    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recylerview = binding.rvList
        recylerview.layoutManager = LinearLayoutManager(activity)

        dataModalArrayList = ArrayList()
        filteredDataArrayList = ArrayList()

        // initializing Firestore instance
        db = FirebaseFirestore.getInstance()

        // Get the SearchView from binding
        searchView = binding.idSV

        // Set up the search functionality
        setupSearchView()

        // Load data in the list
        loadDatainListview()

        // Date range filter setup
        binding.btnClock.setOnClickListener {
            showDateRangePicker()
        }

        binding.btnAdd.setOnClickListener {
            val intent = Intent(context, AddActivity::class.java)
            intent.putExtra("value", "1")
            startActivity(intent)
        }

        return root
    }



    private fun setupSearchView() {
        // Set query listener for the SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter the data based on the query entered by the user
                filterData(newText)
                return false
            }
        })
    }

    private fun showDateRangePicker() {
        // Show the start date picker
        val calendar = Calendar.getInstance()
        val datePickerDialogStart = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                startDate = calendar.time
                // After selecting start date, show end date picker
                showEndDatePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialogStart.show()
    }

    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialogEnd = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                endDate = calendar.time
                // Now filter the data based on the selected date range
                filterData(null)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialogEnd.show()
    }

    private fun filterData(query: String?) {
        // Clear the filtered data list before filtering again
        filteredDataArrayList?.clear()

        // Filter the data based on the selected date range
        if (startDate != null && endDate != null) {
            for (item in dataModalArrayList!!) {
                val dob = item.dob // Assuming it's in "yyyy-MM-dd" format
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dobDate = format.parse(dob)

                if (dobDate != null && dobDate.after(startDate) && dobDate.before(endDate)) {
                    filteredDataArrayList?.add(item)

                }

            }
        } else {
            // If no date range is selected, fallback to default filtering or show all data
            if (query.isNullOrEmpty()) {
                filteredDataArrayList?.addAll(dataModalArrayList!!)
            } else {
                for (item in dataModalArrayList!!) {
                    if ((item.username.contains(query, ignoreCase = true)) ||
                        (item.designation.contains(query, ignoreCase = true)) ||
                        (item.dob.contains(query, ignoreCase = true))) {
                        filteredDataArrayList?.add(item)
                    }
                }
            }
        }

        // Update the RecyclerView with the filtered data
        val adapter = CustomAdapter(this, filteredDataArrayList!!)
        recylerview.adapter = adapter
    }

    private fun loadDatainListview() {
        val itemsCollection = db!!.collection("ItemViewModel")
        itemsCollection.get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (!queryDocumentSnapshots.isEmpty) {
                    val list = queryDocumentSnapshots.documents
                    for (document in queryDocumentSnapshots.documents) {
                        val name = document.getString("username") ?: ""
                        val dob = document.getString("dob") ?: ""
                        val designation = document.getString("designation") ?: ""
                        val documentId = document.id
                        val item = ItemsViewModel("", name, designation, dob, documentId)
                        dataModalArrayList?.add(item)
                    }

                    // Initially, display all the data in RecyclerView
                    filteredDataArrayList?.addAll(dataModalArrayList!!)

                    val adapter = CustomAdapter(this, filteredDataArrayList!!)
                    recylerview.adapter = adapter

                    adapter.setOnClickListener(object : CustomAdapter.OnClickListener {
                        override fun onClick(position: Int, model: ItemsViewModel) {
                            alert(model.documentId)
                        }
                    })
                } else {
                    Toast.makeText(activity, "No data found in Database", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(activity, "Fail to load data..", Toast.LENGTH_SHORT).show()
            }
    }

    private fun alert(documentId: String) {
        val builder = AlertDialog.Builder(activity)

        builder.setMessage("Do you want to delete item ?")
        builder.setTitle("Alert !")
        builder.setCancelable(false)

        builder.setPositiveButton("Yes") { _, _ ->
            deleteCourse(documentId)
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.cancel()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun deleteCourse(documentId: String) {
        db?.collection("ItemViewModel")
            ?.document(documentId)
            ?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(activity, "Data has been deleted from Database.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(activity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(activity, "Fail to delete the course. ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


