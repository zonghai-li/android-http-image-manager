package testapp.activity;

import httpimage.HttpImageManager;
import httpimage.SyncHttpImageManagerWrapper;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;



public class TestActivity extends Activity {
	
	private List<Uri> mUriList ;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        if( mUriList == null ) {
        	mUriList = new ArrayList<Uri> ();
        	String[] urls = this.getResources().getStringArray(R.array.urllist);
    		for ( String i : urls) {
    			mUriList .add( Uri.parse( i ));
    		}
        }
		
        ListView listview = (ListView)findViewById(R.id.listView);
        TestListAdapter listAdapter = new TestListAdapter(this, mUriList);
        listview.setAdapter(listAdapter);
        
        Gallery gallery = (Gallery)findViewById(R.id.gallery);
        gallery.setAdapter(new ImageAdapter());
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.addSubMenu(0, 0, 0, "Test sync loading");
    	menu.addSubMenu(0, 1, 0, "Test progressive loading");
    	return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int id = item.getItemId();
    	if(id==0)
    	{
    		TextView textView = new TextView(this);
    		
    		String html = "Matrix: <img src='http://221.226.91.34:8089/file/vote_picture/7/2/727908884333108.jpg'/>";
    		Spanned s = Html.fromHtml(html, new ImageGetter() {

				@Override
				public Drawable getDrawable(String src) {
					SyncHttpImageManagerWrapper syncloader = new SyncHttpImageManagerWrapper(getHttpImageManager());
					
					try {
						Bitmap bitmap = syncloader.syncLoadImage(Uri.parse(src));
						Drawable d = new BitmapDrawable(bitmap);
						d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
						
						return d;
					}
					catch (RuntimeException e) {
						return null;
					}
				}
    			
    		}, null);
    		
    		textView.setText(s);
    		
    		new AlertDialog .Builder(this).setView(textView).create().show();    		
    	}
    	
    	else if ( id == 1) {
    		
    		Intent i = new Intent(TestActivity.this, MonitorProgressActivity.class);
    		startActivity(i);
    	}
    	
    	return false;
    }


    public HttpImageManager getHttpImageManager () {
    	return ((TestApplication) getApplication()).getHttpImageManager();
    }
    
    
    private class ImageAdapter extends BaseAdapter {
        
        public int getCount() {
            return mUriList.size();
        }

        public Object getItem(int position) {
            return mUriList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }


		@Override
		public View getView(int position, View arg1, ViewGroup arg2) {
			ImageView i = new ImageView(TestActivity.this);
			i.setScaleType(ImageView.ScaleType.FIT_XY);
            i.setLayoutParams(new Gallery.LayoutParams(96, 96));
            i.setPadding(1, 1,1 ,1);
            i.setBackgroundColor(-1);
            
			final Uri uri = (Uri) getItem(position);
			
			i.setImageResource(R.drawable.default_image);
			if(uri != null){
				Bitmap bitmap = getHttpImageManager() .loadImage(new HttpImageManager.LoadRequest(uri, i));
				if (bitmap != null) {
					i.setImageBitmap(bitmap);
			    }
			}
            
            return i;
		}

    }

}