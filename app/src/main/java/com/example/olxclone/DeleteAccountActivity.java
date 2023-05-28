package com.example.olxclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.olxclone.databinding.ActivityDeleteAccountBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DeleteAccountActivity extends AppCompatActivity {

    private ActivityDeleteAccountBinding binding;

    private  static final String TAG = "DELETE_ACCOUNT_TAG";
    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;

    private FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());

        binding = ActivityDeleteAccountBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_delete_account);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait..");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser =firebaseAuth.getCurrentUser();

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void deleteAccount(){
        Log.d(TAG,"deleteAccount");

        String myUid =firebaseAuth.getUid();

        progressDialog.setMessage("Deleting User Account");
        firebaseUser.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        Log.d(TAG, "onSuccess: Account deleted ");

                        progressDialog.setTitle("Deleting Account");

                        DatabaseReference refUserAds = FirebaseDatabase.getInstance().getReference("Ads");
                        refUserAds.orderByChild("uid").equalTo(firebaseAuth.getUid())
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        for (DataSnapshot ds: snapshot.getChildren() ){

                                            ds.getRef().removeValue();
                                        }

                                        DatabaseReference refUsers = FirebaseDatabase.getInstance().getReference("Users");
                                        refUsers.child(myUid)
                                                .removeValue()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {

                                                        Log.d(TAG, "onSuccess: Account deleted");

                                                        progressDialog.setMessage("Deleting User Ads");

                                                        DatabaseReference refUserAds = FirebaseDatabase.getInstance().getReference("Ads");
                                                        refUserAds.orderByChild("uid").equalTo(firebaseAuth.getUid())
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                                        for (DataSnapshot ds : snapshot.getChildren()){
                                                                            ds.getRef().removeValue();
                                                                        }

                                                                        progressDialog.setMessage("Deleting User Data");
                                                                        DatabaseReference refUsers = FirebaseDatabase.getInstance().getReference("Users");
                                                                        refUsers.child(myUid)
                                                                                .removeValue()
                                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                    @Override
                                                                                    public void onSuccess(Void unused) {

                                                                                        Log.d(TAG, "onSuccess: User data deleted...");
                                                                                        startMainActivity();
                                                                                    }
                                                                                })
                                                                                .addOnFailureListener(new OnFailureListener() {
                                                                                    @Override
                                                                                    public void onFailure(@NonNull Exception e) {

                                                                                        Log.d(TAG, "onFailure: ",e);
                                                                                        progressDialog.dismiss();;
                                                                                        Utils.toast(DeleteAccountActivity.this,"Failed to delete account due to"+e.getMessage());

                                                                                        startMainActivity();

                                                                                    }
                                                                                });
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                                    }
                                                                });
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                        Log.d(TAG, "onFailure: ",e);
                                                        progressDialog.dismiss();;
                                                        Utils.toast(DeleteAccountActivity.this,"Failed to delete account due to"+e.getMessage());
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.e(TAG,"onFailure",e);
                        progressDialog.dismiss();
                        Toast.makeText(DeleteAccountActivity.this, "Failed to delete account due to "+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private  void startMainActivity(){
        Log.d(TAG, "startMainActivity: ");
        startActivity(new Intent(this,MainActivity.class));
        finishAffinity();
    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(this,MainActivity.class));
        finishAffinity();
    }
}