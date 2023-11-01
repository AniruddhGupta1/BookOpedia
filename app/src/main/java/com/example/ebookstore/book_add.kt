package com.example.ebookstore

import android.annotation.SuppressLint

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

class book_add : AppCompatActivity() {
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var databaseReference: DatabaseReference
    lateinit var database: FirebaseDatabase
    lateinit var storageReference: StorageReference
    lateinit var title: EditText
    lateinit var year: EditText
    lateinit var author: EditText
    lateinit var description: EditText
    lateinit var genre: EditText
    lateinit var uploadImage: Button
    lateinit var uploadPdf: Button
    lateinit var addBook: Button
    lateinit var selectedPdf:TextView
    lateinit var selectedImage:ImageView
    lateinit var imgPicker: ActivityResultLauncher<String>
    lateinit var pdfPicker: ActivityResultLauncher<String>
    lateinit var linkPdf: String
    lateinit var linkImg: String
    private val STORAGE_PERMISSION_REQUEST_CODE = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_add)
        title = findViewById(R.id.title)
        author = findViewById(R.id.Author)
        description = findViewById(R.id.Description)
        genre = findViewById(R.id.Genre)
        uploadImage = findViewById(R.id.UploadImage)
        uploadPdf = findViewById(R.id.UploadPDF)
        addBook = findViewById(R.id.AddBook)
        year = findViewById(R.id.publishYear)
        selectedPdf = findViewById(R.id.selectedPdf)
        selectedImage = findViewById(R.id.selectedImage)

        linkPdf=""
        linkImg=""

        imgPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            selectedImage.visibility = View.VISIBLE
            selectedImage.setImageURI(result)
            onImageAdd(result!!)
        }

        pdfPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            selectedPdf.visibility = View.VISIBLE
            val fileName = getFileNameFromUri(result!!)

            // Display the file name
            fileName?.let {
                selectedPdf.text = "Selected PDF: $it"
            }

            // Load the PDF content (if needed)
            uploadPdfToFirebase(result!!)
        }

        // Function to extract the file name from a Uri


        uploadImage.setOnClickListener {
            if (checkStoragePermission()) {
                imgPicker.launch("image/*")
            }else{
                Toast.makeText(this, "Permission Denied " , Toast.LENGTH_SHORT).show()
            }
        }

        uploadPdf.setOnClickListener {
            if (checkStoragePermission()) {
                pdfPicker.launch("application/pdf")
            }
        }


        addBook.setOnClickListener {
            validateData(title, author, description, genre, linkImg, linkPdf, year.text.toString())
        }
    }

    // Check for read and write permissions
    // Check for read and write permissions
    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request storage permissions
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,

                        ),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }



    fun uploadPdfToFirebase(pdfUri: Uri?) {
        if (pdfUri != null) {
            // Set the desired filename for the uploaded PDF
            storageReference = FirebaseStorage.getInstance().reference.child("Uploads/" + title + ".pdf")
            storageReference.putFile(pdfUri)
                .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot? ->
                    // PDF upload successful
                    // Get the download URL of the uploaded PDF
                    storageReference.downloadUrl
                        .addOnSuccessListener { downloadUrl: Uri ->
                            linkPdf = downloadUrl.toString()

                        }.addOnFailureListener { e: Exception? -> }
                }
                .addOnFailureListener { e: Exception? -> }
        }
    }

    fun onImageAdd(imageUri: Uri?) {
        if (imageUri != null) {
            // Set the desired filename for the uploaded image
            storageReference =
                FirebaseStorage.getInstance().reference.child("Uploads/${title.text}.jpg")

            storageReference.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot? ->
                    // Image upload successful
                    // Get the download URL of the uploaded image

                    storageReference.downloadUrl
                        .addOnSuccessListener { downloadUrl: Uri ->
                            linkImg = downloadUrl.toString()
                            Log.d(linkImg,"")
                        }
                        .addOnFailureListener { e: Exception? ->
                            // Handle any errors if the download URL retrieval fails
                        }
                }
                .addOnFailureListener { e: Exception? ->
                    // Handle any errors if the image upload fails
                }
        }
    }

    fun validateData(title: EditText, author: EditText, description: EditText, genre: EditText, linkImg: String, linkPdf: String, pubYear: String) {
        val bookTitle = title.text.toString().trim()
        val bookAuthor = author.text.toString().trim()
        val bookDescription = description.text.toString().trim()
        val bookGenre = genre.text.toString().trim()


        if (bookTitle.isEmpty() || bookAuthor.isEmpty() || bookDescription.isEmpty() || bookGenre.isEmpty()  || pubYear.isEmpty()) {
            // Identify and display which fields are empty
            val emptyFields = mutableListOf<String>()
            if (bookTitle.isEmpty()) {
                emptyFields.add("Title")
            }
            if (bookAuthor.isEmpty()) {
                emptyFields.add("Author")
            }
            if (bookDescription.isEmpty()) {
                emptyFields.add("Description")
            }
            if (bookGenre.isEmpty()) {
                emptyFields.add("Genre")
            }
            if (pubYear.isEmpty()) {
                emptyFields.add("Publication Year")
            }

            val errorMessage = "Please fill in the following fields: ${emptyFields.joinToString(", ")}"
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        } else {
            // All fields are filled; proceed with adding the data
            val model: bookModel = bookModel(
                bookTitle, linkImg, bookDescription, "4.3", bookGenre, bookAuthor, pubYear, linkPdf
            )
            realtimeDatabase(model, bookGenre)
        }
    }


    private fun realtimeDatabase(bookModel: bookModel, genre: String) {
        // You should have a reference to your Firebase Realtime Database.
        // You can initialize it like this:
        databaseReference = FirebaseDatabase.getInstance().getReference("books")

        // Generate a unique key for the new book entry

        val newBookKey = databaseReference.child(genre).push().key

        // Create a map with the book data
        val bookData = hashMapOf(
            "title" to bookModel.title,
            "coverImg" to bookModel.coverImg,
            "des" to bookModel.des,
            "rating" to bookModel.rating,
            "genre" to bookModel.genre,
            "authorName" to bookModel.authorName,
            "publishedYear" to bookModel.publishedYear,
            "link" to bookModel.link
        )

        if (newBookKey != null) {
            // Use the unique key to add the book data to the database under the specified genre
            databaseReference.child(genre).child(newBookKey).setValue(bookData)
                .addOnSuccessListener {

                    Toast.makeText(this, "Book data added successfully ...", Toast.LENGTH_LONG).show()

                }
                .addOnFailureListener {
                    // Failed to add book data to the database
                    // Handle the error or show an error message to the user.
                    Toast.makeText(this, "Failed to add book data to the database", Toast.LENGTH_LONG).show()

                }
        }

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now proceed with file operations
            } else {
                // Permission denied, handle it accordingly
                Toast.makeText(this, "Storage permission is required to perform this action.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            } else {
                "Unknown.pdf"
            }
        } ?: "Unknown.pdf"
    }
}