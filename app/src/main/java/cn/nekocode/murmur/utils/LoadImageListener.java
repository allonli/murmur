package cn.nekocode.murmur.utils;

import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import cn.nekocode.murmur.R;

/**
 * Created by Nekocode on 2014/11/23 0023.
 */
public class LoadImageListener implements ImageLoader.ImageListener {
    private ImageView imageView;
    private String imageUrl;

    public LoadImageListener(ImageView imageView, String imageUrl) {
        imageView.setTag(imageUrl);
        this.imageView = imageView;
        this.imageUrl = imageUrl;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        imageView.setImageResource(R.drawable.transparent);
    }

    @Override
    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
        if(imageUrl.equals((String)imageView.getTag())) {
            if (response.getBitmap() != null) {
                imageView.setImageBitmap(response.getBitmap());
            } else {
                imageView.setImageResource(R.drawable.transparent);
            }
        }
    }
}
