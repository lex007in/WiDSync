package com.lex007.widsync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class FileTransferAsyncTask extends AsyncTask<Void, Void, String> {

	private Context context;
    private ProgressDialog progressDialog;
    private Activity activity;
    private FolderDbAdapter mDbHelper;
    private boolean isGroupOwner;
    private boolean groupFormed;
    private InetAddress groupOwnerAddress;
    
    
    //Messages
    private static final int REQ_FOLDER_ID = 1;
    private static final int FOLDER_ID_EXIST = 2;
    private static final int FOLDER_ID_NO_EXIST = 3;
    private static final int REQ_NEXT_FILE = 4;
    private static final int NEXT_FILE = 5;
    private static final int NO_NEXT_FILE = 6;
    private static final int REQ_FILE_BIN = 7;
    
    //JSON fields
    private static final String JSON_MSG = "message";
    private static final String JSON_PATH = "path";
    private static final String JSON_FOLDER_ID = "folder_id";
    private static final String JSON_TIMESTAMP = "timestamp";

    /**
     * @param context
     * @param statusText
     */
    public FileTransferAsyncTask(Context context, ProgressDialog progressDialog, boolean isGroupOwner, boolean groupFormed, InetAddress groupOwnerAddress, Activity activity) {
        this.context = context;
        this.progressDialog = progressDialog;
        this.activity = activity;
        this.isGroupOwner = isGroupOwner;
        this.groupFormed = groupFormed;
        this.groupOwnerAddress = groupOwnerAddress;
    }

    @Override
    protected String doInBackground(Void... params) {
    	Log.d("Test", "doInBackground");
    	ServerSocket serverSocket = null;
    	Socket socket = null;
        try {
        	
        	if (groupFormed && isGroupOwner) {
        		Log.d("Test", "is owner");
        		serverSocket = new ServerSocket(9999);
        		socket = serverSocket.accept();
        		syncDown(socket);
        		syncUp(socket);
            } else if (groupFormed) {
            	Log.d("Test", "is not owner");
            	InetAddress host = groupOwnerAddress;
            	socket = new Socket(host, 9999);
            	syncUp(socket);
            	syncDown(socket);
            }
        	
            Log.d("Test", "Socket opened");
            Log.d("Test", "connection done");
            
            /*final File f = new File(Environment.getExternalStorageDirectory() + "/"
                    + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                    + ".jpg");

            File dirs = new File(f.getParent());
            if (!dirs.exists())
                dirs.mkdirs();
            f.createNewFile();

            Log.d("Test", "server: copying files " + f.toString());
            InputStream inputstream = socket.getInputStream();
            copyFile(inputstream, new FileOutputStream(f));*/
            
            if (socket != null) {
            	socket.close();
            }
            
            if (serverSocket != null) {
            	serverSocket.close();
            }
            
            //return f.getAbsolutePath();
            return "ok";
        } catch (IOException e) {
            Log.e("Test", e.getMessage());
            return null;
        }
    }

    private void syncDown(Socket socket) {
    	try {
	    	mDbHelper = ((MainActivity) activity).getmDbHelper();
	    	Cursor cur = mDbHelper.fetchDstFolders();
	    	cur.moveToFirst();
	    	InputStream in = socket.getInputStream();
	    	BufferedReader inBuf = new BufferedReader(new InputStreamReader(in));
	    	PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	    	JSONObject JSIn;
	    	JSONObject JSOut;
	    	String rFolderPath;
	    	File f;
	    	
	    	while (cur.moveToNext()){
	    		cur.getString(cur.getColumnIndex(FolderDbAdapter.KEY_FOLDER_ID));
	    		Log.e("Test", cur.getString(cur.getColumnIndex(FolderDbAdapter.KEY_FOLDER_ID)));
	    		progressDialog.setTitle(cur.getString(cur.getColumnIndex(FolderDbAdapter.KEY_FOLDER_ID)));
	    		
	    		JSOut = new JSONObject();
	    		JSOut.put(JSON_MSG, REQ_FOLDER_ID);
	    		JSOut.put(JSON_FOLDER_ID, cur.getString(cur.getColumnIndex(FolderDbAdapter.KEY_FOLDER_ID)));
	    		out.println(JSOut.toString());
	    		out.flush();
	    		JSIn = new JSONObject(inBuf.readLine());
	    		Log.e("Test", JSIn.toString());
	    		if (JSIn.getInt(JSON_MSG) == FOLDER_ID_NO_EXIST) {
	    			continue;
	    		}
	    		rFolderPath = cur.getString(cur.getColumnIndex(FolderDbAdapter.KEY_PATH)) ;
	    		JSOut = new JSONObject();
	    		JSOut.put(JSON_MSG, REQ_NEXT_FILE);
	    		out.println(JSOut.toString());
	    		out.flush();
	    		JSIn = new JSONObject(inBuf.readLine());
	    		while (JSIn.getInt(JSON_MSG) != NO_NEXT_FILE) {
	    			Log.e("Test", rFolderPath.concat(JSIn.getString(JSON_PATH)));
	    			f = new File(rFolderPath.concat(JSIn.getString(JSON_PATH)));
	    			if ((f != null) && (f.lastModified() < JSIn.getLong(JSON_TIMESTAMP))) {
	    				JSOut = new JSONObject();
			    		JSOut.put(JSON_MSG, REQ_FILE_BIN);
			    		out.println(JSOut.toString());
			    		out.flush();
			    		OutputStream outSFile = null;
						outSFile = new FileOutputStream(f, false);
						copyFile(in, outSFile);
	    			}
	    			JSOut = new JSONObject();
		    		JSOut.put(JSON_MSG, REQ_NEXT_FILE);
		    		out.println(JSOut.toString());
		    		out.flush();
	    		}	    		
	    	}
	    	
	    	in.close();
	    	out.close();
    	} catch (IOException e) {
    		Log.e("Test", e.getMessage());
    	} catch (JSONException e) {
    		Log.e("Test", e.getMessage());
		}
	}

	private void syncUp(Socket socket) {
		try {
			mDbHelper = ((MainActivity) activity).getmDbHelper();
			InputStream in = socket.getInputStream();
			BufferedReader inBuf = new BufferedReader(new InputStreamReader(in));
			OutputStream sOut = socket.getOutputStream();
	    	PrintWriter out = new PrintWriter(sOut, true);
	    	String inputLine;
	    	JSONObject JSIn;
	    	JSONObject JSOut;
	    	String filePath;
	    	File startDir = null;
	    	Iterator<File> filesIterator = null;
	    	File f = null;
	    	
	    	
			while ((inputLine = inBuf.readLine()) != null) {  
				JSIn = new JSONObject(inputLine);
				switch (JSIn.getInt(JSON_MSG)) {
				case REQ_FOLDER_ID: 
					JSOut = new JSONObject();
					Log.e("Test", JSIn.toString());
					if ((filePath = mDbHelper.getSrcFolderById(JSIn.getString(JSON_FOLDER_ID))) != null) {
						JSOut.put(JSON_MSG, FOLDER_ID_EXIST);
						startDir = new File(filePath);
						filesIterator = getFileList(startDir).iterator();
					} else {
						JSOut.put(JSON_MSG, FOLDER_ID_NO_EXIST);
					}
					out.println(JSOut.toString());
					out.flush();
					break;
				case REQ_NEXT_FILE:
					if (filesIterator.hasNext()) {
						f = filesIterator.next();
						JSOut = new JSONObject();
						JSOut.put(JSON_MSG, NEXT_FILE);
						JSOut.put(JSON_PATH, new File(startDir.toString()).toURI().relativize(new File(f.toString()).toURI()).getPath());
						JSOut.put(JSON_TIMESTAMP, f.lastModified());
						out.println(JSOut.toString());
						out.flush();
					} else {
						JSOut = new JSONObject();
						JSOut.put(JSON_MSG, NO_NEXT_FILE);
						out.println(JSOut.toString());
						out.flush();
					}
					break;
				case REQ_FILE_BIN:
					InputStream inSFile = null;
					inSFile = new FileInputStream(f);
					copyFile(inSFile, sOut);
					inSFile.close();
					break;
				}
	    	}
	    	
	    	in.close();
	    	out.close();
		} catch (IOException e) {
    		Log.e("Test", e.getMessage());
    	} catch (JSONException e) {
    		Log.e("Test", e.getMessage());
		}
	}

	private List<File> getFileList(File startDir) {
		List<File> listFile = new ArrayList<File>();
		fill(listFile, startDir);
		return listFile;
	}

	private void fill(List<File> listFile, File startDir) {
		File[] files = startDir.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				fill(listFile, f);
			} else {
				listFile.add(f);
			}
		}	
	}

	/*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
        	progressDialog.dismiss();
        }
    }
    
    public static boolean copyFile(InputStream inputStream, OutputStream out) {
            byte buf[] = new byte[1024];
            int len;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);

                }
                //out.close();
                //inputStream.close();
            } catch (IOException e) {
                Log.d("Test", e.toString());
                return false;
            }
            return true;
        }

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	@Override
	protected void onPreExecute() {
		Log.d("Test", "onPreExecute");
		super.onPreExecute();
	}
}
