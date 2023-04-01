package com.lesa_humdet;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;
import java.util.List;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AlertDialog;


public class CustomArrayAdapter extends BaseAdapter{
    private LayoutInflater inflater;
    private List<String[]>url;
    private List<JSONObject[]> jsonObjects;
    String jsonData;
    Context context;
    Conf conf = new Conf();
    public CustomArrayAdapter(Context context,List<String[]> url, List<JSONObject[]> jsonObjects,String jsonData) {
        this.url = url;
        this.jsonObjects = jsonObjects;
        this.jsonData = jsonData;
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return url.size();
    }

    @Override
    public Object getItem(int position) {
        return url.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView  = inflater.inflate(R.layout.custom_list, parent, false);
        ImageView imageView2 =  convertView.findViewById(R.id.imageView2);
        ImageView imageView3 =  convertView.findViewById(R.id.imageView3);
        ImageView imageView4 =  convertView.findViewById(R.id.imageView4);

        String [] urls = url.get(position);

        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context,DetailActivity.class);
                intent.putExtra("jsonObject",jsonObjects.get(position)[0].toString());
                intent.putExtra("position",position);
                intent.putExtra("allJsonObject",jsonData);
                context.startActivity(intent);
            }
        });
        imageView2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showAlertDialog(urls[0]);
                return true;
            }
        });

        if(jsonObjects.get(position)[1]!=null){
            try{
                imageView3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context,DetailActivity.class);
                        intent.putExtra("jsonObject",jsonObjects.get(position)[1].toString());
                        intent.putExtra("position",position+1);
                        intent.putExtra("allJsonObject",jsonData);
                        context.startActivity(intent);
                    }
                });
                imageView3.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        showAlertDialog(urls[1]);
                        return true;
                    }
                });
            }catch (Exception e){}
        }
        if(jsonObjects.get(position)[2]!=null){
            try{
                imageView4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context,DetailActivity.class);
                        intent.putExtra("jsonObject",jsonObjects.get(position)[2].toString());
                        intent.putExtra("position",position+2);
                        intent.putExtra("allJsonObject",jsonData);
                        context.startActivity(intent);
                    }
                });
                imageView4.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        showAlertDialog(urls[2]);
                        return true;
                    }
                });
            }catch (Exception e){}
        }


        Picasso.with(context)
                .load(conf.getDomen()+"image?imgname="+urls[0]+"_SMALL.jpg/")
                .placeholder(R.drawable.progress_animation)
                .error(R.drawable.person_ic)
                .fit().centerCrop()
                .into(imageView2, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                    }
                });

        if(urls[1]!=null){
            Picasso.with(context)
                    .load(conf.getDomen()+"image?imgname="+urls[1]+"_SMALL.jpg/")
                    .placeholder(R.drawable.progress_animation)
                    .error(R.drawable.person_ic)
                    .fit().centerCrop()
                    .into(imageView3, new Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError() {
                        }
                    });
        }else{
            imageView3.setVisibility(View.INVISIBLE);
        }
        if(urls[2]!=null){
            Picasso.with(context)
                    .load(conf.getDomen()+"image?imgname="+urls[2]+"_SMALL.jpg/")
                    .placeholder(R.drawable.progress_animation)
                    .error(R.drawable.person_ic)
                    .fit().centerCrop()
                    .into(imageView4, new Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError() {
                        }
                    });
        }else{
            imageView4.setVisibility(View.INVISIBLE);
        }



        return convertView;

    }

    @Override
    public boolean isEnabled(int position)
    {
        return true;
    }
    public int color(@ColorRes int resId) {
        return context.getResources().getColor(resId);
    }

    AlertDialog alertDialog;
    ImageView dialogImageView;



    public void alertDialogBuilder(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        View layoutView = inflater.inflate(R.layout.img_dialog, null);
        dialogImageView = layoutView.findViewById(R.id.imageView5);
        dialogBuilder.setView(layoutView);
        alertDialog = dialogBuilder.create();
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }
    private void showAlertDialog(String imgName){
        Picasso.with(context)
                .load(conf.getDomen()+"image?imgname="+imgName+".jpg/")
                .placeholder(R.drawable.progress_animation)
                .error(R.drawable.person_ic)
                .fit().centerCrop()
                .into(dialogImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                    }
                });
        alertDialog.show();
    }

}