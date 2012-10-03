This is a simple library to provide a simple but powerful library that can address most of the requirement of asynchronously dowloading, caching and managing network resource (images).

Loading a resource is straightforward:

<code>
HttpImageManager.loadImage(new HttpImageManager.LoadRequest(uri, imageView));
</code>

Interface
-------
To load an image, one will have to instantiate an HttpImageManager object, do either way below:
* Set a target ImageView for displaying the loaded image
<code>
HttpImageManager.loadImage(new HttpImageManager.LoadRequest(uri, imageView));
</code>

* Set a listener for being notified of the result
<code>
HttpImageManager.loadImage(new HttpImageManager.LoadRequest(uri, new HttpImageManager.OnLoadResponseListener() {

            @Override
            public void onLoadResponse(LoadRequest r, Bitmap data) {
            }

            @Override
            public void onLoadError(LoadRequest r, Throwable e) {
            }

            
            @Override
            public void onLoadProgress(LoadRequest r, long totalContentSize,
                    long loadedContentSize) {
            }

        }));
</code>

Three Level Cache Hierarchy
-------
[memory cache] -> [persistent storage cache] -> [network laoder]

The memory cache is optional. To use too much memory cache may kill the system resource very fast. It is important to do so with a list view, though.

LIFO Threading Pool
-------
We use a LIFO queue for threading pool. This is to improve responsiveness of a listview, for example, when a user scroll down the list that loads a quite number of images sequentially.


Write-back Protection for List Adapters
-------
When used in list adapters that utilize techniques of reusing inflated view objects, the same ImageView object might be set as targets in a row of requests for difference resources. Knowing HttpImageManager works asynchronously, so the previously requested resource may come late and gets written back to the wrong ImageView object.
To prevent this write-back, we always bind the requested URL to the ImageView object. After the resource being downloaded, a comparison will be performed to check if the ImageView object is still representing the requested URL. This binding is realized by setting the ImageView's tag object. So, caller can not use the setTag() for other purpose during the process of HttpImageManager.

More features
--------

* Loading progress for large full-sized pictures
* Post manipulation of the loaded image by pluging in BitmapFilter
* Synchronous wrapper





