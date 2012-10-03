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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * DB Table definition for storing cached data.
 * 
 * @author zonghai@gmail.com
 */
public class DBImageTable implements BaseColumns {
    
    
    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI
            = Uri.parse("httpimage.provider.DataProvider/thumbnail");
    
    /**
     * The name of the image
     * <P>Type: TEXT</P>
     */
    public static final String NAME = "Name";

    /**
     * The image data itself
     * <P>Type: IMAGE DATA</P>
     */
    public static final String DATA = "Data";

    
    /**
     * size of the image after being compressed.
     * <P>Type: INTEGER (long)</P>
     */
    public static final String SIZE = "Size";
    
    
    /**
     * The timestamp when the image was last modified
     * <P>Type: DATE </P>
     */
    public static final String TIMESTAMP = "Timestamp";
    
    /**
     * number of reference of the image
     * <P>Type: INTEGER (long)</P>
     */
    public static final String NUSE = "nUsed";
    
}
