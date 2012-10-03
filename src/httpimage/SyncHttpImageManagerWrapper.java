//  Copyright 2012 Zonghai Li. All rights reserved.
//
//  Redistribution and use in binary and source forms, with or without modification,
//  are permitted for any project, commercial or otherwise, provided that the
//  following conditions are met:
//  
//  Redistributions in binary form must display the copyright notice in the About
//  view, website, and/or documentation.
//  
//  Redistributions of source code must retain the copyright notice, this list of
//  conditions, and the following disclaimer.
//
//  THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
//  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
//  PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
//  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
//  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THIS SOFTWARE.


package httpimage;

import httpimage.HttpImageManager.LoadRequest;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;



/**
 * 
 * Wrapper around HttpImageManager to provide synchronous loading behavior.
 * 
 * @author zonghai
 *
 */
public class SyncHttpImageManagerWrapper {

    private static final String TAG = "SyncHttpImageManagerWrapper";
    private static final boolean DEBUG = true;


    public SyncHttpImageManagerWrapper(HttpImageManager mgr) {
        mManager = mgr;
    }


    public Bitmap syncLoadImage ( Uri uri ) {

        mCompleted = false;
        Bitmap bitmap = mManager.loadImage(new HttpImageManager.LoadRequest(uri, new HttpImageManager.OnLoadResponseListener() {

            @Override
            public void onLoadResponse(LoadRequest r, Bitmap data) {
                synchronized (mLock) {
                    mBitmap = data;
                    mCompleted = true;
                    mLock.notifyAll();
                }

            }

            @Override
            public void onLoadError(LoadRequest r, Throwable e) {
                synchronized (mLock) {
                    mException = e;
                    mCompleted = true;
                    mLock.notifyAll();
                }
            }

            
            @Override
            public void onLoadProgress(LoadRequest r, long totalContentSize,
                    long loadedContentSize) {
            }

        }));

        if (bitmap != null)  return bitmap;
        
        synchronized ( mLock ) {
            if ( mBitmap != null ) return mBitmap;
            while ( !mCompleted ) {
                try {
                    if(DEBUG)  Log.d(TAG, "waiting for the request to be completed ");
                    mLock .wait();
                } catch (InterruptedException e1) {}
            }

        }
        if ( mException != null ) 
            throw new RuntimeException (mException);

        return mBitmap;
    }


    ////////PRIVATE
    private HttpImageManager mManager;
    private Object     mLock = new Object();
    private Bitmap  mBitmap;
    private Throwable mException;
    private boolean mCompleted;
}
