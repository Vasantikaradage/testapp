
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


/*package com.example.testapp.ui.home

import android.app.AlertDialog
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
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var db: FirebaseFirestore? = null
    private lateinit var searchView: SearchView

    var dataModalArrayList: ArrayList<ItemsViewModel>? = null
    var filteredDataArrayList: ArrayList<ItemsViewModel>? = null

    private val binding get() = _binding!!
    lateinit var recylerview: RecyclerView

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

    private fun filterData(query: String?) {
        // Clear the filtered data list before filtering again
        filteredDataArrayList?.clear()

        // If the query is empty, show all data
        if (query.isNullOrEmpty()) {
            filteredDataArrayList?.addAll(dataModalArrayList!!)
        } else {
            // Filter the data based on the query
            for (item in dataModalArrayList!!) {
                if ((item.username.contains(query, ignoreCase = true)) || (item.designation.contains(query, ignoreCase = true)) || (item.dob.contains(query, ignoreCase = true))) {
                    filteredDataArrayList?.add(item)
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
}*/

////////////////////////////////////////////////



/*
package com.example.testapp.ui.home

import android.app.AlertDialog
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
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var db: FirebaseFirestore? = null
    private lateinit var searchView:SearchView

    var dataModalArrayList: ArrayList<ItemsViewModel>? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    lateinit var recylerview:RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


         recylerview= binding.rvList
        recylerview.layoutManager = LinearLayoutManager(activity)

        dataModalArrayList = ArrayList<ItemsViewModel>()


        // initializing our variable for firebase
        // firestore and getting its instance.
        db = FirebaseFirestore.getInstance()


        // here we are calling a method
        // to load data in our list view.
        loadDatainListview()
       */
/* homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }*//*

        binding.btnAdd.setOnClickListener {
            val intent = Intent(context, AddActivity::class.java)
            intent.putExtra("value","1")
            startActivity(intent)
        }

        return root
    }

    private fun loadDatainListview() {
        // below line is use to get data from Firebase
        // firestore using collection in android.

        val itemsCollection = db!!.collection("ItemViewModel")
        itemsCollection.get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                // after getting the data we are calling on success method
                // and inside this method we are checking if the received
                // query snapshot is empty or not.
                if (!queryDocumentSnapshots.isEmpty) {
                    // if the snapshot is not empty we are hiding
                    // our progress bar and adding our data in a list.
                    val list = queryDocumentSnapshots.documents
                   */
/* for (d in list) {
                        // after getting this list we are passing
                        // that list to our object class.
                        val dataModal: ItemsViewModel = d.toObject<ItemsViewModel>(ItemsViewModel::class.java)!!


                        // after getting data from Firebase we are
                        // storing that data in our array list
                        dataModalArrayList!!.add(dataModal)
                    }*//*



                    for (document in queryDocumentSnapshots.documents) {
                        val name = document.getString("username") ?: ""
                        val dob = document.getString("dob") ?: ""
                        val designation = document.getString("designation") ?: ""
                        val documentId=document.id
                        val item = ItemsViewModel("", name, designation,dob,documentId)
                        // Create a User object and add it to the list
                        dataModalArrayList?.add(item)
                    }
                    // after that we are passing our array list to our adapter class.
                    val adapter: CustomAdapter =
                        dataModalArrayList?.let { CustomAdapter(this, it) }!!


                    // after passing this array list to our adapter
                    // class we are setting our adapter to our list view.
                    recylerview.setAdapter(adapter)


                    adapter.setOnClickListener(object : CustomAdapter.OnClickListener {
                        override fun onClick(position: Int, model: ItemsViewModel) {
                            // Show the document ID when the item is clicked
                            alert(model.documentId)  // or use Toast or any other method to show it
                        }
                    })


                } else {
                    // if the snapshot is empty we are displaying a toast message.
                    Toast.makeText(
                        activity,
                        "No data found in Database",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener { // we are displaying a toast message
                // when we get any error from Firebase.
                Toast.makeText(
                    activity,
                    "Fail to load data..",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun alert(documentId: String) {
        val builder = AlertDialog.Builder(activity)

        // Set the message show for the Alert time
        builder.setMessage("Do you want to delete item ?")

        // Set Alert Title
        builder.setTitle("Alert !")

        // Set Cancelable false for when the user clicks
        // on the outside the Dialog Box then it will remain show
        builder.setCancelable(false)

        // Set the positive button with yes name Lambda
        // OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton("Yes") {

            // When the user click yes button then app will close
                dialog, which ->
            deleteCourse(documentId)

           // finish()
        }

        // Set the Negative button with No name Lambda
        // OnClickListener method is use of DialogInterface interface.
        builder.setNegativeButton("No") {

            // If user click no then dialog box is canceled.
                dialog, which -> dialog.cancel()
        }

        // Create the Alert dialog
        val alertDialog = builder.create()

        // Show the Alert Dialog box
        alertDialog.show()
    }


    private fun deleteCourse(documentId: String) {
        // Get the reference to the Firestore document using the provided document ID
        db?.collection("ItemViewModel")
            ?.document(documentId)
            ?.delete()
            ?.addOnCompleteListener { task ->
                // Inside the listener, check if the task was successful or not
                if (task.isSuccessful) {
                    // Task is successful, show success toast and navigate back to MainActivity
                    Toast.makeText(activity, "Data has been deleted from Database.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(activity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Task failed, show error toast
                    Toast.makeText(activity, "Fail to delete the course. ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}*/
