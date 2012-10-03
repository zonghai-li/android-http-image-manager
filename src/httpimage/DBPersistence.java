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


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;


/**
 * DB implementation of persistent storage.
 * 
 * @author zonghai@gmail.com
 */
public class DBPersistence implements BitmapCache{
    
    private static final String TAG = "DBPersistence";
    private static final boolean DEBUG = false;
    
    
    public DBPersistence(Context context) {
        this.mContext = context;
    }
    
    
    public boolean exists(String key) {
        //TODO
        return false;
    }


    public Bitmap loadData(String key) {
        Bitmap bitmap = null;
        
        Uri image = Uri.withAppendedPath(DBImageTable.CONTENT_URI, key);
        if (DEBUG) Log.d(TAG, "loaddata " + image.toString());
        String[] returnCollums = new String[] {
            DBImageTable.DATA,
        };
        
        Cursor c = null;
        try {
            ContentResolver cr = mContext.getContentResolver();
            c = cr.query(image, returnCollums, null, null, null);
            if (DEBUG) Log.d(TAG, "count=" + c.getCount());
            if(c.getCount() < 1) {
                return null;
            }
            if(c .getCount() > 1) {
                throw new RuntimeException("shouldn't reach here, make sure the NAME collumn is unique: " + key);
            }
            c.moveToFirst();
            byte[] binary = c.getBlob(c.getColumnIndex(DBImageTable.DATA));
            
            if( binary != null ) {
                bitmap = BitmapUtil.decodeByteArray(binary, HttpImageManager.DECODING_MAX_PIXELS_DEFAULT);
                if(bitmap == null) {
                     // something wrong with the persistent data, can't be decoded to bitmap.
                    throw new RuntimeException("data from db can't be decoded to bitmap");
                }
            }
            return bitmap;
        }
        finally{
            if(c != null){
                c.close();
            }
        }
    }

    
    public void storeData(String key, Object data) {
        
        byte[] ba = (byte[])data;
        if (ba != null) {
        
            ContentValues values = new ContentValues();
            values.put(DBImageTable.NAME, key);
            values.put(DBImageTable.DATA, ba);
            values.put(DBImageTable.SIZE, ba.length);
            values.put(DBImageTable.NUSE, 1);
            values.put(DBImageTable.TIMESTAMP, System.currentTimeMillis());
            mContext.getContentResolver().insert(DBImageTable.CONTENT_URI, values);
        }
    }

    
    @Override
    public void clear() {
        //TODO
    }


    @Override
    public void invalidate(String key) {
        //TODO:
    }
    

    private Context mContext;

}
