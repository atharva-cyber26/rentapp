package com.example.olxclone;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;


//A class that will contain static functions, constants, variables that we will be used in whole application
    public class Utils {

        /** A Function to show Toast
         *
         * @param context the context of activity/fragement from where this function will be called
         * @param message the message to be shown in the Toast
         *
         *
         */

        public static final String AD_STATUS_AVAILABLE="AVAILABLE";
        public static final String AD_STATUS_SOLD="SOLD";

        public static final String[] categories={
                "Mobiles",
                "Computer/Laptop",
                "Electronics & Home Appliances",
                "Vehicles",
                "Furniture & Home Decor",
                "Fashion & Beauty",
                "Books",
                "Sports",
                "Agriculture"
        };
        public static final String[] conditions={"New","Used","Refurbished"};
    public static void toast(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static final int[] categoryIcons={
            R.drawable.ic_category_mobile,
            R.drawable.ic_category_laptop,
            R.drawable.ic_category_electronics,
            R.drawable.ic_category_car,
            R.drawable.ic_category_furniture,
            R.drawable.ic_category_fashion,
            R.drawable.ic_category_book,
            R.drawable.ic_category_sports,
            R.drawable.ic_category_agriculture
    };

        /** A function to get current timestamp
         * @return Return the current timestamp as long datatype
         */
    public static long getTimeStamp(){

        return System.currentTimeMillis();
    }

    public static String formatTimestampData(Long timestamp){
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);

        String date = DateFormat.format("dd/MM/yyyy", calendar).toString();

        return date;
    }

    public  static void addToFavourite(Context context, String adId){
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser()==null){
            Utils.toast(context,"You're not logged in! ");
        }
        else {
            long timestamp = Utils.getTimeStamp();

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("adId",adId);
            hashMap.put("timestamp",timestamp);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favourites").child(adId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Utils.toast(context,"Added to Favourites");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Utils.toast(context,"Failed to add to Favourites");
                        }
                    });

        }
    }

    public static void removeFromFavourite(Context context, String adId){

        FirebaseAuth firebaseAuth =  FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser()==null){
            Utils.toast(context,"You're not logged in! ");
        }
        else{
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favourites").child(adId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {

                            Utils.toast(context,"Removed Successfully");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Utils.toast(context,"Failed to remove from favourites"+ e.getMessage());
                        }
                    });
        }
    }
}
