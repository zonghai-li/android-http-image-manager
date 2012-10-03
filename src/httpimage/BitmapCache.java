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

import android.graphics.Bitmap;


/**
 * BitmapCache  
 * 
 * @author zonghai@gmail.com
 */
public interface BitmapCache {

    /**
     * Test if a specified bitmap exists
     * @param key
     * @return
     */
    public boolean exists(String key);
    
    
    /**
     * Invalidate a bitmap, release any resource associated.
     * @param key
     */
    public void invalidate(String key);
    
    
    /**
     * Retrieve the bitmap, return null means cache miss
     * @param key
     */
    public Bitmap loadData(String key);
    
    
    /**
     * Store bitmap
     * @param key
     * @param data
     */
    public void storeData(String key, Object data);
    
    
    /**
     * Clear this bitmap cache, reclaim all resources assigned.
     */
    public void clear();
}
