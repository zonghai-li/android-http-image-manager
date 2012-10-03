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


import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;


/**
 * HttpImageManager uses 3-level caching to download and store network images.
 * <p>
 *     ---------------<br>
 *     memory cache<br>
 *     ---------------<br>
 *     persistent storage (DB/FS)<br>
 *     ---------------<br>
 *     network loader<br>
 *     ---------------
 *     
 * <p>
 * HttpImageManager will first look up the memory cache, return the image bitmap if it was already
 * cached in memory. Upon missing, it will further look at the 2nd level cache, 
 * which is the persistence layer. It only goes to network if the resource has never been downloaded.
 * 
 * <p>
 * The downloading process is handled in asynchronous manner. To get notification of the response, 
 * one can add an OnLoadResponseListener to the LoadRequest object.
 * 
 * <p>
 * HttpImageManager is usually used for ImageView to display a network image. To simplify the code, 
 * One can register an ImageView object as target to the LoadRequest instead of an 
 * OnLoadResponseListener. HttpImageManager will try to feed the loaded resource to the target ImageView
 * upon successful download. Following code snippet shows how it is used in a customer list adapter.
 * 
 * <p>
 * <pre>
 *         ...
 *         String imageUrl = userInfo.getUserImage();
 *         ImageView imageView = holder.image;
 * 
 *         imageView.setImageResource(R.drawable.default_image);
 * 
 *         if(!TextUtils.isEmpty(imageUrl)){
 *             Bitmap bitmap = mHttpImageManager.loadImage(new HttpImageManager.LoadRequest(Uri.parse(imageUrl), imageView));
 *            if (bitmap != null) {
 *                imageView.setImageBitmap(bitmap);
 *            }
 *        }
 *
 * </pre>
 * 
 * 
 * @author zonghai@gmail.com
 */
public class HttpImageManager{

    private static final String TAG = "HttpImageManager";
    private static final boolean DEBUG = false;

    public static final int DEFAULT_CACHE_SIZE = 64;
    public static final int UNCONSTRAINED = -1;
    public static final int DECODING_MAX_PIXELS_DEFAULT = 600 * 800;


    public static class LoadRequest {
        public LoadRequest (Uri uri) {
            this(uri, null, null);
        }


        public LoadRequest(Uri uri, ImageView v){
            this(uri, v, null);
        }


        public LoadRequest(Uri uri, OnLoadResponseListener l){
            this( uri, null, l);
        }


        public LoadRequest(Uri uri, ImageView v, OnLoadResponseListener l){
            if(uri == null) 
                throw new NullPointerException("uri must not be null");

            mUri = uri;
            mHashedUri = computeHashedName(uri.toString());
            mImageView = v;
            mListener = l;
        }


        public ImageView getImageView() {
            return mImageView;
        }


        public Uri getUri() {
            return mUri;
        }


        public String getHashedUri () {
            return this.mHashedUri;
        }


        @Override 
        public int hashCode() {
            return mUri.hashCode();
        }


        @Override 
        public boolean equals(Object b){
            if(b instanceof LoadRequest)
                return mUri.equals(((LoadRequest)b).getUri());

            return false;
        }

        /* Hex representation of the hash over input name */
        private String computeHashedName (String name) {
            try {
                MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                digest.update(name.getBytes());
                
                byte[] result = digest.digest();
                return String.format("%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
                        result[0], result[1], result[2], result[3], result[4], result[5], result[6], result[7],
                        result[8],result[9], result[10], result[11],result[12], result[13], result[14], result[15]);
            } 
            catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }


        private Uri mUri;
        private String mHashedUri;

        private OnLoadResponseListener mListener;
        private ImageView mImageView;
    }


    public static interface OnLoadResponseListener {
        public void onLoadResponse(LoadRequest r, Bitmap data);
        public void onLoadProgress(LoadRequest r, long totalContentSize, long loadedContentSize);
        public void onLoadError(LoadRequest r, Throwable e);
    }

    
    /**
     * 
     * Give a chance to apply any future processing on the bitmap retrieved from network. 
     */
    public static interface BitmapFilter {
        public Bitmap filter ( final Bitmap in );
    }
    

    ////////HttpImageManager
    public HttpImageManager (BitmapCache cache,  BitmapCache persistence ) {
        mCache = cache;
        mPersistence = persistence;
        if (mPersistence == null) {
            throw new IllegalArgumentException (" persistence layer should be specified");
        }
    }


    public HttpImageManager ( BitmapCache persistence ) {
        this(null, persistence);
    }


    public void setDecodingPixelConstraint (int max) {
        mMaxNumOfPixelsConstraint = max;
    }
    
    
    public int getDecodingPixelConstraint()
    {
        return mMaxNumOfPixelsConstraint;
    }
    
    
    public void setBitmapFilter (BitmapFilter filter) {
        mFilter = filter;
    }
    
    
    static public BitmapCache createDefaultMemoryCache() {
        return new BasicBitmapCache(DEFAULT_CACHE_SIZE);
    }


    public Bitmap loadImage(Uri uri) {
        return loadImage(new LoadRequest(uri));
    }


    /**
     * Nonblocking call, return null if the bitmap is not in cache.
     * @param r
     * @return
     */
    public Bitmap loadImage( LoadRequest r ) {
        if(r == null || r.getUri() == null || TextUtils.isEmpty(r.getUri().toString())) 
            throw new IllegalArgumentException( "null or empty request");

        ImageView iv = r.getImageView();
        if(iv != null){
            synchronized ( iv ) {
                iv.setTag(r.getUri()); // bind URI to the ImageView, to prevent image write-back of earlier requests.
            }
        }

        String key = r.getHashedUri();

        if(mCache != null && mCache.exists(key)) {
            return mCache.loadData(key);
        }
        else { 
            // not ready yet, try to retrieve it asynchronously.
            mExecutor.execute( newRequestCall(r));
            return null;
        }
    }


    ////PRIVATE
    private Runnable newRequestCall(final LoadRequest request) {
        return new Runnable() {

            public void run() {

                // if the request dosen't represent the intended ImageView, do nothing.
                if(request.getImageView() != null) {
                    final ImageView iv = request.getImageView();
                    synchronized ( iv ) {
                        if ( iv.getTag() != request.getUri() ) {
                            if(DEBUG)  Log.d(TAG, "give up loading: " + request.getUri().toString());
                            return;
                        }
                    }
                }

                synchronized (mActiveRequests) {
                    // If there's been already request pending for the same URL, we just wait until it is handled.
                    while (mActiveRequests.contains(request)) {
                        try {
                            mActiveRequests.wait();
                        } catch(InterruptedException e) {}
                    }

                    mActiveRequests.add(request);
                }

                Bitmap data = null;
                String key = request.getHashedUri();

                try {
                    //first we lookup memory cache
                    if (mCache != null)
                        data = mCache.loadData(key);

                    if(data == null) {
                        if(DEBUG)  Log.d(TAG, "cache missing " + request.getUri().toString());
                        //then check the persistent storage
                        data = mPersistence.loadData(key);
                        if(data != null) {
                            if(DEBUG)  Log.d(TAG, "found in persistent: " + request.getUri().toString());
                            
                            // load it into memory
                            if (mCache != null)
                                mCache.storeData(key, data);

                            fireLoadProgress(request, 1, 1); // fire progress done
                        }
                        else {
                            // we go to network
                            if(DEBUG)  Log.d(TAG, "go to network " + request.getUri().toString());
                            long millis = System.currentTimeMillis();
                            
                            byte[] binary = null;
                            HttpResponse httpResp = mNetworkResourceLoader.load(request.getUri());

                            if(DEBUG) {
                                Header[] headers = httpResp.getAllHeaders();
                                for (Header header :headers) {
                                    Log.i(TAG, header.toString());
                                }
                            }

                            HttpEntity entity = httpResp.getEntity();
                            if (entity != null) {
                                InputStream responseStream = entity.getContent();
                                try {
                                    Header header = entity.getContentEncoding();
                                    if (header != null && header.getValue() != null && header.getValue().contains("gzip")) {
                                        responseStream =  new GZIPInputStream(responseStream);
                                    }

                                    responseStream = new FlushedInputStream(responseStream); //patch the inputstream
                                    
                                    long contentSize = entity.getContentLength();
                                    binary = readInputStreamProgressively(responseStream, (int)contentSize, request);
                                    data = BitmapUtil.decodeByteArray(binary, mMaxNumOfPixelsConstraint);
                                } 
                                finally {
                                    if(responseStream != null) {
                                        try { responseStream.close(); } catch (IOException e) {}
                                    }
                                }
                            }

                            if(data == null) 
                                throw new RuntimeException("data from remote can't be decoded to bitmap");

                            if(DEBUG) Log.d(TAG, "decoded image: " + data.getWidth() + "x" + data.getHeight() );
                            if(DEBUG) Log.d(TAG, "time consumed: " + (System.currentTimeMillis() - millis));

                            //apply filter(s)
                            if (mFilter != null) {
                                try {
                                    Bitmap newData = mFilter.filter(data);
                                    if (newData != null) data = newData;
                                }
                                catch (Throwable e) {}
                            }
                            
                            // load it into memory
                            if (mCache != null)
                                mCache.storeData(key, data);

                            // persist it. Save the file as-is, preserving the format.
                            mPersistence.storeData(key, binary);
                        }
                    }

                    if(data != null && request.getImageView() != null) {
                        final Bitmap finalData = data;
                        final ImageView iv = request.getImageView();

                        synchronized ( iv ) {
                            if ( iv.getTag() == request.getUri() ) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if ( iv.getTag() == request.getUri()) {
                                            iv.setImageBitmap(finalData);
                                        }
                                    }
                                });
                            }
                        }
                    }

                    // callback listener if any
                    fireLoadResponse(request, data);
                }
                catch (Throwable e) {
                    fireLoadFailure(request, e);
                    if(DEBUG) Log.e(TAG, "error handling request " + request.getUri(), e);
                }
                finally{
                    synchronized (mActiveRequests) {
                        mActiveRequests.remove(request);
                        mActiveRequests.notifyAll();  // wake up pending requests who's querying the same URL. 
                    }

                    if (DEBUG) Log.d(TAG, "finished request for: " + request.getUri());
                }
            }
        };
    }


    /**
     * Make memory cache empty, release all bitmap reference held. 
     */
    public void emptyCache () {
        if ( mCache != null) 
            mCache .clear();
    }


    /**
     * Remove the persistent data. This is a blocking call. 
     */
    public void emptyPersistence () {
        if (mPersistence != null)
            mPersistence .clear();
    }


    ////////PRIVATE
    private byte[] readInputStreamProgressively (InputStream is, int totalSize, LoadRequest r) 
            throws IOException {

        fireLoadProgress(r, 3, 1); // compensate 33% of total time, which was consumed by establishing HTTP connection

        if (totalSize > 0) { // content length is known
            byte[] data = new byte[totalSize];
            int offset = 0;
            int readed;

            while (offset < totalSize && (readed = is.read(data, offset, totalSize - offset)) != -1) {
                offset += readed;
                fireLoadProgress(r, totalSize, (totalSize + offset) >> 1 );
            }

            if (offset != totalSize)
                throw new IOException("Unexpected readed size. current: " + offset + ", excepted: " + totalSize);
            
            return data;

        }
        else if (totalSize == 0) {
            return new byte[0];
        }
        else {
            // content length is unknown
            byte[] buf = new byte[1024];
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            long count = 0;
            int readed;
            while ((readed = is.read(buf)) != -1) {
                output.write(buf, 0, readed);
                count += readed;
            }

            fireLoadProgress(r, count, count);

            if (count > Integer.MAX_VALUE) 
                throw new IOException("content too large: " + (count / (1024 * 1024 )) + " M");

            return output.toByteArray();
        }
    }


    private void fireLoadResponse(final LoadRequest r, final Bitmap image) {
        if ( r.mListener != null) {
            try {
                r.mListener.onLoadResponse(r, image);
            }
            catch (Throwable t) {}
        }
    }


    private void fireLoadProgress(final LoadRequest r, final long totalContentSize, final long loadedContentSize) {
        if ( r.mListener != null) {
            try {
                r.mListener.onLoadProgress(r, totalContentSize, loadedContentSize);
            }
            catch (Throwable t) {}
        }
    }
    
    
    private void fireLoadFailure(final LoadRequest r, final Throwable e) {
        if ( r.mListener != null) {
            try {
                r.mListener.onLoadError(r, e);
            }
            catch (Throwable t) {}
        }
    }


    private int mMaxNumOfPixelsConstraint = DECODING_MAX_PIXELS_DEFAULT;
    private BitmapCache mCache;
    private BitmapCache mPersistence;
    private NetworkResourceLoader mNetworkResourceLoader = new NetworkResourceLoader(); 

    private Handler mHandler = new Handler();
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(1, 4, 10, TimeUnit.SECONDS, new LinkedBlockingStack<Runnable>());
    private Set<LoadRequest> mActiveRequests = new HashSet<LoadRequest>();
    private BitmapFilter mFilter;

    
    /*
     * The BitmapFactory.decodeStream() method fails to read a JPEG image (i.e.
     * returns null) if the skip() method of the used InputStream skip less bytes
     * than the required amount.
     * 
     * author: public domain
     */
    private static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        
        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int byt = read();
                    if (byt < 0) {
                        break;  // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}
