package com.kqrse.proxichat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kqrse.proxichat.databinding.ActivitySettingsBinding
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsBinding
    lateinit var storageRef: StorageReference
    lateinit var currentPicName: String
    var newPicName = ""

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        storageRef = FirebaseStorage.getInstance().getReference("image")

        setupEditTextListeners()

        binding.settingsDisplayPic.setOnClickListener { selectNewPic() }
        loadCurrentPic()
        loadCurrentName()
        manageSaveButtonState(false)
    }

    private fun setupEditTextListeners() {
        binding.settingsNameEditText.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.settingsNameEditText.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.settingsNameEditText.addTextChangedListener{
            if (it!!.isNotEmpty()) {
                manageSaveButtonState(true)
            } else if (it.isEmpty() and newPicName.isEmpty()) {
                manageSaveButtonState(false)
            }
        }
    }

    private fun manageSaveButtonState(isClickable: Boolean) {
        if (isClickable) {
            binding.settingsSaveButton.alpha = 1f
            binding.settingsSaveButton.isClickable = true
            binding.settingsSaveButton.setOnClickListener {saveSettings()}
        } else {
            binding.settingsSaveButton.alpha = 0.33f
            binding.settingsSaveButton.isClickable = false
            binding.settingsSaveButton.setOnClickListener {null}
        }
    }

    private fun loadCurrentName() {
        val sharedPrefs = this.getSharedPreferences("proxiChat", Context.MODE_PRIVATE) ?: return
        binding.settingsNameEditText.hint = sharedPrefs.getString("displayName", "Unnamed")
    }

    private fun loadCurrentPic() {
        val sharedPrefs = this.getSharedPreferences("proxiChat", Context.MODE_PRIVATE) ?: return
        val fileName = sharedPrefs.getString("displayPic", "NULL").toString()

        if (fileName != "NULL") {
            currentPicName = fileName

            binding.settingsEditIcon.visibility = View.INVISIBLE
            binding.settingsDisplayPic.background = null
            binding.settingsDisplayPic.setImageDrawable(null)
            storageRef.child(fileName).downloadUrl.addOnSuccessListener { uri: Uri ->
                Glide.with(this)
                    .load(uri.toString())
                    .into(binding.settingsDisplayPic)
                binding.settingsEditIcon.visibility = View.VISIBLE
                Log.d("shark", "Successfully loaded stored pic")
            }.addOnFailureListener {
                binding.settingsDisplayPic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.default_avatar))
                binding.settingsEditIcon.visibility = View.VISIBLE
                Log.d("shark", "Failed to load stored pic")
            }
        }

    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            uploadNewPic(data?.data!!)
        }
    }

    private fun selectNewPic() {
        val openGalleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        resultLauncher.launch(openGalleryIntent)
    }

    private fun uploadNewPic(imageUri : Uri) {
        //Display dialog
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading new display picture ...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        //Name new file
        val fileName = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(Date())
        val imageRef = storageRef.child(fileName)

        imageRef.putFile(imageUri).addOnSuccessListener {
            //Set new Image
            binding.settingsDisplayPic.setImageURI(imageUri)
            newPicName = fileName
            if (progressDialog.isShowing) progressDialog.dismiss()
            Log.d("shark", "Uploaded new pic")
            manageSaveButtonState(true)

        }.addOnFailureListener {
            if (progressDialog.isShowing) progressDialog.dismiss()
            Log.d("shark", "Failed to upload new pic")
        }
    }

    private fun saveSettings() {
        var validSave = false

        if (binding.settingsNameEditText.text.length > 24){
            binding.settingsNameEditText.error = "Please make your name less than 24 characters"

        } else if (binding.settingsNameEditText.text.isNotEmpty()) {
            val sharedPrefs = this.getSharedPreferences("proxiChat", Context.MODE_PRIVATE) ?: return
            with (sharedPrefs.edit()) {
                putString("displayName", binding.settingsNameEditText.text.toString())
                apply()
            }

            Toast.makeText(this, "Your display name is now: " + binding.settingsNameEditText.text.toString(),
                Toast.LENGTH_SHORT).show()

            Log.d("shark", "Saved new name")
            validSave = true
        }

        if (newPicName.isNotEmpty()) {
            val sharedPrefs = this.getSharedPreferences("proxiChat", Context.MODE_PRIVATE)

            //Delete old image if it exists
//            val oldImageRef = sharedPrefs.getString("displayPic", "NULL").toString()
//            if (oldImageRef != "NULL") {
//                storageRef.child(oldImageRef).delete()
//                Log.d("shark", "Deleted old pic")
//            }

            //Save new image into shared prefs
            with (sharedPrefs.edit()) {
                putString("displayPic", newPicName)
                apply()
            }

            Log.d("shark", "Saved new pic")
            validSave = true

            //Prevent from deleting new pic
            newPicName = ""
        }

        if (validSave) onSupportNavigateUp()
    }

    override fun onSupportNavigateUp(): Boolean {
        //Delete new image if it was not saved
//        if (newPicName.isNotEmpty()) {
//            storageRef.child(newPicName).delete()
//            Log.d("shark", "Deleted new pic")
//        }
        onBackPressed()
        return true
    }

}