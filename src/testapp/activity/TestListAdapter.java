package testapp.activity;

import httpimage.HttpImageManager;

import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;



public class TestListAdapter extends ArrayAdapter<Uri>  {
	
	private LayoutInflater mInflater;
	private HttpImageManager mHttpImageManager;
	
	
	public TestListAdapter(Activity context, List<Uri> uris) {
		super(context, 0, uris);
		mInflater = context.getLayoutInflater();
		mHttpImageManager = ((TestApplication) context.getApplication()).getHttpImageManager();
	}
	
	
	public View getView(int position, View convertView, ViewGroup parent) {
		
		final ViewHolder holder;
		
		if (convertView == null || convertView.getTag() == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.listitem, null);
			holder.url = (TextView) convertView.findViewById(R.id.url);
			holder.image = (ImageView) convertView.findViewById(R.id.image);
	        convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		final Uri uri = getItem(position);
			
		ImageView imageView = holder.image;
		imageView.setImageResource(R.drawable.default_image);
		if(uri != null){
			Bitmap bitmap = mHttpImageManager.loadImage(new HttpImageManager.LoadRequest(uri, imageView));
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
		    }
		}
		
		holder.url.setText(uri.toString());
		
		return convertView;
	}
	
	
	private static class ViewHolder {
        ImageView image;
        TextView url;
    }
}
