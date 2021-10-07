package com.example.recipe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recipe.AddRecipeActivity;
import com.example.recipe.LoginActivity;
import com.example.recipe.ProfileEditAdminActivity;
import com.example.recipe.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class MainAdminActivity extends AppCompatActivity {

    private TextView nameTv, chefNameTv, emailTv, tabRecipeTv, tabOrderIngredientsTv, filteredRecipesTv;
    private EditText searchRecipeEt;
    private ImageButton logoutBtn, editProfileBtn, addRecipeBtn, filterRecipeBtn;
    private ImageView profileIv;
    private RelativeLayout recipesRl, orderIngredientsRl;
    private RecyclerView recipesRv;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private ArrayList<ModelRecipe> recipeList;
    private AdapterRecipeAdmin adapterRecipeAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        nameTv = findViewById(R.id.nameTv);
        chefNameTv = findViewById(R.id.chefNameTv);
        emailTv = findViewById(R.id.emailTv);
        tabRecipeTv = findViewById(R.id.tabRecipeTv);
        tabOrderIngredientsTv = findViewById(R.id.tabOrderIngredientsTv);
        filteredRecipesTv = findViewById(R.id.filteredRecipesTv);
        searchRecipeEt = findViewById(R.id.searchRecipeEt);
        logoutBtn = findViewById(R.id.logoutBtn);
        editProfileBtn = findViewById(R.id.editProfileBtn);
        addRecipeBtn = findViewById(R.id.addRecipeBtn);
        filterRecipeBtn = findViewById(R.id.filterRecipeBtn);
        profileIv = findViewById(R.id.profileIv);
        recipesRl = findViewById(R.id.recipesRl);
        orderIngredientsRl = findViewById(R.id.orderIngredientsRl);
        recipesRv = findViewById(R.id.recipesRv);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();
        loadAllRecipes();

        showRecipesUi();

        //search
        searchRecipeEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterRecipeAdmin.getFilter().filter(s);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //make offline
                //sign out
                //go to login activity
                makeMeOffline();
            }
        });

        editProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open edit profile activity
                startActivity(new Intent(MainAdminActivity.this, ProfileEditAdminActivity.class));
            }
        });

        addRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open edit recipe activity
                startActivity(new Intent(MainAdminActivity.this, AddRecipeActivity.class));
            }
        });

        tabRecipeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //load recipes
                showRecipesUi();
            }
        });
        tabOrderIngredientsTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //load order ingredients
                showOrderIngredientsUI();
            }
        });
        filterRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainAdminActivity.this);
                builder.setTitle("Choose Category:")
                        .setItems(Constants.recipeCategories1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //get selected item
                                String selected = Constants.recipeCategories1[which];
                                filteredRecipesTv.setText(selected);
                                if (selected.equals("All")){
                                    //load all
                                    loadAllRecipes();
                                }
                                else {
                                    //load filtered
                                    loadFilteredRecipes(selected);
                                }
                            }
                        })
                        .show();
            }
        });

    }

    private void loadFilteredRecipes(String selected) {
        recipeList = new ArrayList<>();

        //get all recipes
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Recipes")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //before getting reset list
                        recipeList.clear();
                        for (DataSnapshot ds: dataSnapshot.getChildren()){

                            String recipeCategory = ""+ds.child("recipeCategory").getValue();

                            //if selected category matches recipe category then add in list
                            if (selected.equals(recipeCategory)){
                                ModelRecipe modelRecipe = ds.getValue(ModelRecipe.class);
                                recipeList.add(modelRecipe);
                            }


                        }
                        //setup adapter
                        adapterRecipeAdmin = new AdapterRecipeAdmin(MainAdminActivity.this, recipeList);
                        //set adapter
                        recipesRv.setAdapter(adapterRecipeAdmin);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadAllRecipes() {
        recipeList = new ArrayList<>();

        //get all recipes
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Recipes")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //before getting reset list
                        recipeList.clear();
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            ModelRecipe modelRecipe = ds.getValue(ModelRecipe.class);
                            recipeList.add(modelRecipe);
                        }
                        //setup adapter
                        adapterRecipeAdmin = new AdapterRecipeAdmin(MainAdminActivity.this, recipeList);
                        //set adapter
                        recipesRv.setAdapter(adapterRecipeAdmin);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void showRecipesUi() {
        //show recipes ui and hide order ingredients ui
        recipesRl.setVisibility(View.VISIBLE);
        orderIngredientsRl.setVisibility(View.GONE);

        tabRecipeTv.setTextColor(getResources().getColor(R.color.colorBlack));
        tabRecipeTv.setBackgroundResource(R.drawable.shape_rec04);

        tabOrderIngredientsTv.setTextColor(getResources().getColor(R.color.colorWhite));
        tabOrderIngredientsTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }
    private void showOrderIngredientsUI() {
        //show order ingredients ui and hide recipes ui
        recipesRl.setVisibility(View.GONE);
        orderIngredientsRl.setVisibility(View.VISIBLE);

        tabRecipeTv.setTextColor(getResources().getColor(R.color.colorWhite));
        tabRecipeTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        tabOrderIngredientsTv.setTextColor(getResources().getColor(R.color.colorBlack));
        tabOrderIngredientsTv.setBackgroundResource(R.drawable.shape_rec04);
    }



    private void makeMeOffline() {
        //after logging in, make user online
        progressDialog.setMessage("Logging Out...");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("online","false");

        //update value to db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //update successfully
                        firebaseAuth.signOut();
                        checkUser();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed updating
                        progressDialog.dismiss();
                        Toast.makeText(MainAdminActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null){
            startActivity(new Intent(MainAdminActivity.this, LoginActivity.class));
            finish();
        }
        else {
            loadMyInfo();
        }
    }

    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            //get data from db
                            String name = ""+ds.child("name").getValue();
                            String accountType = ""+ds.child("accountType").getValue();
                            String email = ""+ds.child("email").getValue();
                            String chefName = ""+ds.child("chefName").getValue();
                            String profileImage = ""+ds.child("profileImage").getValue();

                            //set data to ui
                            nameTv.setText(name);
                            chefNameTv.setText(chefName);
                            emailTv.setText(email);
                            try{
                                Picasso.get().load(profileImage).placeholder(R.drawable.ic_chef_gray).into(profileIv);
                            }
                            catch (Exception e){
                                profileIv.setImageResource(R.drawable.ic_chef_gray);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}