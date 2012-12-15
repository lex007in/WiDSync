package com.lex007.widsync;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class FolderListFragment extends ListFragment {

	private FolderDbAdapter mDbHelper;
	private static final int DELETE_ID = 0;
	private SimpleCursorAdapter folders;
	
	@Override
	  public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    mDbHelper = ((MainActivity)getActivity()).getmDbHelper();
	    fillData();
	    registerForContextMenu(getListView());
	    Log.d("Test", "registerForContextMenu");
	  }


	public void fillData() {
		String[] from = new String[]{FolderDbAdapter.KEY_FOLDER_ID, FolderDbAdapter.KEY_PATH, FolderDbAdapter.KEY_TYPE_SYNC_TXT};
	    int[] to = new int[]{R.id.folderId, R.id.folderPath, R.id.typeFoldW};
	    folders =
	    		new SimpleCursorAdapter(getActivity(), R.layout.folder_row, mDbHelper.fetchAllFolders(), from, to);
	    setListAdapter(folders);
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		Log.d("Test", "Menu created");
		menu.add(Menu.NONE, DELETE_ID, Menu.NONE, R.string.delete_item);
		super.onCreateContextMenu(menu, v, menuInfo);
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d("Test", String.valueOf(item.getItemId()));
		switch(item.getItemId()) {
        case DELETE_ID:
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            mDbHelper.deleteFolder(info.id);
            fillData();
            return true;
		}
		return super.onContextItemSelected(item);
	}
}
