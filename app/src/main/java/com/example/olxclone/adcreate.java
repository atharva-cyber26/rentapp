package com.example.olxclone;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import com.example.olxclone.databinding.ActivityAdcreateBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class adcreate extends AppCompatActivity {


    private ActivityAdcreateBinding binding;
    private static final String TAG="AD_CREATE_TAG";

    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;


    private Uri imageUri=null;

    private ArrayList<ModelImagePicked> imagePickedArrayList;
    private AdapterImagesPicked adapterImagesPicked;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adcreate);

        binding =ActivityAdcreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth=FirebaseAuth.getInstance();

        ArrayAdapter<String> adapterCategories =new ArrayAdapter<>(this,R.layout.row_category_act,Utils.categories);
        binding.categoryAct.setAdapter(adapterCategories);

        ArrayAdapter<String> adapterConditions=new ArrayAdapter<>(this,R.layout.row_condition_act,Utils.conditions);
        binding.conditionAct.setAdapter(adapterConditions);

        imagePickedArrayList=new ArrayList<>();
        loadImages();
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();

            }
        });

        binding.toolbarAdImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickOptions();
            }
        });

        binding.postAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();

            }
        });

    }

    private void loadImages()
    {
        Log.d(TAG,"LoadImages:");
        adapterImagesPicked =new AdapterImagesPicked(this,imagePickedArrayList);
        binding.imagesRv.setAdapter(adapterImagesPicked);


    }

    private void showImagePickOptions()
    {
      Log.d(TAG,"showImagePickOptions:");
        PopupMenu popupMenu=new PopupMenu(this,binding.toolbarAdImageBtn);
        popupMenu.getMenu().add(Menu.NONE,1,1,"Camera");
        popupMenu.getMenu().add(Menu.NONE,2,2,"Gallery");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int itemId= menuItem.getItemId();

                if(itemId==1)
                {
                  if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
                  {
                     String cameraPermissions[]=new String[]{Manifest.permission.CAMERA};
                     requestCameraPermission.launch(cameraPermissions);
                  }
                  else
                  {
                      String cameraPermissions[]=new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                      requestCameraPermission.launch(cameraPermissions);
                  }
                }
                else if(itemId==2)
                {
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
                    {
                        pickImageGallery();
                    }
                    else
                    {
                        String storagePermission=Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        requestStoragePermission.launch(storagePermission);
                    }
                }
                return true;
            }
        });
    }

    private ActivityResultLauncher<String> requestStoragePermission=registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG,"onActivityResult:isGranted:"+isGranted);
                    if(isGranted)
                    {
                       pickImageGallery();
                    }else
                    {
                        Utils.toast(adcreate.this,"Storage Permission denied...");
                    }
                }
            }
    );

    private ActivityResultLauncher<String[]> requestCameraPermission =registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG,"onActivityResult:");
                    Log.d(TAG,"onActivityResult"+result.toString());

                    boolean areAllGranted=true;

                    for(Boolean isGranted:result.values())
                    {
                        areAllGranted=areAllGranted && isGranted;
                    }
                    if(areAllGranted)
                    {
                      pickImageCamera();
                    }
                    else
                    {
                        Utils.toast(adcreate.this,"Camera or Storage or both Permission denied...");
                    }
                }
            }
    );

    private void pickImageGallery()
    {

        Log.d(TAG,"pickImageGallery");

        Intent intent=new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }
    private void pickImageCamera()
    {
        Log.d(TAG,"pickImageCamera:");

        ContentValues contentValues=new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"TEMPORARY_IMAGE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"TEMPORARY_IMAGE_DESCRIPTION");

        imageUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        cameraActivityResultLauncher.launch(intent);

    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher =registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result)
                {
                    if(result.getResultCode()== Activity.RESULT_OK)
                    {
                        Intent data=result.getData();
                        imageUri=data.getData();

                        Log.d(TAG,"onActivityResult:imageUri"+imageUri);

                        String timestamp=""+Utils.getTimeStamp();

                        ModelImagePicked modelImagePicked=new ModelImagePicked(timestamp,imageUri,null,false);
                        imagePickedArrayList.add(modelImagePicked);

                        loadImages();

                    }
                    else
                    {
                        Utils.toast(adcreate.this,"Cancelled...!");
                    }

                }
            }

    );
    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher=
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result)
                        {
                            if(result.getResultCode()== Activity.RESULT_OK)
                            {


                                Log.d(TAG,"onActivityResult:imageUri"+imageUri);

                                String timestamp=""+Utils.getTimeStamp();

                                ModelImagePicked modelImagePicked=new ModelImagePicked(timestamp,imageUri,null,false);
                                imagePickedArrayList.add(modelImagePicked);

                                loadImages();

                            }
                            else
                            {
                                Utils.toast(adcreate.this,"Cancelled...!");
                            }

                        }


                    }
            );

        private String brand="";
        private String category="";
        private String condition="";
        private String address="";
        private String price="";
        private String title="";
        private String description="";
        private double latitude=0;
        private double longtitude=0;
       private void validateData()
       {
         Log.d(TAG,"validateData:");

         brand=binding.brandEt.getText().toString().trim();
         category=binding.categoryAct.getText().toString().trim();
         condition=binding.conditionAct.getText().toString().trim();
         address=binding.locationAct.getText().toString().trim();
         price=binding.priceEt.getText().toString().trim();
         title=binding.titleEt.getText().toString().trim();
         description=binding.descriptionEt.getText().toString().trim();

         if(brand.isEmpty())
         {
             binding.brandEt.setError("Enter Brand");
             binding.brandEt.requestFocus();
         } else if (category.isEmpty()) {
             binding.categoryAct.setError("Choose Category");
             binding.categoryAct.requestFocus();
         } else if (condition.isEmpty()) {
             binding.conditionAct.setError("Choose Condition");
             binding.conditionAct.requestFocus();
         }/*else if (address.isEmpty()) {
             binding.locationAct.setError("Choose Location");
             binding.locationAct.requestFocus();

         }*/ else if (title.isEmpty()) {
             binding.titleEt.setError("Enter Title");
             binding.titleEt.requestFocus();
         }else if (description.isEmpty()) {
             binding.descriptionEt.setError("Enter Description");
             binding.descriptionEt.requestFocus();
         }/*else if (imagePickedArrayList.isEmpty()) {
             Utils.toast(this,"Pick at-least one image");
         }*/ else
         {
             postAd();
         }
       }

       private void postAd()
       {
           Log.d(TAG,"postAd:");

           progressDialog.setMessage("Publishing Ad");
           progressDialog.show();

           long timestamp=Utils.getTimeStamp();
           DatabaseReference refAds= FirebaseDatabase.getInstance().getReference("Ads");
           String keyId=refAds.push().getKey();

           HashMap<String,Object> hashmap=new HashMap<>();
           hashmap.put("id",""+keyId);
           hashmap.put("uid",""+firebaseAuth.getUid());
           hashmap.put("brand",""+brand);
           hashmap.put("category",""+category);
           hashmap.put("condition",""+condition);
           hashmap.put("address",""+address);
           hashmap.put("price",""+price);
           hashmap.put("title",""+title);
           hashmap.put("description",""+description);
           hashmap.put("status",""+Utils.AD_STATUS_AVAILABLE);
           hashmap.put("timestamp",timestamp);
           hashmap.put("latitude",latitude);
           hashmap.put("longitude",longtitude);

           refAds.child(keyId).
                   setValue(hashmap)
                   .addOnSuccessListener(new OnSuccessListener<Void>() {
                       @Override
                       public void onSuccess(Void unused) {
                           Log.d(TAG,"onSuccess:Ad Published");

                           uploadImagesStorage(keyId);


                       }
                   })
                   .addOnFailureListener(new OnFailureListener() {
                       @Override
                       public void onFailure(@NonNull Exception e) {
                           Log.e(TAG,"onFailure:",e);
                           progressDialog.dismiss();
                           Utils.toast(adcreate.this,"Failed to publish Ad due to"+e.getMessage());


                       }
                   });



       }
       private void uploadImagesStorage(String adId)
       {
           Log.d(TAG,"uploadImagesStorage:");
           for(int i=0;i<imagePickedArrayList.size();i++)
           {
               ModelImagePicked modelImagePicked=imagePickedArrayList.get(i);
               String imageName=modelImagePicked.getId();
               String filePathAndName="Ads/"+imageName;

               StorageReference storageReference= FirebaseStorage.getInstance().getReference(filePathAndName);

               int imageIndexForProgress=i+1;

               storageReference.putFile(modelImagePicked.getImageUri())
                       .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                           @Override
                           public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            double progress=(100.0*snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                            String message="Uploading.."+imageIndexForProgress+"of"+imagePickedArrayList.size()+"images...\nProgress"+(int)progress+"%";

                            progressDialog.setMessage(message);
                            progressDialog.show();

                           }
                       })
                       .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                           @Override
                           public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                               Log.d(TAG,"onSucess:");
                               Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                               while(!uriTask.isSuccessful());
                               Uri uploadedImageUrl=uriTask.getResult();

                               if(uriTask.isSuccessful()){
                                   HashMap<String,Object> hashMap=new HashMap<>();
                                   hashMap.put("id",""+modelImagePicked.imageUri);
                                   hashMap.put("imageUrl",""+uploadedImageUrl);

                                   DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Ads");
                                   ref.child(adId).child("Images")
                                           .child(imageName)
                                           .updateChildren(hashMap);

                               }
                               progressDialog.dismiss();

                           }
                       })
                       .addOnFailureListener(new OnFailureListener() {
                           @Override
                           public void onFailure(@NonNull Exception e) {
                               Log.e(TAG,"onFaliure",e);
                               progressDialog.dismiss();

                           }
                       });


           }
       }


}