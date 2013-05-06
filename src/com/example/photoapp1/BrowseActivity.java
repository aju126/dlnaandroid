/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.photoapp1;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import java.util.Comparator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.BooleanDatatype;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.support.connectionmanager.ConnectionManagerService;
import org.teleal.cling.support.contentdirectory.ui.ContentBrowseActionCallback;
import org.teleal.cling.support.model.Protocol;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.ProtocolInfos;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.transport.SwitchableRouter;



import com.wireme.player.GPlayer;

/**
 * @author Christian Bauer
 */
public class BrowseActivity extends ListActivity  {

    // private static final Logger log = Logger.getLogger(BrowseActivity.class.getName());

    private ArrayAdapter<DeviceDisplay> listAdapter;
    
    private ArrayAdapter<DeviceItem> deviceListAdapter;
	private ArrayAdapter<ContentItem> contentListAdapter;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private AndroidUpnpService upnpService;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            // Refresh the list with all known devices
            listAdapter.clear();
            for (Device device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Getting ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Search asynchronously for all devices
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(this, "in browser activity", 2);
        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1){
        	/*@Override
        	public Object getItem(int position) {
        		// TODO Auto-generated method stub
        		DeviceDisplay d = (DeviceDisplay)getItem(position);
        		
        		Intent returnIntent = new Intent();
        		 returnIntent.putExtra("result",d.getDevice().getDisplayString());
        		 setResult(RESULT_OK,returnIntent);     
        		 finish();
        		return d;
        	}*/
        };
        setListAdapter(listAdapter);
        
        getListView().setOnItemClickListener(  new OnItemClickListener() {
        	
        	

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
DeviceDisplay d = (DeviceDisplay)getListAdapter().getItem(arg2);
        		upnpService.getRegistry().addDevice((RemoteDevice)d.getDevice());
        		Service service = d.getDevice().findService(new UDAServiceType(
    					"ConnectionManager"));
        		/*
        		Action getStatusAction = service.getAction("GetStatus");
        		 ActionInvocation getStatusInvocation = new ActionInvocation(getStatusAction);
    			upnpService.getControlPoint().execute( new ActionCallback(getStatusInvocation) {
					
					@Override
					public void success(ActionInvocation arg0) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
						// TODO Auto-generated method stub
						
					}
				});
				*/
        		makeHttpConnection();
        		SubscriptionCallback callback = new SubscriptionCallback(service, 600) {

        		    @Override
        		    public void established(GENASubscription sub) {
        		        System.out.println("Established: " + sub.getSubscriptionId());
        		    }

        		    @Override
        		    protected void failed(GENASubscription subscription,
        		                          UpnpResponse responseStatus,
        		                          Exception exception,
        		                          String defaultMsg) {
        		        System.err.println(defaultMsg);
        		    }

        		   /* @Override
        		    public void ended(GENASubscription sub,
        		                      CancelReason reason,
        		                      UpnpResponse response) {
        		        assert reason == null;
        		    }*/

        		    public void eventReceived(GENASubscription sub) {

        		        System.out.println("Event: " + sub.getCurrentSequence().getValue());

        		        Map<String, StateVariableValue> values = sub.getCurrentValues();
        		        StateVariableValue status = values.get("Status");

        		       // assertEquals(status.getDatatype().getClass(), BooleanDatatype.class);
        		        //assertEquals(status.getDatatype().getBuiltin(), Datatype.Builtin.BOOLEAN);

        		       // System.out.println("Status is: " + status.toString());

        		    }

        		    public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
        		        System.out.println("Missed events: " + numberOfMissedEvents);
        		    }

					@Override
					protected void ended(GENASubscription arg0,
							CancelReason arg1, UpnpResponse arg2) {
						// TODO Auto-generated method stub
						
					}

        		};

        		upnpService.getControlPoint().execute(callback);
        		Intent returnIntent = new Intent();
        		 returnIntent.putExtra("result",d.getDevice().getDisplayString());
        		 setResult(RESULT_OK,returnIntent);   
        		 MainActivity.device = (RemoteDevice) d.getDevice();
        		 MainActivity.upnpService = upnpService;
        		 finish();
			}
        	
		});

        getApplicationContext().bindService(
                new Intent(this, BrowserUpnpService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
        searchNetwork();
    }
    
    private void makeHttpConnection()
    {
    	LocalService<ConnectionManagerService> service =
    	        new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);

    	service.setManager(
    	        new DefaultServiceManager<ConnectionManagerService>(
    	                service,
    	                ConnectionManagerService.class
    	        )
    	);
    	
    	final ProtocolInfos sourceProtocols =
    	        new ProtocolInfos(
    	                new ProtocolInfo(
    	                        Protocol.HTTP_GET,
    	                        ProtocolInfo.WILDCARD,
    	                        "audio/mpeg",
    	                        "DLNA.ORG_PN=MP3;DLNA.ORG_OP=01"
    	                ),
    	                new ProtocolInfo(
    	                        Protocol.HTTP_GET,
    	                        ProtocolInfo.WILDCARD,
    	                        "video/mpeg",
    	                        "DLNA.ORG_PN=MPEG1;DLNA.ORG_OP=01;DLNA.ORG_CI=0"
    	                )
    	        );
    	
    	service.setManager(
    		    new DefaultServiceManager<ConnectionManagerService>(service, null) {
    		        @Override
    		        protected ConnectionManagerService createServiceInstance() throws Exception {
    		            return new ConnectionManagerService(sourceProtocols, null);
    		        }
    		    }
    		);
    }

    public void startSearching()
    {
    	getApplicationContext().bindService(
                new Intent(this, BrowserUpnpService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
        searchNetwork();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        getApplicationContext().unbindService(serviceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.add(0, 0, 0, R.string.search_lan).setIcon(android.R.drawable.ic_menu_search);
        //menu.add(0, 1, 0, R.string.switch_router).setIcon(android.R.drawable.ic_menu_revert);
        //menu.add(0, 2, 0, R.string.toggle_debug_logging).setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                searchNetwork();
                break;
            case 1:
                if (upnpService != null) {
                    SwitchableRouter router = (SwitchableRouter) upnpService.get().getRouter();
                    if (router.isEnabled()) {
                        //Toast.makeText(this, R.string.disabling_router, Toast.LENGTH_SHORT).show();
                        router.disable();
                    } else {
                        //Toast.makeText(this, R.string.enabling_router, Toast.LENGTH_SHORT).show();
                        router.enable();
                    }
                }
                break;
            case 2:
                Logger logger = Logger.getLogger("org.teleal.cling");
                if (logger.getLevel().equals(Level.FINEST)) {
                    //Toast.makeText(this, R.string.disabling_debug_logging, Toast.LENGTH_SHORT).show();
                    logger.setLevel(Level.INFO);
                } else {
                    //Toast.makeText(this, R.string.enabling_debug_logging, Toast.LENGTH_SHORT).show();
                    logger.setLevel(Level.FINEST);
                }
                break;
        }
        return false;
    }

    protected void searchNetwork() {
        if (upnpService == null) return;
        //$BrowseRegistryListener; Toast.makeText(this, R.string.searching_lan, Toast.LENGTH_SHORT).show();
        upnpService.getRegistry().removeAllRemoteDevices();
        upnpService.getControlPoint().search();
    }
    
    OnItemClickListener deviceItemClickListener = new OnItemClickListener() {

		protected Container createRootContainer(Service service) {
			Container rootContainer = new Container();
			rootContainer.setId("0");
			rootContainer.setTitle("Content Directory on "
					+ service.getDevice().getDisplayString());
			return rootContainer;
		}

		//@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			// TODO Auto-generated method stub
			Device device = ((DeviceDisplay)listAdapter.getItem(position)).getDevice();
			Service service = device.findService(new UDAServiceType(
					"ContentDirectory"));
			upnpService.getControlPoint().execute(
					new ContentBrowseActionCallback(BrowseActivity.this, service,
							createRootContainer(service)));
		}
	};
/*
	OnItemClickListener contentItemClickListener = new OnItemClickListener() {

		//@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			// TODO Auto-generated method stub
			ContentItem content = listAdapter.getItem(position);
			if (content.isContainer()) {
				upnpService.getControlPoint().execute(
						new ContentBrowseActionCallback(BrowseActivity.this,
								content.getService(), content.getContainer(),
								listAdapter));
			} else {
				Intent intent = new Intent();
				intent.setClass(BrowseActivity.this, GPlayer.class);
				intent.putExtra("playURI", content.getItem().getFirstResource()
						.getValue());
				startActivity(intent);
			}
		}
	};
*/
    protected class BrowseRegistryListener extends DefaultRegistryListener {

        /* Discovery performance optimization for very slow Android devices! */

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
            showToast(
                    "Discovery failed of '" + device.getDisplayString() + "': " +
                            (ex != null ? ex.toString() : "Couldn't retrieve device/service descriptors"),
                    true
            );
            deviceRemoved(device);
        }
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            deviceRemoved(device);
        }

        public void deviceAdded(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);

                    int position = listAdapter.getPosition(d);
                    if (position >= 0) {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(d);
                        listAdapter.insert(d, position);
                    } else {
                        listAdapter.add(d);
                        //Toast.makeText(this, d.toString(), 2);
                    }

                    // Sort it?
                    // listAdapter.sort(DISPLAY_COMPARATOR);
                    // listAdapter.notifyDataSetChanged();
                }
            });
        }

        public void deviceRemoved(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    listAdapter.remove(new DeviceDisplay(device));
                }
            });
        }
    }

    protected void showToast(final String msg, final boolean longLength) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        BrowseActivity.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

     class DeviceDisplay {

        Device device;

        public DeviceDisplay(Device device) {
            this.device = device;
        }

        public Device getDevice() {
            return device;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeviceDisplay that = (DeviceDisplay) o;
            return device.equals(that.device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }

        @Override
        public String toString() {
            String name =
                    device.getDetails() != null && device.getDetails().getFriendlyName() != null
                            ? device.getDetails().getFriendlyName()
                            : device.getDisplayString();
            // Display a little star while the device is being loaded (see performance optimization earlier)
            return device.isFullyHydrated() ? name : name + " *";
        }
    }

    static final Comparator<DeviceDisplay> DISPLAY_COMPARATOR =
            new Comparator<DeviceDisplay>() {
                public int compare(DeviceDisplay a, DeviceDisplay b) {
                    return a.toString().compareTo(b.toString());
                }
            };
}
