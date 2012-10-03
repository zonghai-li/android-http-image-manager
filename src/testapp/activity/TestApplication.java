package testapp.activity;

import httpimage.*;

public class TestApplication extends android.app.Application {

	public static final String BASEDIR = "/sdcard/httpimage";
	
	
	@Override
	public void onCreate() {
		super.onCreate();

		// init HttpImageManager manager.
		mHttpImageManager = new HttpImageManager(HttpImageManager.createDefaultMemoryCache(), 
				new FileSystemPersistence(BASEDIR));
	}

	
	public HttpImageManager getHttpImageManager() {
		return mHttpImageManager;
	}


	//////PRIVATE
	private HttpImageManager mHttpImageManager; 
}
