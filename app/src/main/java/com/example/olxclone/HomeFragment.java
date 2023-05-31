package com.example.olxclone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.olxclone.databinding.FragmentHomeBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class HomeFragment extends Fragment {

    private Context mContext;

    private AdapterAd adapterAd;

    private ArrayList<ModelAd> adArrayList;

    private SharedPreferences locationsp;
    private FragmentHomeBinding binding;
    private static final String TAG="HOME_TAG";


    private static final int MAX_DISTANCE_TO_LOAD_ADS_KM=10;

    private double currentLatitude=0.0;
    private double currentLongtitude=0.0;

    private String currentAddress="";

    @Override
    public void onAttach(@NonNull Context context) {
        mContext=context;
        super.onAttach(context);
    }

    public HomeFragment() {
        // Required empty public constructor
    }





    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding =FragmentHomeBinding.inflate(LayoutInflater.from(mContext),container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationsp=mContext.getSharedPreferences("LOCATION_SP",Context.MODE_PRIVATE);
        currentLatitude=locationsp.getFloat("CURRENT_LATITUDE",0.0f);
        currentLongtitude=locationsp.getFloat("CURRENT_LONGTITUDE",0.0f);
        currentAddress=locationsp.getString("CURRENT_ADDRESS","");

        if(currentLatitude !=0.0 && currentLongtitude !=0.0)
        {
          binding.locationTv.setText(currentAddress);
        }
        loadCategories();

        loadAds("All");

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                Log.d(TAG,"onTextChanged:Query"+s);

                try {
                    String query=s.toString();
                    adapterAd.getFilter().filter(query);
                }catch (Exception e)
                {
                    Log.e(TAG,"onTextChanged",e);
                }

            }


            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        binding.locationCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(mContext,LocationPickerActivity.class);
                locationPickerActivityResult.launch(intent);
            }
        });
    }
    private ActivityResultLauncher<Intent> locationPickerActivityResult=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {

            if(result.getResultCode()== Activity.RESULT_OK)
            {
               Log.d(TAG,"onActivityResult:RESULT_OK");
               Intent data=result.getData();

               if(data!=null)
               {
                   Log.d(TAG,"onActivityResult:Location Picked");
                   currentLatitude=data.getDoubleExtra("latitude",0.00);
                   currentLongtitude=data.getDoubleExtra("longitude",0.00);
                   currentAddress=data.getStringExtra("address");

                   locationsp.edit()
                           .putFloat("CURRENT_LATITUDE", Float.parseFloat(""+currentLatitude))
                   .putFloat("CURRENT_LONGTITUDE",Float.parseFloat(""+currentLongtitude))
                           .putString("CURRENT_ADDRESS",currentAddress)
                   .apply();

                   binding.locationTv.setText(currentAddress);


               }
            }else
            {
                Log.d(TAG,"onActivityResult:Cancelled!");
                Utils.toast(mContext,"Cancelled");
            }

        }
    });

    private void loadCategories()
    {
        ArrayList<ModelCategory> categoryArrayList=new ArrayList<>();

        ModelCategory modelCategoryAll=new ModelCategory("All",R.drawable.ic_category_all);
        categoryArrayList.add(modelCategoryAll);

        for(int i=0;i<Utils.categories.length;i++)
        {
            ModelCategory modelCategory=new ModelCategory(Utils.categories[i], Utils.categoryIcons[i]);
            categoryArrayList.add(modelCategory);
        }
        AdapterCategory adapterCategory=new AdapterCategory(mContext, categoryArrayList, new RvListenerCategory() {
            @Override
            public void onCategoryClick(ModelCategory modelCategory) {

                loadAds(modelCategory.getCategory());

            }
        });
        binding.categoryRV.setAdapter(adapterCategory);
    }

    private void loadAds(String category)
    {
       Log.d(TAG,"loadAds : Category"+category);

       adArrayList=new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                adArrayList.clear();

                for (DataSnapshot ds:snapshot.getChildren())
                {
                    ModelAd modelAd=ds.getValue(ModelAd.class);
                    double distance=calculateDistanceKm(modelAd.getLatitude(),modelAd.getLongtitude());

                    Log.d(TAG,"onDataChange:distance"+distance);

                    if(category.equals("All"))
                    {
                        if(distance<=MAX_DISTANCE_TO_LOAD_ADS_KM)
                        {
                            adArrayList.add(modelAd);
                        }

                    }else
                    {
                        if(modelAd.category.equals(category))
                        {
                            if(distance<=MAX_DISTANCE_TO_LOAD_ADS_KM)
                            {
                                adArrayList.add(modelAd);
                            }
                        }
                    }
                    
                }
                adapterAd=new AdapterAd(mContext,adArrayList);
                binding.adsRv.setAdapter(adapterAd);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private double calculateDistanceKm(double adlatitude, double adlongtitude) {

        Log.d(TAG,"calculateDistanceKM: currentlatitude"+currentLatitude);
        Log.d(TAG,"calculateDistanceKM: currentlongtitude"+currentLongtitude);
        Log.d(TAG,"calculateDistanceKM: adLatitude"+adlatitude);
        Log.d(TAG,"calculateDistanceKM: adLongtitude"+adlongtitude);

        Location startpoint=new Location(LocationManager.NETWORK_PROVIDER);
        startpoint.setLatitude(currentLatitude);
        startpoint.setLongitude(currentLongtitude);


        Location endPoint=new Location(LocationManager.NETWORK_PROVIDER);
        endPoint.setLatitude(adlatitude);
        endPoint.setLongitude(adlongtitude);


       double distanceInMeters=startpoint.distanceTo(endPoint);
       double distanceInKM=distanceInMeters/1000;

       return  distanceInKM;

    }


}