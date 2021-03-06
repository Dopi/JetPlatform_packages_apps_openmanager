/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.nexes.manager;

import java.io.File;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
//import android.util.Log;

/**
 * This is the main activity. The activity that is presented to the user
 * as the application launches. This class is, and expected not to be, instantiated.
 * <br>
 * <p>
 * This class handles creating the buttons and
 * text views. This class relies on the class EventHandler to handle all button
 * press logic and to control the data displayed on its ListView. This class
 * also relies on the FileManager class to handle all file operations such as
 * copy/paste zip/unzip etc. However most interaction with the FileManager class
 * is done via the EventHandler class. Also the SettingsMangager class to load
 * and save user settings. 
 * <br>
 * <p>
 * The design objective with this class is to control only the look of the
 * GUI (option menu, context menu, ListView, buttons and so on) and rely on other
 * supporting classes to do the heavy lifting. 
 *
 * @author Joe Berria
 *
 */
public final class Main extends ListActivity {
	public static final String PREFS_NAME = "ManagerPrefsFile";	//user preference file name
	public static final String PREFS_HIDDEN = "hidden";
	public static final String PREFS_COLOR = "color";
	
	private static final int MENU_MKDIR =   0x00;			//option menu id
	private static final int MENU_SETTING = 0x01;			//option menu id
	private static final int MENU_SEARCH =  0x02;			//option menu id
	private static final int MENU_SPACE =   0x03;			//option menu id
	private static final int MENU_QUIT = 	0x04;			//option menu id
	private static final int MENU_SORT =	0x05;			//option menu id
	private static final int SEARCH_B = 	0x09;
	
	private static final int D_MENU_DELETE = 0x05;			//context menu id
	private static final int D_MENU_RENAME = 0x06;			//context menu id
	private static final int D_MENU_COPY =   0x07;			//context menu id
	private static final int D_MENU_PASTE =  0x08;			//context menu id
	private static final int D_MENU_ZIP = 	 0x0e;			//context menu id
	private static final int D_MENU_UNZIP =  0x0f;			//context menu id
	private static final int F_MENU_DELETE = 0x0a;			//context menu id
	private static final int F_MENU_RENAME = 0x0b;			//context menu id
	private static final int F_MENU_ATTACH = 0x0c;			//context menu id
	private static final int F_MENU_COPY =   0x0d;			//context menu id
	private static final int SETTING_REQ = 	 0x10;			//request code for intent

	private FileManager flmg;
	private EventHandler handler;
	private EventHandler.TableRow table;
	
	private SharedPreferences settings;
	private boolean holding_file = false;
	private boolean holding_zip = false;
	private boolean use_back_key = true;
	private String copied_target;
	private String zipped_target;
	private String selected_list_item;				//item from context menu
	private TextView  path_label, detail_label;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        /*read settings*/
        settings = getSharedPreferences(PREFS_NAME, 0);
        boolean hide = settings.getBoolean(PREFS_HIDDEN, false);
        int color = settings.getInt(PREFS_COLOR, -1);
        
        flmg = new FileManager();
        flmg.setShowHiddenFiles(hide);
        
        handler = new EventHandler(Main.this, flmg);
        handler.setTextColor(color);
        table = handler.new TableRow();
        
        /*sets the ListAdapter for our ListActivity and
         *gives our EventHandler class the same adapter
         */
        handler.setListAdapter(table);
        setListAdapter(table);
               
        /* register context menu for our list view */
        registerForContextMenu(getListView());
        
        path_label = (TextView)findViewById(R.id.path_label);
        detail_label = (TextView)findViewById(R.id.detail_label);
        path_label.setText("path: /sdcard");
        
        handler.setUpdateLabel(path_label, detail_label);
        
        /*buttons on the top row or the main activity*/
        ImageButton help_b = (ImageButton)findViewById(R.id.help_button);
        help_b.setOnClickListener(handler);
        
        ImageButton home_b = (ImageButton)findViewById(R.id.home_button);
        home_b.setOnClickListener(handler);
        
        ImageButton back_b = (ImageButton)findViewById(R.id.back_button);        
        back_b.setOnClickListener(handler);
        
        ImageButton info_b = (ImageButton)findViewById(R.id.info_button);
        info_b.setOnClickListener(handler);
        
        ImageButton manage_b = (ImageButton)findViewById(R.id.manage_button);
        manage_b.setOnClickListener(handler);
        
        ImageButton multi_b = (ImageButton)findViewById(R.id.multiselect_button);
        multi_b.setOnClickListener(handler);
        
        /*hidden multiselect buttons*/
        Button copy = (Button)findViewById(R.id.hidden_copy);
        copy.setOnClickListener(handler);
        
        Button attach = (Button)findViewById(R.id.hidden_attach);
        attach.setOnClickListener(handler);
        
        Button delete = (Button)findViewById(R.id.hidden_delete);
        delete.setOnClickListener(handler);
    }

	/**
	 *  To add more functionality and let the user interact with more
	 *  file types, this is the function to add the ability. 
	 *  
	 *  (note): this method can be done more efficiently 
	 */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
    	final String item = handler.getData(position);
    	boolean multiSelect = handler.isMultiSelected();
    	File file = new File(flmg.getCurrentDir() + "/" + item);
    	String item_ext = null;

    	try {
    		item_ext = item.substring(item.lastIndexOf("."), item.length());
    		
    	} catch(IndexOutOfBoundsException e) {	
    		item_ext = ""; 
    	}
    	
    	/*
    	 * If the user has multi-select on, we just need to record the file
    	 * not make an intent for it.
    	 */
    	if(multiSelect) {
    		table.addMultiPosition(position, file.getPath());
    		
    	} else {
	    	if (file.isDirectory()) {
				if(file.canRead()) {
		    		handler.updateDirectory(flmg.getNextDir(item, false));
		    		path_label.setText(flmg.getCurrentDir());
		    		
		    		/*set back button switch to true (this will be better implemented later)*/
		    		if(!use_back_key)
		    			use_back_key = true;
		    		
	    		} else {
	    			Toast.makeText(this, "Can't read folder due to permissions", 
	    							Toast.LENGTH_SHORT).show();
	    		}
	    	}
	    	
	    	/*music file selected--add more audio formats*/
	    	else if (item_ext.equalsIgnoreCase(".mp3") || item_ext.equalsIgnoreCase(".m4a")) {
	    			
	        	Intent music_int = new Intent(this, AudioPlayblack.class);
	        	music_int.putExtra("MUSIC PATH", flmg.getCurrentDir() +"/"+ item);
	        	startActivity(music_int);
	    	}
	    	
	    	/*photo file selected*/
	    	else if(item_ext.equalsIgnoreCase(".jpeg") || item_ext.equalsIgnoreCase(".jpg") ||
	    			item_ext.equalsIgnoreCase(".png")  || item_ext.equalsIgnoreCase(".gif") || 
	    			item_ext.equalsIgnoreCase(".tiff")) {
	 			    		
	    		if (file.exists()) {
		    		Intent picIntent = new Intent();
		    		picIntent.setAction(android.content.Intent.ACTION_VIEW);
		    		picIntent.setDataAndType(Uri.fromFile(file), "image/*");
		    		startActivity(picIntent);
	    		}
	    	}
	    	
	    	/*video file selected--add more video formats*/
	    	else if(item_ext.equalsIgnoreCase(".m4v") || item_ext.equalsIgnoreCase(".3gp") ||
	    			item_ext.equalsIgnoreCase(".wmv") || item_ext.equalsIgnoreCase(".mp4") || 
	    			item_ext.equalsIgnoreCase(".ogg")) {
	    		
	    		if (file.exists()) {
		    		Intent movieIntent = new Intent();
		    		movieIntent.setAction(android.content.Intent.ACTION_VIEW);
		    		movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
		    		startActivity(movieIntent);
	    		}
	    	}
	    	
	    	/*zip and gzip file selected (gzip will be implemented later)*/
	    	else if(item_ext.equalsIgnoreCase(".zip")) {
	    		
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		AlertDialog alert;
	    		zipped_target = flmg.getCurrentDir() + "/" + item;
	    		CharSequence[] option = {"Extract here", "Extract to..."};
	    		
	    		builder.setTitle("Extract");
	    		builder.setItems(option, new DialogInterface.OnClickListener() {
	
					public void onClick(DialogInterface dialog, int which) {
						switch(which) {
							case 0:
								String dir = flmg.getCurrentDir();
								handler.unZipFile(item, dir + "/");
								break;
								
							case 1:
								detail_label.setText("Holding " + item + " to extract");
								holding_zip = true;
								break;
						}
					}
	    		});
	    		
	    		alert = builder.create();
	    		alert.show();
	    	}
	    	
	    	/*pdf file selected*/
	    	else if(item_ext.equalsIgnoreCase(".pdf")) {
	    		
	    		if(file.exists()) {
		    		Intent pdfIntent = new Intent();
		    		pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
		    		pdfIntent.setDataAndType(Uri.fromFile(file), "application/pdf");
		    		startActivity(pdfIntent);
	    		}
	    	}
	    	
	    	/*Android application file*/
	    	else if(item_ext.equalsIgnoreCase(".apk")){
	    		
	    		if(file.exists()) {
	    			Intent apkIntent = new Intent();
	    			apkIntent.setAction(android.content.Intent.ACTION_VIEW);
	    			apkIntent.setDataAndType(Uri.fromFile(file), 
	    									 "application/vnd.android.package-archive");
	    			startActivity(apkIntent);
	    		}
	    	}
	    	
	    	/* HTML file */
	    	else if(item_ext.equalsIgnoreCase(".html")) {
	    		
	    		if(file.exists()) {
	    			Intent htmlIntent = new Intent();
	    			htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
	    			htmlIntent.setDataAndType(Uri.fromFile(file), "application/htmlviewer");
	    			try {
	    				startActivity(htmlIntent);
	    			} catch(ActivityNotFoundException e) {
	    				Toast.makeText(this, "Sorry, couldn't find a HTML view", 
	    									Toast.LENGTH_SHORT).show();
	    			}
	    		}
	    	}
	    	
	    	/* generic intent */
	    	else {
	    		if(file.exists()) {
		    		Intent generic = new Intent();
		    		generic.setAction(android.content.Intent.ACTION_VIEW);
		    		generic.setDataAndType(Uri.fromFile(file), "application/*");
		    		
		    		startActivity(generic);
	    		}
	    	}
    	}
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	SharedPreferences.Editor editor = settings.edit();
    	boolean check;
    	int color;
    	
    	/* resultCode must equal RESULT_CANCELED because the only way
    	 * out of that activity is pressing the back button on the phone
    	 * this publishes a canceled result code not an ok result code
    	 */
    	if(requestCode == SETTING_REQ && resultCode == RESULT_CANCELED) {
    		//save the information we get from settings activity
    		check = data.getBooleanExtra("HIDDEN", false);
    		color = data.getIntExtra("COLOR", -1);
    		
    		editor.putBoolean(PREFS_HIDDEN, check);
    		editor.putInt(PREFS_COLOR, color);
    		editor.commit();
    		
    		flmg.setShowHiddenFiles(check);
    		handler.setTextColor(color);
    		handler.updateDirectory(flmg.getNextDir(flmg.getCurrentDir(), true));
    	}
    }
    
    /* ================Menus, options menu and context menu start here=================*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_MKDIR, 0, "New Directory").setIcon(R.drawable.newfolder);
    	menu.add(0, MENU_SEARCH, 0, "Search").setIcon(R.drawable.search);
    	
    		/* free space will be implemented at a later time */
//    	menu.add(0, MENU_SPACE, 0, "Free space").setIcon(R.drawable.space);
    	menu.add(0, MENU_SETTING, 0, "Settings").setIcon(R.drawable.setting);
    	menu.add(0, MENU_SORT, 0, "Sort").setIcon(R.drawable.filter);
    	menu.add(0, MENU_QUIT, 0, "Quit").setIcon(R.drawable.logout);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case MENU_MKDIR:
    			showDialog(MENU_MKDIR);
    			return true;
    			
    		case MENU_SEARCH:
    			showDialog(MENU_SEARCH);
    			return true;
    			
    		case MENU_SPACE: /* not yet implemented */
    			return true;
    			
    		case MENU_SETTING:
    			Intent settings_int = new Intent(this, Settings.class);
    			settings_int.putExtra("HIDDEN", settings.getBoolean("hidden", false));
    			settings_int.putExtra("COLOR", settings.getInt(PREFS_COLOR, -1));
    			
    			startActivityForResult(settings_int, SETTING_REQ);
    			return true;
    			
    		case MENU_SORT:
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			CharSequence[] options = {"Alphabetical", "By type"};
    			
    			builder.setTitle("Sort by...");
    			builder.setItems(options, new DialogInterface.OnClickListener() {					
					@Override
					public void onClick(DialogInterface dialog, int index) {
						switch(index) {
						case 0:
							handler.sortAlphabetical();
							break;
						case 1:
							handler.sortByType();
							break;
						}
					}
				});
    			
    			builder.create().show();
    			return true;
    			
    		case MENU_QUIT:
    			finish();
    			return true;
    	}
    	return false;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
    	super.onCreateContextMenu(menu, v, info);
    	
    	boolean multi_data = handler.hasMultiSelectData();
    	AdapterContextMenuInfo _info = (AdapterContextMenuInfo)info;
    	selected_list_item = handler.getData(_info.position);

    	if(flmg.isDirectory(selected_list_item)) {
    		menu.setHeaderTitle("Folder operations");
        	menu.add(0, D_MENU_DELETE, 0, "Delete Folder");
        	menu.add(0, D_MENU_RENAME, 0, "Rename Folder");
        	menu.add(0, D_MENU_COPY, 0, "Copy Folder");
        	menu.add(0, D_MENU_PASTE, 0, "Paste into folder").setEnabled(holding_file || multi_data);
        	menu.add(0, D_MENU_ZIP, 0, "Zip Folder");        	
        	menu.add(0, D_MENU_UNZIP, 0, "Extract here").setEnabled(holding_zip);
    		
    	} else {
        	menu.setHeaderTitle("File Operations");
    		menu.add(0, F_MENU_DELETE, 0, "Delete File");
    		menu.add(0, F_MENU_RENAME, 0, "Rename File");
    		menu.add(0, F_MENU_COPY, 0, "Copy File");
    		menu.add(0, F_MENU_ATTACH, 0, "Email File");
    	}	
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {

    	switch(item.getItemId()) {
    		case D_MENU_DELETE:
    		case F_MENU_DELETE:
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setTitle("Warning ");
    			builder.setIcon(R.drawable.warning);
    			builder.setMessage("Deleting " + selected_list_item +
    							" cannot be undone. Are you sure you want to delete?");
    			builder.setCancelable(false);
    			
    			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
    			});
    			builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						handler.deleteFile(flmg.getCurrentDir() + "/" + selected_list_item);
					}
    			});
    			AlertDialog alert_d = builder.create();
    			alert_d.show();
    			return true;
    			
    		case D_MENU_RENAME:
    			showDialog(D_MENU_RENAME);
    			return true;
    			
    		case F_MENU_RENAME:
    			showDialog(F_MENU_RENAME);
    			return true;
    			
    		case F_MENU_ATTACH:
    			File file = new File(flmg.getCurrentDir() +"/"+ selected_list_item);
    			Intent mail_int = new Intent();
    			
    			mail_int.setAction(android.content.Intent.ACTION_SEND);
    			mail_int.setType("application/mail");
    			mail_int.putExtra(Intent.EXTRA_BCC, "");
    			mail_int.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
    			startActivity(mail_int);
    			return true;
    			
    		case F_MENU_COPY:
    		case D_MENU_COPY:
    			copied_target = flmg.getCurrentDir() +"/"+ selected_list_item;
    			holding_file = true;
    			detail_label.setText("Waiting to paste file " + selected_list_item);
    			return true;
    			
    		case D_MENU_PASTE:
    			boolean multi_select = handler.hasMultiSelectData();
    			
    			if(multi_select) {
    				handler.copyFileMultiSelect(flmg.getCurrentDir() +"/"+ selected_list_item);
    				
    			} else if(holding_file && copied_target.length() > 1) {
    				
    				handler.copyFile(copied_target, flmg.getCurrentDir() +"/"+ selected_list_item);
    				holding_file = false;
    				detail_label.setText("");
    			}
    			return true;
    			
    		case D_MENU_ZIP:
    			String dir = flmg.getCurrentDir();
    			
    			handler.zipFile(dir + "/" + selected_list_item);
    			return true;
    			
    		case D_MENU_UNZIP:
    			if(holding_zip && zipped_target.length() > 1) {
    				String current_dir = flmg.getCurrentDir() +"/" + selected_list_item + "/";
    				String old_dir = zipped_target.substring(0, zipped_target.lastIndexOf("/"));
    				String name = zipped_target.substring(zipped_target.lastIndexOf("/") + 1, zipped_target.length());
    				
    				if(new File(zipped_target).canRead() && new File(current_dir).canWrite()) {
	    				handler.unZipFileToDir(name, current_dir, old_dir);				
	    				path_label.setText(current_dir);
	    				
    				} else {
    					Toast.makeText(this, "You do not have permission to unzip " + name, 
    							Toast.LENGTH_SHORT).show();
    				}
    			}
    			
    			holding_zip = false;
    			detail_label.setText("");
    			zipped_target = "";
    			return true;
    	}
    	return false;
    }
    
    /* ================Menus, options menu and context menu end here=================*/

    @Override
    protected Dialog onCreateDialog(int id) {
    	final Dialog dialog = new Dialog(Main.this);
    	
    	switch(id) {
    		case MENU_MKDIR:
    			dialog.setContentView(R.layout.input_layout);
    			dialog.setTitle("Create New Directory");
    			dialog.setCancelable(false);
    			
    			ImageView icon = (ImageView)dialog.findViewById(R.id.input_icon);
    			icon.setImageResource(R.drawable.newfolder);
    			
    			TextView label = (TextView)dialog.findViewById(R.id.input_label);
    			label.setText(flmg.getCurrentDir());
    			final EditText input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button cancel = (Button)dialog.findViewById(R.id.input_cancel_b);
    			Button create = (Button)dialog.findViewById(R.id.input_create_b);
    			
    			create.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {
    					if (input.getText().length() > 1) {
    						if (flmg.createDir(flmg.getCurrentDir() + "/", input.getText().toString()) == 0)
    							Toast.makeText(Main.this, "Folder " + input.getText().toString() + " created", Toast.LENGTH_LONG)
    								.show();
    						else
    							Toast.makeText(Main.this, "New folder was not created", Toast.LENGTH_SHORT).show();
    					}
    					
    					dialog.dismiss();
    					String temp = flmg.getCurrentDir();
    					handler.updateDirectory(flmg.getNextDir(temp, true));
    				}
    			});
    			cancel.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {	dialog.dismiss(); }
    			});
    		break; 
    		case D_MENU_RENAME:
    		case F_MENU_RENAME:
    			dialog.setContentView(R.layout.input_layout);
    			dialog.setTitle("Rename " + selected_list_item);
    			dialog.setCancelable(false);
    			
    			ImageView rename_icon = (ImageView)dialog.findViewById(R.id.input_icon);
    			rename_icon.setImageResource(R.drawable.rename);
    			
    			TextView rename_label = (TextView)dialog.findViewById(R.id.input_label);
    			rename_label.setText(flmg.getCurrentDir());
    			final EditText rename_input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button rename_cancel = (Button)dialog.findViewById(R.id.input_cancel_b);
    			Button rename_create = (Button)dialog.findViewById(R.id.input_create_b);
    			rename_create.setText("Rename");
    			
    			rename_create.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {
    					if(rename_input.getText().length() < 1)
    						dialog.dismiss();
    					
    					if(flmg.renameTarget(flmg.getCurrentDir() +"/"+ selected_list_item, rename_input.getText().toString()) == 0) {
    						Toast.makeText(Main.this, selected_list_item + " was renamed to " +rename_input.getText().toString(),
    								Toast.LENGTH_LONG).show();
    					}else
    						Toast.makeText(Main.this, selected_list_item + " was not renamed", Toast.LENGTH_LONG).show();
    						
    					dialog.dismiss();
    					String temp = flmg.getCurrentDir();
    					handler.updateDirectory(flmg.getNextDir(temp, true));
    				}
    			});
    			rename_cancel.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {	dialog.dismiss(); }
    			});
    		break;
    		
    		case SEARCH_B:
    		case MENU_SEARCH:
    			dialog.setContentView(R.layout.input_layout);
    			dialog.setTitle("Search");
    			dialog.setCancelable(false);
    			
    			ImageView searchIcon = (ImageView)dialog.findViewById(R.id.input_icon);
    			searchIcon.setImageResource(R.drawable.search);
    			
    			TextView search_label = (TextView)dialog.findViewById(R.id.input_label);
    			search_label.setText("Search for a file");
    			final EditText search_input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button search_button = (Button)dialog.findViewById(R.id.input_create_b);
    			Button cancel_button = (Button)dialog.findViewById(R.id.input_cancel_b);
    			search_button.setText("Search");
    			
    			search_button.setOnClickListener(new OnClickListener() {
    				public void onClick(View v) {
    					String temp = search_input.getText().toString();
    					
    					if (temp.length() > 0)
    						handler.searchForFile(temp);
    					dialog.dismiss();
    				}
    			});
    			
    			cancel_button.setOnClickListener(new OnClickListener() {
    				public void onClick(View v) { dialog.dismiss(); }
    			});

    		break;
    	}
    	return dialog;
    }
    
    /*
     * (non-Javadoc)
     * This will check if the user is at root directory. If so, if they press back
     * again, it will close the application. 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
   public boolean onKeyDown(int keycode, KeyEvent event) {
    	String current = flmg.getCurrentDir();
    	
    	if(keycode == KeyEvent.KEYCODE_SEARCH) {
    		showDialog(SEARCH_B);
    		
    		return true;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && use_back_key && !current.equals("/")) {
    		if(handler.isMultiSelected()) {
    			table.killMultiSelect();
    			Toast.makeText(Main.this, "Multi-select is now off", Toast.LENGTH_SHORT).show();
    		}
    		
    		handler.updateDirectory(flmg.getPreviousDir());
    		path_label.setText(flmg.getCurrentDir());
    		
    		return true;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && use_back_key && current.equals("/")) {
    		Toast.makeText(Main.this, "Press back again to quit.", Toast.LENGTH_SHORT).show();
    		use_back_key = false;
    		path_label.setText(flmg.getCurrentDir());
    		
    		return false;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && !use_back_key && current.equals("/")) {
    		finish();
    		
    		return false;
    	}
    	return false;
    }
}
