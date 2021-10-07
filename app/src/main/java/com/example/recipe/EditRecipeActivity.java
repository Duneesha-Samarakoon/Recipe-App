package com.example.recipe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class EditRecipeActivity extends AppCompatActivity {

    //uid views
    private ImageButton backBtn;
    private ImageView recipeIconIv;
    private EditText titleEt, descriptionEt;
    private TextView categoryTv, ingrediantEt, methodEt, decorationedMethodEt, decorationedNoteEt;
    private SwitchCompat decorationSwitch;
    private Button updateRecipeBtn;

    private String recipeId;

    //permission constants
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    //image pick constants
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    //permission arrays
    private String[] cameraPermissions;
    private String[] storagePermissions;
    //image picked uri
    private Uri image_uri;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        //init ui views
        backBtn = findViewById(R.id.backBtn);
        recipeIconIv = findViewById(R.id.recipeIconIv);
        titleEt = findViewById(R.id.titleEt);
        descriptionEt = findViewById(R.id.descriptionEt);
        categoryTv = findViewById(R.id.categoryTv);
        ingrediantEt = findViewById(R.id.ingrediantEt);
        methodEt = findViewById(R.id.methodEt);
        decorationSwitch = findViewById(R.id.decorationSwitch);
        decorationedMethodEt = findViewById(R.id.decorationedMethodEt);
        decorationedNoteEt = findViewById(R.id.decorationedNoteEt);
        //updateRecipeBtn = findViewById(R.id.updateRecipeBtn);

        //get id of the recipe from intent
        recipeId = getIntent().getStringExtra("recipeId");

        //on start is unchecked, hide decorationPriceEt, decorationNoteEt
        decorationedMethodEt.setVisibility(View.GONE);
        decorationedNoteEt.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        loadRecipeDetails();//to set on views

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init permission arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //if decorationSwitch is checked: show decorationMethodEt, decorationNoteEt | if decorationSwitch is not checked: hide decorationPriceEt, decorationNoteEt
        decorationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //checked, show decorationMethodEt, decorationNoteEt
                    decorationedMethodEt.setVisibility(View.VISIBLE);
                    decorationedNoteEt.setVisibility(View.VISIBLE);
                }
                else{
                    //unchecked, hide decorationPriceEt, decorationNoteEt
                    decorationedMethodEt.setVisibility(View.GONE);
                    decorationedNoteEt.setVisibility(View.GONE);
                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onBackPressed();
            }
        });

        recipeIconIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show dialog to pick image
                showImagePickDialog();
            }
        });

        categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pick category
                categoryDialog();
            }
        });

        updateRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //flow:
                //1) Input data
                //2) Validate data
                //3) update data to db
                inputData();
            }
        });

    }

    private void loadRecipeDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Recipes").child(recipeId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //get data
                        String recipeId = ""+dataSnapshot.child("recipeId").getValue();
                        String recipeTitle = ""+dataSnapshot.child("recipeTitle").getValue();
                        String recipeDescription = ""+dataSnapshot.child("recipeDescription").getValue();
                        String recipeCategory = ""+dataSnapshot.child("recipeCategory").getValue();
                        String recipeIngredient = ""+dataSnapshot.child("recipeIngredient").getValue();
                        String recipeIcon = ""+dataSnapshot.child("recipeIcon").getValue();
                        String originMethod = ""+dataSnapshot.child("originMethod").getValue();
                        String decorationMethod = ""+dataSnapshot.child("decorationMethod").getValue();
                        String decorationNote = ""+dataSnapshot.child("decorationNote").getValue();
                        String decorationAvailable = ""+dataSnapshot.child("decorationAvailable").getValue();
                        String timestamp = ""+dataSnapshot.child("timestamp").getValue();
                        String uid = ""+dataSnapshot.child("uid").getValue();

                        //set data to views
                        if (decorationAvailable.equals("true")){
                            decorationSwitch.setChecked(true);

                            decorationedMethodEt.setVisibility(View.VISIBLE);
                            decorationedNoteEt.setVisibility(View.VISIBLE);
                        }
                        else {
                            decorationSwitch.setChecked(false);

                            decorationedMethodEt.setVisibility(View.GONE);
                            decorationedNoteEt.setVisibility(View.GONE);
                        }

                        titleEt.setText(recipeTitle);
                        descriptionEt.setText(recipeDescription);
                        categoryTv.setText(recipeCategory);
                        decorationedNoteEt.setText(decorationNote);
                        ingrediantEt.setText(recipeIngredient);
                        methodEt.setText(originMethod);
                        decorationedMethodEt.setText(decorationMethod);

                        try {
                            Picasso.get().load(recipeIcon).placeholder(R.drawable.ic_add_recipe_white).into(recipeIconIv);
                        }
                        catch (Exception e){
                            recipeIconIv.setImageResource(R.drawable.ic_add_recipe_white);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private String recipeTitle, recipeDescription, recipeCategory, recipeIngrediant, originalMethod, decorationMethod, decorationNote;
    private boolean decorationAvailable = false;

    private void inputData() {
        //1) Input data
        recipeTitle = titleEt.getText().toString().trim();
        recipeDescription = descriptionEt.getText().toString().trim();
        recipeCategory = categoryTv.getText().toString().trim();
        recipeIngrediant = ingrediantEt.getText().toString().trim();
        originalMethod = methodEt.getText().toString().trim();
        decorationAvailable = decorationSwitch.isChecked(); //true/false

        //2) Validate data
        if (TextUtils.isEmpty(recipeTitle)){
            Toast.makeText(this, "Title is required...", Toast.LENGTH_SHORT).show();
            return;// don't proceed further
        }
        if (TextUtils.isEmpty(recipeCategory)){
            Toast.makeText(this, "Category is required...", Toast.LENGTH_SHORT).show();
            return;// don't proceed further
        }
        if (TextUtils.isEmpty(originalMethod)){
            Toast.makeText(this, "Method is required...", Toast.LENGTH_SHORT).show();
            return;// don't proceed further
        }
        if (decorationAvailable){
            //recipe is with decoration
            decorationMethod = decorationedMethodEt.getText().toString().trim();
            decorationNote = decorationedNoteEt.getText().toString().trim();
            if (TextUtils.isEmpty(decorationMethod)){
                Toast.makeText(this, "Decoration Method is required...", Toast.LENGTH_SHORT).show();
                return;//don't proceed further
            }
        }
        else {
            //recipe is without decoration
            decorationMethod = "0";
            decorationNote ="";
        }

        updateRecipe();
    }

    private void updateRecipe() {
        //show progress
        progressDialog.setMessage("Updating recipe...");
        progressDialog.show();

        if (image_uri == null){
            //update without image

            //setup data in hashmap to update
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("recipeTitle", "" + recipeTitle);
            hashMap.put("recipeDescription", "" + recipeDescription);
            hashMap.put("recipeCategory", "" + recipeCategory);
            hashMap.put("recipeIngrediant", "" + recipeIngrediant);
            hashMap.put("originalMethod", "" + originalMethod);
            hashMap.put("decorationMethod", "" + decorationMethod);
            hashMap.put("decorationNote", "" + decorationNote);
            hashMap.put("decorationAvailable", "" + decorationAvailable);

            //update to db
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(firebaseAuth.getUid()).child("Recipes").child(recipeId)
                    .updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //update success
                            progressDialog.dismiss();
                            Toast.makeText(EditRecipeActivity.this, "Updated...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //update failed
                            Toast.makeText(EditRecipeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            //update with image

            //first upload image
            //image name and path on firebase storage
            String filePathAndName = "recipe_images/" + "" + recipeId;//override previous image using same id
            //upload image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //image uploaded, get url of uploaded image

                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();

                            if (uriTask.isSuccessful()) {
                                //setup data in hashmap to update
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("recipeTitle", "" + recipeTitle);
                                hashMap.put("recipeDescription", "" + recipeDescription);
                                hashMap.put("recipeCategory", "" + recipeCategory);
                                hashMap.put("recipeIcon", "" + downloadImageUri);
                                hashMap.put("originalMethod", "" + originalMethod);
                                hashMap.put("decorationMethod", "" + decorationMethod);
                                hashMap.put("decorationNote", "" + decorationNote);
                                hashMap.put("decorationAvailable", "" + decorationAvailable);

                                //update to db
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                                reference.child(firebaseAuth.getUid()).child("Recipes").child(recipeId)
                                        .updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //update success
                                                progressDialog.dismiss();
                                                Toast.makeText(EditRecipeActivity.this, "Updated...", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //update failed
                                                Toast.makeText(EditRecipeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //upload failed
                            progressDialog.dismiss();
                            Toast.makeText(EditRecipeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void categoryDialog() {
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recipe Category")
                .setItems(Constants.recipeCategories, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //get picked category
                        String category = Constants.recipeCategories[which];

                        //set picked category
                        categoryTv.setText(category);
                    }
                })
                .show();
    }

    private void showImagePickDialog() {
        //options to display in dialog
        String[] options = {"Camera", "Gallery"};
        //dialog
        AlertDialog.Builder builder =  new AlertDialog.Builder(this);
        builder.setTitle("Pick Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //handle item clicks
                        if(which==0){
                            //camera clicked
                            if(checkCameraPermission()){
                                //permission granted
                                pickFromCamera();
                            }
                            else {
                                //permission not granted, request
                                requestCameraPermission();
                            }
                        }
                        else {
                            //gallery clicked
                            if(checkStoragePermission()){
                                //permission granted
                                pickFromGallery();
                            }
                            else {
                                //permission not granted, request
                                requestStoragePermission();
                            }
                        }
                    }
                })
                .show();
    }

    private void pickFromGallery(){
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera(){
        //intent to pick image from camera

        //using media store to pick high/original quality image
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image_Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image_Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent =new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);

        return result; //returns true/false
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    //handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted){
                        //both permission granted
                        pickFromCamera();
                    }
                    else {
                        //both or one of permissions denied
                        Toast.makeText(this, "Camera & Storage permissions are required...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        //permission granted
                        pickFromGallery();
                    }else {
                        //permission denied
                        Toast.makeText(this, "Storage permission is required...", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //handle image pick results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image picked from gallery

                //save picked image uri
                image_uri = data.getData();

                //set image
                recipeIconIv.setImageURI(image_uri);
            }
            else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image picked from camera

                recipeIconIv.setImageURI(image_uri);
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}