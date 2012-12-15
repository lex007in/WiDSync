package com.lex007.widsync;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.lex007.widsync.DeviceListFragment.DeviceActionListener;
import com.lex007.widsync.EditFragment.onEndEditListener;

public class MainActivity extends Activity implements ChannelListener, onEndEditListener, DeviceActionListener {

	private FolderDbAdapter mDbHelper;
	private FragmentTransaction fTrans;
	private FolderListFragment fLisFrag;
	private EditFragment fEditFrag;
	private DeviceListFragment fDevList;


	private Boolean mDualPane;
	private final IntentFilter intentFilter = new IntentFilter();
	private WifiP2pManager mManager;
	private Channel mChannel;
	private boolean isWifiP2pEnabled = false;
	private boolean retryChannel = false;
	private com.lex007.widsync.WiFiDirectBroadcastReceiver receiver;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Test", "before setContentView");
        setContentView(R.layout.activity_main);
        mDbHelper = new FolderDbAdapter(this);
        mDbHelper.open();

    	fDevList = new DeviceListFragment();
    	
        if (findViewById(R.id.fragment_container) != null) {
        	mDualPane = false;
        	if (savedInstanceState != null) {
                return;
            }
        	
        	fLisFrag = new FolderListFragment();
        	fLisFrag.setArguments(getIntent().getExtras());
        	
        	getFragmentManager().beginTransaction().add(R.id.fragment_container, fLisFrag).commit();
        	
        } else {
        	mDualPane = true;
        	fLisFrag = (FolderListFragment) getFragmentManager().findFragmentById(R.id.main_list_frag);
        }
        
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
        case R.id.act_add_folder:
            addFolder();
            return true;
        case R.id.start_srv:
        	if (!isWifiP2pEnabled) {
                Toast.makeText(MainActivity.this, R.string.p2p_off_warning,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        	
        	fTrans = getFragmentManager().beginTransaction();
    		if (mDualPane) {
    			fTrans.replace(R.id.second_fragment, fDevList);
    		}else{
    			fTrans.replace(R.id.fragment_container, fDevList);
    		}
    		
    		if (!fDevList.isVisible())
    			fTrans.addToBackStack(null);
    		
    		fTrans.commit();
    		
    		fDevList.onInitiateDiscovery(this);
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "Discovery Initiated",
                            Toast.LENGTH_SHORT).show();
                }

                public void onFailure(int reasonCode) {
                    Toast.makeText(MainActivity.this, "Discovery Failed : " + reasonCode,
                            Toast.LENGTH_SHORT).show();
                }
            });
        	return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
    
	private void addFolder() {
		fTrans = getFragmentManager().beginTransaction();
		fEditFrag = new EditFragment();
		if (mDualPane) {
			fTrans.replace(R.id.second_fragment, fEditFrag);
		}else{
			fTrans.replace(R.id.fragment_container, fEditFrag);
		}
		if (!fEditFrag.isVisible())
			fTrans.addToBackStack(null);
		fTrans.commit();
    }


	public void onChooseDirectory(String dir) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(dir)
		       .setTitle("Dir:");
		builder.create().show();
	}

	/**
	 * @return the mDbHelper
	 */
	public FolderDbAdapter getmDbHelper() {
		return mDbHelper;
	}

	public void endEdit() {
		if (!mDualPane) {
			fTrans = getFragmentManager().beginTransaction();
			fTrans.replace(R.id.fragment_container, fLisFrag);
			fTrans.commit();
		}else{
			fLisFrag.fillData();
		}
	}
	
	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
	
	/**
	 * @return the fDevList
	 */
	public DeviceListFragment getfDevList() {
		return fDevList;
	}
	
    public void resetData() {
        if (fDevList != null && fDevList.isVisible()) {
        	fDevList.clearPeers();
        }
    }
    
    public void connect(WifiP2pConfig config) {
        mManager.connect(mChannel, config, new ActionListener() {

            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void disconnect() {
        mManager.removeGroup(mChannel, new ActionListener() {

            public void onFailure(int reasonCode) {
                Log.d("Test", "Disconnect failed. Reason :" + reasonCode);

            }

            public void onSuccess() {
                
            }

        });
    }

    public void onChannelDisconnected() {
        // we will try once more
        if (mManager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            mManager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (mManager != null) {
            final DeviceListFragment fragment = fDevList;
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

            	mManager.cancelConnect(mChannel, new ActionListener() {

                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    public void onFailure(int reasonCode) {
                        Toast.makeText(MainActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
