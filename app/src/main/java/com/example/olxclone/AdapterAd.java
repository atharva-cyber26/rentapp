package com.example.olxclone;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.olxclone.databinding.RowAdBinding;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdapterAd extends RecyclerView.Adapter<AdapterAd.HolderAd> implements Filterable {

    private RowAdBinding binding;

    private static final String TAG="ADAPTER_AD_TAG";

    private FirebaseAuth firebaseAuth;

    private Context context;
    public ArrayList<ModelAd> adArrayList;
    private ArrayList<ModelAd> filterList;

    private FilterAd filter;



    public AdapterAd(Context context, ArrayList<ModelAd> adArrayList) {
        this.context = context;
        this.adArrayList = adArrayList;
        this.filterList=adArrayList;

        firebaseAuth=FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderAd onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding=RowAdBinding.inflate(LayoutInflater.from(context),parent,false);
        return new HolderAd(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderAd holder, int position) {
        ModelAd modelAd=adArrayList.get(position);

        String title= modelAd.getTitle();
        String description=modelAd.getDescription();
        String address= modelAd.getAddress();
        String condition= modelAd.getCondition();
        String price= modelAd.getPrice();
        long timestamp=modelAd.getTimestamp();
        String formattedDate=Utils.formatTimestampData(timestamp);

        loadAdFirstImage(modelAd,holder);

        if (firebaseAuth.getCurrentUser() != null){

            checkIsFavourite(modelAd,holder);

        }

        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.addressTv.setText(address);
        holder.conditionTv.setText(condition);
        holder.priceTv.setText(price);
        holder.dateTv.setText(formattedDate);

        holder.favBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean favourite = modelAd.isFavorite();

                if (favourite){
                    Utils.removeFromFavourite(context,modelAd.getId());
                }
                else{
                    Utils.addToFavourite(context,modelAd.getId());
                }
            }
        });

    }

    private void checkIsFavourite(ModelAd modelAd, HolderAd holder) {

        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favourites").child(modelAd.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        boolean favourite = snapshot.exists();

                        modelAd.setFavorite(favourite);

                        if (favourite){

                            holder.favBtn.setImageResource(R.drawable.ic_fav_yes);
                        }
                        else{
                            holder.favBtn.setImageResource(R.drawable.ic_fav_no);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadAdFirstImage(ModelAd modelAd, HolderAd holder) {
        Log.d(TAG,"loadAdFirstImage:");

        String adId=modelAd.getId();

        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Ads");
        reference.child(adId).child("Images").limitToFirst(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot ds:snapshot.getChildren())
                        {
                            String imageUrl=""+ ds.child("imageUrl").getValue();
                            Log.d(TAG,"onDataChange:imageUrl:"+ imageUrl);

                            try {
                                Glide.with(context)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_image_gray)
                                        .into(holder.imageIv);

                            }catch (Exception e)
                            {
                                Log.e(TAG,"onDataChange:",e);
                            }

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        Log.d(TAG, "onCancelled: Cancelled");
                    }
                });


    }

    @Override
    public int getItemCount() {
        return adArrayList.size();
    }

    @Override
    public Filter getFilter() {
        if(filter==null) {
            filter =new FilterAd(this,filterList);
        }
        return filter;
    }


    class HolderAd extends RecyclerView.ViewHolder{

        ShapeableImageView imageIv;

        TextView titleTv,descriptionTv,addressTv,conditionTv,priceTv,dateTv;
        ImageButton favBtn;

        public HolderAd(@NonNull View itemView) {
            super(itemView);

            imageIv=binding.imageIv;
            titleTv=binding.titleTv;
            descriptionTv=binding.descriptionTv;
            favBtn=binding.favBtn;
            addressTv=binding.addressTv;
            conditionTv=binding.conditionTv;
            priceTv=binding.priceTv;
            dateTv=binding.dateTv;


        }
    }
}
