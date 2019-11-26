package com.example.chatclone;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
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
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName, profileStatus;
    private CircleImageView userProfileImage;
    private String currentUSerID;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private ProgressDialog loadingBar;
    private String dbProfileImage;

    private StorageReference userProfileImageRef;

    private static final int GalleryPick = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUSerID = mAuth.getCurrentUser().getUid();

        rootRef = FirebaseDatabase.getInstance().getReference();
        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        loadingBar = new ProgressDialog(SettingsActivity.this);

        InitializeFields();

        updateAccountSettings.setOnClickListener(
                new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                updateSettings();
            }
        });

        retreiveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(SettingsActivity.this, "Moving to crop Activity", Toast.LENGTH_SHORT).show();

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(2,2)
                        .start(SettingsActivity.this);
            }
        });
    }

    private void retreiveUserInfo() {

        rootRef.child("Users").child(currentUSerID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if ((dataSnapshot.exists()) && dataSnapshot.hasChild("name")
                            && dataSnapshot.hasChild("status") && dataSnapshot.hasChild("image")) {

                            String dbUserName = dataSnapshot.child("name").getValue().toString();
                            String dbUserStatus = dataSnapshot.child("status").getValue().toString();
                            dbProfileImage = dataSnapshot.child("image").getValue().toString();

                            userName.setText(dbUserName);
                            profileStatus.setText(dbUserStatus);
                            //userProfileImage.setImageURI(dbProfileImage);
                            Log.d("URL of image", dbProfileImage);
                            Picasso.get().load(dbProfileImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                            //userProfileImage.setVisibility(View.VISIBLE);
                                   // .centerCrop().into(userProfileImage);
                        }
                        else if ((dataSnapshot.exists() && dataSnapshot.hasChild("name"))) {

                            String dbUserName = dataSnapshot.child("name").getValue().toString();
                            String dbUserStatus = dataSnapshot.child("status").getValue().toString();

                            Log.d("URL of image", "no image found");

                            userName.setText(dbUserName);
                            profileStatus.setText(dbUserStatus);
                        }
                        else {
                            Toast.makeText(SettingsActivity.this, "Please Update your Profile...",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void InitializeFields() {
        updateAccountSettings = (Button) findViewById(R.id.update_settings_button);

        userName = (EditText) findViewById(R.id.set_user_name);
        profileStatus = (EditText) findViewById(R.id.set_profile_status);

        userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            Toast.makeText(this, "Cropping Activity", Toast.LENGTH_SHORT).show();

            final CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri resultUri = result.getUri();

            if (resultCode == RESULT_OK) {

                loadingBar.setTitle("Updating Profile Image");
                loadingBar.setMessage("Please wait, while we are updating your profile.....");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                StorageReference filePath = userProfileImageRef.child(currentUSerID + ".jpg");
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        if(task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Profile Image Updated Successfully", Toast.LENGTH_SHORT).show();

                            //final String downloadUrl = task.getResult().getMetadata().getReference().getDownloadUrl().toString();

                            String downloadUrl = task.getResult().getDownloadUrl().toString();
                            Log.d("Saving ", downloadUrl);
                            rootRef.child("Users").child(currentUSerID).child("image")
                                    .setValue(downloadUrl)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(SettingsActivity.this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                            else {
                                                String message = task.getException().toString();
                                                Toast.makeText(SettingsActivity.this, "Error " + message , Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });

                        }
                        else {
                            String message = task.getException().toString();
                            Toast.makeText(SettingsActivity.this, "Error " + message , Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    private void updateSettings() {
        String setUserName = userName.getText().toString();
        String setProfileStatus = profileStatus.getText().toString();

        if (TextUtils.isEmpty(setUserName)) {
            Toast.makeText(SettingsActivity.this, "Please write your name....", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(setProfileStatus)) {
            Toast.makeText(SettingsActivity.this, "Please write your status....", Toast.LENGTH_SHORT).show();
        }
        else {
            HashMap<String, String> profileMap = new HashMap<>();
            profileMap.put("uid", currentUSerID);
            profileMap.put("name", setUserName);
            profileMap.put("status", setProfileStatus);
            profileMap.put("image", dbProfileImage);

            //String image =
            Log.d("Image Url", dbProfileImage);
            //if (!(rootRef.child("Users").child(currentUSerID).child("image").getKey()).isEmpty()) {
            //    profileMap.put("image",rootRef.child("Users").child(currentUSerID).child("image").getKey());
            //}

            rootRef.child("Users").child(currentUSerID).setValue(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                sendUsertoMainActivity();
                                Toast.makeText(SettingsActivity.this, "Profile Updated Successfully",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else {
                                String error = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error : "+ error,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void sendUsertoMainActivity() {

        Intent settingsIntent = new Intent(SettingsActivity.this, MainActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(settingsIntent);
        finish();
    }
}