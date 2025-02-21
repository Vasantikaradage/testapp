package com.example.testapp

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.testapp.databinding.ActivityAddBinding
import com.example.testapp.ui.ItemsViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Calendar


class AddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding
    private var db: FirebaseFirestore? = null
    private var imageUrl: String? =""


  //  lateinit var binding:ActivityAddBinding
    val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val galleryUri = it
        try{
            binding.imageview.setImageURI(galleryUri)

            /////
            val auth = FirebaseAuth.getInstance()


// Check if the user is already signed in
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is signed in, proceed with uploading the image
                uploadImage(galleryUri)
            } else {
                // User is not signed in, sign them in first
                auth.signInWithEmailAndPassword("vasantikaradage@gmail.com", "Vasanti@123")
                    .addOnCompleteListener(
                        this
                    ) { task: Task<AuthResult?> ->
                        if (task.isSuccessful) {
                            // Successfully signed in
                            val user = auth.currentUser
                            // Proceed with uploading the image after sign-in
                            uploadImage(galleryUri)
                        } else {
                            // Handle sign-in failure
                            Toast.makeText(
                                applicationContext,
                                "Sign-in failed: " + task.exception!!.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }

            /////
          /*  FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(
                    this
                ) { task: Task<AuthResult?> ->
                    if (task.isSuccessful) {
                        // Successfully signed in anonymously
                        val user = FirebaseAuth.getInstance().currentUser
                        // Now proceed with uploading the image
                        val storage: FirebaseStorage = FirebaseStorage.getInstance()
                        val storageRef: StorageReference = storage.getReference()


                        // Create a reference to store the image
                        val imageRef: StorageReference =
                            storageRef.child("images/" + System.currentTimeMillis() + ".jpg")


                        // Upload the image
                        galleryUri?.let { it1 ->
                            imageRef.putFile(it1)
                                .addOnSuccessListener { taskSnapshot ->
                                    // Get the download URL after successful upload
                                    imageRef.getDownloadUrl().addOnSuccessListener { uri ->
                                        imageUrl = uri.toString()
                                        // val username = "ExampleUsername" // Replace with the actual username

                                        // Now upload the username and image URL to Firestore
                                        //uploadDataToFirestore(username, imageUrl)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        applicationContext,
                                        "Upload failed: " + e,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        // Handle anonymous sign-in failure
                        Toast.makeText(
                            applicationContext,
                            "Anonymous sign-in failed: " + task.exception!!.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
*/

            /////

        }catch(e:Exception){
            e.printStackTrace()
        }

    }

    private fun uploadImage(galleryUri: Uri?) {
        val storage: FirebaseStorage = FirebaseStorage.getInstance()
        val storageRef: StorageReference = storage.getReference()


        // Create a reference to store the image
        val imageRef: StorageReference =
            storageRef.child("images/" + System.currentTimeMillis() + ".jpg")


        // Upload the image
        galleryUri?.let { it1 ->
            imageRef.putFile(it1)
                .addOnSuccessListener { taskSnapshot ->
                    // Get the download URL after successful upload
                    imageRef.getDownloadUrl().addOnSuccessListener { uri ->
                        imageUrl = uri.toString()
                        // val username = "ExampleUsername" // Replace with the actual username

                        // Now upload the username and image URL to Firestore
                        //uploadDataToFirestore(username, imageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        applicationContext,
                        "Upload failed: " + e,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageview.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.editdob.setOnClickListener{
            val c = Calendar.getInstance()

            // on below line we are getting
            // our day, month and year.
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // on below line we are creating a
            // variable for date picker dialog.
            val datePickerDialog = DatePickerDialog(
                // on below line we are passing context.
                this,
                { view, year, monthOfYear, dayOfMonth ->
                    // on below line we are setting
                    // date to our edit text.
                    val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                    binding.editdob.setText(dat)
                },
                // on below line we are passing year, month
                // and day for the selected date in our date picker.
                year,
                month,
                day
            )
            // at last we are calling show
            // to display our date picker dialog.
            datePickerDialog.show()
        }
        val intent = intent

        // Retrieve data from the intent (in this case, we expect a string with key "value")
        val value = intent.getStringExtra("value")

        // Use the value (For example, display it in a TextView)
        if (value.equals("2")) {
            binding.editText.setText(intent.getStringExtra("username"))
            binding.editdob.setText(intent.getStringExtra("dob"))
            binding.editdesg.setText(intent.getStringExtra("designation"))
            val docid=intent.getIntExtra("docId",0)

            binding.btnAdd.setText("Update")
            binding.btnAdd.setOnClickListener {
                imageUrl?.let { it1 ->
                    updateDataToFirestore(
                        it1,
                        binding.editText.text.toString(),
                        binding.editdob.text.toString(),
                        binding.editdesg.text.toString(),
                        docid.toString()
                    )
                }

            }

        }else {
            binding.btnAdd.setText("Submit")
            binding.btnAdd.setOnClickListener {
                imageUrl?.let { it1 ->
                    addDataToFirestore(
                        it1,
                        binding.editText.text.toString(),
                        binding.editdob.text.toString(),
                        binding.editdesg.text.toString()
                    )
                }

            }
        }

       // addDataToFirestore(binding.editText.text.toString(), binding.editdob.text.toString(), binding.editdesg.text.toString())
    }


    private fun updateDataToFirestore(image: String, userName: String, dob: String, designation: String, docId: String) {
        val updatedCourse = ItemsViewModel(image, userName, dob, designation, docId)

        // Get the reference to the Firestore collection and the document by document ID.
        val documentRef = db!!.collection("ItemViewModel").document(docId)

        // Now, update the document with specific fields.
        documentRef.update(
            "image", updatedCourse.image,
            "username", updatedCourse.username,
            "dob", updatedCourse.dob,
            "designation", updatedCourse.designation
        )
            .addOnSuccessListener {
                // On successful update, show a success message.
                Toast.makeText(this, "Data has been updated.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                // On failure, show a failure message.
                Toast.makeText(this, "Failed to update the data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    /* private fun updateDataToFirestore(image: String,userName: String, dob: String, designation: String,docId:String) {
        val updatedCourse: ItemsViewModel = ItemsViewModel(image, userName, dob,designation,docId)


        // after passing data to object class we are
        // sending it to firebase with specific document id.
        // below line is use to get the collection of our Firebase Firestore.
        db!!.collection("ItemViewModel").document(docId).// below line is use toset the id of
        // document where we have to perform
        // update operation.
      //  (courses.getId()).set // after setting our document id we are
        // passing our whole object class to it.

        set(updatedCourse).addOnSuccessListener (OnSuccessListener<Void?> { // on successful completion of this process
            // we are displaying the toast message.
            Toast.makeText(this, "data has been updated..", Toast.LENGTH_SHORT)
                .show()
        }).addOnFailureListener(OnFailureListener
        // inside on failure method we are
        // displaying a failure message.
        {
            Toast.makeText(this, "Fail to update the data..", Toast.LENGTH_SHORT)
                .show()
        })
    }
*/
    private fun addDataToFirestore(image: String,userName: String, dob: String, designation: String) {
// Assuming 'db' is your FirebaseFirestore instance
        val dbCourses = db?.collection("ItemViewModel")

// Create a Courses object with the course details
        val courses = ItemsViewModel(image,userName, designation, dob)

// Adding data to Firebase Firestore
        dbCourses?.add(courses)
            ?.addOnSuccessListener { documentReference ->
                // Data added successfully

                Toast.makeText(this@AddActivity, "Your Record has been added to Firebase Firestore", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            ?.addOnFailureListener { e ->
                // Failed to add data
                Toast.makeText(this@AddActivity, "Fail to add Record \n$e", Toast.LENGTH_SHORT).show()
            }

    }


}