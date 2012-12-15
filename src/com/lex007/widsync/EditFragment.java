package com.lex007.widsync;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;

import com.lex007.widsync.DialogChooseDirectory.FolderResult;


public class EditFragment extends Fragment implements FolderResult, OnClickListener {

	private View mEditView;
	private Fragment mThisFrag;
	private Activity mAct;
	private FolderDbAdapter mDbHelper;
	
	@Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {
		mThisFrag = this;
		mAct = getActivity();
		mDbHelper = ((MainActivity) mAct).getmDbHelper();
		mEditView = inflater.inflate(R.layout.fragment_edit, null);
		mEditView.findViewById(R.id.choose_dialog_btn).setOnClickListener(this);
		mEditView.findViewById(R.id.editOk).setOnClickListener(this);
	    return mEditView;
	  }

	public void onChooseDirectory(String dir) {
		((EditText) mEditView.findViewById(R.id.folderPathInput)).setText(dir);	
	}


	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.choose_dialog_btn:
			new DialogChooseDirectory(mAct, (FolderResult) mThisFrag, Environment.getExternalStorageDirectory().getPath());
			break;
		case R.id.editOk:
			File file = new File(((EditText) mEditView.findViewById(R.id.folderPathInput)).getText().toString());
			mDbHelper.createFolder(((EditText) mEditView.findViewById(R.id.folderIdInput)).getText().toString(),
					((EditText) mEditView.findViewById(R.id.folderPathInput)).getText().toString(),
					new SimpleDateFormat("yyyy-MM-dd").format(new Date(file.lastModified())),
					((RadioButton) mEditView.findViewById(R.id.radioButtonDst)).isChecked()?FolderDbAdapter.TYPE_DST:FolderDbAdapter.TYPE_SRC);
			((onEndEditListener) mAct).endEdit();
			break;
		}
		
	}
	public interface onEndEditListener {
		    public void endEdit();
	}
}
