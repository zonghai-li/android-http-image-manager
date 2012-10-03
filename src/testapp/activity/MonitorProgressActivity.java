package testapp.activity;

import httpimage.FileSystemPersistence;
import httpimage.HttpImageManager;
import httpimage.HttpImageManager.LoadRequest;
import testapp.activity.R.id;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MonitorProgressActivity extends Activity implements HttpImageManager.OnLoadResponseListener{

	private static final String TAG = "MonitorProgressActivity";
	ImageView mImageView;
	ProgressBar mProgress;
	TextView mTextView;
	Bitmap mBitmap;
	Bitmap mLogo;
	
	private Handler mHandler = new Handler();

	static private String URL = "http://221.226.91.34:8089/file/vote_picture/7/2/727908884333108.jpg";
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progressive_image_loading);
        
        mImageView = (ImageView) findViewById(id.imageView1);
        mProgress = (ProgressBar) findViewById(id.progressBar1);
        mTextView = (TextView) findViewById(id.textView1);
        
        mLogo = BitmapFactory.decodeResource(getResources(), R.drawable.watermark);
        
        findViewById(id.button1).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (mBitmap != null && !mBitmap.isRecycled()) 
					mBitmap.recycle();
				
				mImageView.setImageBitmap(null);
				
				HttpImageManager imageManager = new HttpImageManager(new FileSystemPersistence(TestApplication.BASEDIR) );
				
				//use filter to alter images obtained from network
				imageManager.setBitmapFilter(new HttpImageManager.BitmapFilter() {
					@Override
					public Bitmap filter(Bitmap in) {
						return createWaterBitmap (in, mLogo);
					}
				});
				
				Uri picUri = Uri.parse(URL);
				
				imageManager.loadImage( new HttpImageManager.LoadRequest(picUri, MonitorProgressActivity.this) );
			}
		});
        
    }
    

    public void onStop () 
    {
    	super.onStop();
    	
    	if (mBitmap != null && !mBitmap.isRecycled()) 
			mBitmap.recycle();
    }
    

	@Override
	public void onLoadResponse(LoadRequest r, Bitmap data) {
		
		mBitmap = data;
		mHandler.post( new Runnable() {

			@Override
			public void run() {
				mImageView.setImageBitmap(mBitmap);
			}
			
		});
		
	}

	
	@Override
	public void onLoadProgress(LoadRequest r, final long totalContentSize,
			final long loadedContentSize) {
		
		Log.d(TAG, "progress: " + loadedContentSize + "/" + totalContentSize);
		
		mHandler.post( new Runnable() {

			@Override
			public void run() {
				mTextView.setText("" + (loadedContentSize * 100 / totalContentSize ) + "%");
			}
			
		});
		
		mProgress.setMax((int)totalContentSize);
		mProgress.setProgress((int)loadedContentSize);
	}
	

	@Override
	public void onLoadError(LoadRequest r, Throwable e) {
		
	}

	
	/**
	 * 　　* create the bitmap from a byte array 　　* 　　* @param src the bitmap
	 * object you want proecss 　　* @param watermark the water mark above the src
	 * 　　* @return return a bitmap object ,if paramter's length is 0,return null
	 * 　　
	 */
	static public Bitmap createWaterBitmap(Bitmap src, Bitmap watermark) {

		if (src == null)
			return null;

		int w = src.getWidth();
		int h = src.getHeight();
		int ww = watermark.getWidth();
		int wh = watermark.getHeight();
		Bitmap newb = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		Canvas cv = new Canvas(newb);

		cv.drawBitmap(src, 0, 0, null);
		Paint paintClip = new Paint();
		paintClip.setAntiAlias(true);
		int x = ww / wh;
		Rect dstLeft = new Rect();
		dstLeft.left = w / 4 * 3;
		dstLeft.right = w / 4 * 3 + w / 4;
		dstLeft.top = h / 20;
		dstLeft.bottom = dstLeft.top + (dstLeft.right - dstLeft.left) / x;
		cv.drawBitmap(watermark, null, dstLeft, paintClip);
		cv.save(Canvas.ALL_SAVE_FLAG);
		cv.restore();
		return newb;
	}
    
}
