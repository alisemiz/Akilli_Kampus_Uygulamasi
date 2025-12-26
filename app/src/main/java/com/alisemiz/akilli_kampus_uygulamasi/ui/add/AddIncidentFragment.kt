package com.alisemiz.akilli_kampus_uygulamasi.ui.add

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentAddIncidentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddIncidentFragment : Fragment() {

    private var _binding: FragmentAddIncidentBinding? = null
    private val binding get() = _binding!!

    private var secilenGorselUri: Uri? = null
    private var photoURI: Uri? = null // Kameradan çekilen fotoğrafın geçici adresi

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // 1. Galeri Başlatıcı (Galeriden seçilince burası çalışır)
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                secilenGorselUri = data.data
                binding.ivSelectedImage.setImageURI(secilenGorselUri)
                binding.ivSelectedImage.visibility = View.VISIBLE
            }
        }
    }

    // 2. Kamera Başlatıcı (Fotoğraf çekilince burası çalışır)
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoURI != null) {
            secilenGorselUri = photoURI
            binding.ivSelectedImage.setImageURI(photoURI)
            binding.ivSelectedImage.visibility = View.VISIBLE
        }
    }

    // 3. İzin İsteyici
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            kamerayiAc()
        } else {
            Toast.makeText(context, "Fotoğraf çekmek için kamera izni vermelisiniz.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddIncidentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()

        // Fotoğraf Ekle Butonu
        binding.btnAddPhoto.setOnClickListener {
            resimSecimDialoguGoster()
        }

        // Gönder Butonu
        binding.btnSubmit.setOnClickListener {
            olayiKaydet()
        }
    }

    private fun setupSpinner() {
        val categories = arrayOf("Güvenlik", "Temizlik", "Teknik", "Sağlık", "Ulaşım", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter
    }

    private fun resimSecimDialoguGoster() {
        val secenekler = arrayOf("Kamera ile Çek", "Galeriden Seç")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Fotoğraf Ekle")
        builder.setItems(secenekler) { _, which ->
            when (which) {
                0 -> { // Kamera
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        kamerayiAc()
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                1 -> { // Galeri
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryLauncher.launch(intent)
                }
            }
        }
        builder.show()
    }

    private fun kamerayiAc() {
        try {
            val photoFile = createImageFile()
            // FileProvider yetkisi (AndroidManifest'teki authorities ile AYNI olmalı)
            photoURI = FileProvider.getUriForFile(
                requireContext(),
                "com.alisemiz.akilli_kampus_uygulamasi.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoURI)
        } catch (ex: IOException) {
            Toast.makeText(context, "Dosya oluşturulamadı: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Benzersiz dosya adı: JPEG_20231025_153020_ gibi
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun olayiKaydet() {
        val title = binding.etTitle.text.toString()
        val desc = binding.etDescription.text.toString()
        val category = binding.spinnerCategory.selectedItem.toString()

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Lütfen başlık ve açıklama giriniz.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        if (secilenGorselUri != null) {
            // Resim varsa Firebase Storage'a yükle
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val ref = storage.reference.child("incident_images/$fileName")

            ref.putFile(secilenGorselUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        firestoreKaydet(title, desc, category, uri.toString())
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Resim yüklenemedi, tekrar deneyin.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Resim yoksa direkt kaydet
            firestoreKaydet(title, desc, category, null)
        }
    }

    private fun firestoreKaydet(title: String, desc: String, category: String, imageUrl: String?) {
        val incident = hashMapOf(
            "title" to title,
            "description" to desc,
            "type" to category,
            "status" to "İnceleniyor",
            "timestamp" to Timestamp(Date()),
            "userId" to auth.currentUser!!.uid,
            "imageUrl" to imageUrl,
            "followers" to emptyList<String>(),
            "latitude" to 39.92077, // Harita entegrasyonu yoksa varsayılan veya MapFragment'tan gelen değer
            "longitude" to 32.85411
        )

        db.collection("incidents").add(incident)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Olay başarıyla bildirildi!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                Toast.makeText(context, "Hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}