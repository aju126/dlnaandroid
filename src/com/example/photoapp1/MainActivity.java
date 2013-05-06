package com.example.photoapp1;



import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.support.avtransport.impl.AVTransportService;







import android.R.string;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity implements OnScrollListener {
	
	private static final int CONNECT_DEVICE = 199;
	
	private static final int SELECT_PICTURE = 1;

    private String selectedImagePath;
    
    GridView mGrid;
    
    public AppsAdapter mAdapter;
    
    public static RemoteDevice device;
    
    public static AndroidUpnpService upnpService;
    
    HashMap< String, String> imglinks = new HashMap<String, String>();
    
    private Map<Integer,SoftReference<ImageView>> mThumbnails = new HashMap<Integer,SoftReference<ImageView>>();
    private Map<Integer,SoftReference<Bitmap>> mThumbnailImages = new HashMap<Integer,SoftReference<Bitmap>>();
    
    static int indx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photogal);
        
        Button opengal = (Button)findViewById(R.id.galopen);
        opengal.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);
			}
		});
        
        Button seldev = (Button)findViewById(R.id.seldev);
        seldev.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent( MainActivity.this , BrowseActivity.class );
                
                startActivityForResult(intent,CONNECT_DEVICE);
			}
		});
        
        mGrid = (GridView) findViewById(R.id.myGrid);
        mGrid.setOnScrollListener(this);
        
        mAdapter = new AppsAdapter();
		 mGrid.setAdapter(mAdapter);
		 
		 mGrid.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				String path = imglinks.get(""+arg2);
				makeconnection(path);
			}
		});
    }
    
    private void makeconnection( String path )
    {
    	LocalService<AVTransportService> service =
    	        new AnnotationLocalServiceBinder().read(AVTransportService.class);

    	service.setManager(
    	        new DefaultServiceManager<AVTransportService>(service, null) {
    	            @Override
    	            protected AVTransportService createServiceInstance() throws Exception {
    	                return new AVTransportService(
    	                        MyRendererStateMachine.class,   // All states
    	                        MyRendererNoMediaPresent.class  // Initial state
    	                );
    	            }
    	        }
    	);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);
                indx++;
                imglinks.put(""+indx, selectedImagePath);
                refreshGrid();
            }
            if(requestCode == CONNECT_DEVICE)
            {
            	
            	String devname = data.getStringExtra("result");
            	((Button)findViewById(R.id.seldev)).setText(devname);
            	
            }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        
        return cursor.getString(column_index);
    }
    
    private void refreshGrid()
    {
    	mAdapter.notifyDataSetChanged();
    }
    
    public class AppsAdapter extends BaseAdapter {
        public AppsAdapter() {
        	map = new HashMap();
        }
        
        public Map<Integer,SoftReference<Bitmap>> map;
        public View getView(final int position, View convertView, ViewGroup parent) {
            ImageView i;

            if (convertView == null) {
                i = new ImageView(MainActivity.this);
                i.setScaleType(ImageView.ScaleType.FIT_CENTER);
                i.setLayoutParams(new GridView.LayoutParams(80, 80));
            } else {
                i = (ImageView) convertView;
            }
            
            if( mThumbnailImages.containsKey(position) 
        			&& mThumbnailImages.get(position).get()!=null) {
        		i.setImageBitmap(mThumbnailImages.get(position).get());
        	}
        	else  {
        		i.setImageBitmap(null);
        		loadThumbnail(i,position);
        	}
            
            i.setOnClickListener(new OnClickListener() {
				
				//@Override
				public void onClick(View v) {
					Toast.makeText(MainActivity.this, "Opening Image...", Toast.LENGTH_LONG).show();

					// TODO Auto-generated method stub
					SharedPreferences indexPrefs = getSharedPreferences("currentIndex",
							MODE_PRIVATE);
					
					SharedPreferences.Editor indexEditor = indexPrefs.edit();
					indexEditor.putInt("currentIndex", position);
					indexEditor.commit();
					//final Intent intent = new Intent(MainActivity.this, ImageViewFlipper.class);
		            //startActivity(intent);
		           
				}
			});
            
            
            return i;
        }


        public final int getCount() {
            return imglinks.size();
        }

        public final Object getItem(int position) {
            return imglinks.get(""+position);
        }

        public final long getItemId(int position) {
            return position;
        }
        
        
    }
    
    
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
	
	private void loadThumbnail( ImageView iv, int position ){
    	mThumbnails.put(position,new SoftReference<ImageView>(iv));
    	try{new LoadThumbnailTask().execute(position);}catch(Exception e){}
    }
    public void onThumbnailLoaded( int position, Bitmap bm, LoadThumbnailTask t ){
    	Bitmap tn = bm;
    	if( mThumbnails.get(position).get() != null && tn!=null)
    		mThumbnails.get(position).get().setImageBitmap(tn);
    	
    	t.cancel(true);
    }
    
    public class LoadThumbnailTask extends AsyncTask<Integer, Void, Bitmap>{
    	private int position;
		@Override
		protected Bitmap doInBackground(Integer... params) {
        	try{
				position = params[0];
				Bitmap bitmapOrg = BitmapFactory.decodeFile(imglinks.get(""+(position +1)));
	        
	        	int width = bitmapOrg.getWidth();
	        	int height = bitmapOrg.getHeight();
	     
	        	//new width / height
	        	int newWidth = 80;
	        	int newHeight = 80;
	
	        	// calculate the scale
	        	float scaleWidth = (float) newWidth / width;
	        	float scaleHeight = (float) newHeight/ (height * scaleWidth) ;
	        	// create a matrix for the manipulation
	        	Matrix matrix = new Matrix();
	
	        	// resize the bit map
	        	matrix.postScale(scaleWidth, scaleWidth);
	        	matrix.postScale(scaleHeight, scaleHeight);
	
	        	// recreate the new Bitmap and set it back
	        	Bitmap bm = Bitmap.createBitmap(bitmapOrg, 0, 0,width, height, matrix, true);
	            
	            mThumbnailImages.put(position, new SoftReference<Bitmap>(bm));
	            System.gc();
	            return bm;
        	}catch(Exception e){
        		
        	}
            
            
			return null;
		}
		protected void onPostExecute(Bitmap bm) {
	         
	         onThumbnailLoaded(position, bm, this);
	     }
    }

    
}

	
