/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.sms.SmsData;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.AboutActivity;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.util.debug.TestMock;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.sourcesview.SourcesTreeListActivity;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoMetadata;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.TableDescriptions;
import eu.hydrologis.geopaparazzi.maps.DataManager;
import eu.hydrologis.geopaparazzi.maps.LogMapItem;
import eu.hydrologis.geopaparazzi.maps.MapsActivity;
import eu.hydrologis.geopaparazzi.osm.OsmUtilities;
import eu.hydrologis.geopaparazzi.preferences.PreferencesActivity;
import eu.hydrologis.geopaparazzi.util.Constants;

import static eu.geopaparazzi.library.util.LibraryConstants.MAPSFORGE_EXTRACTED_DB_NAME;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD;

public class EclipsMainActivity extends Activity {
    public boolean criado = false;
    private static final int MENU_ABOUT = Menu.FIRST;
    private static final int MENU_EXIT = 2;
    private static final int MENU_TILE_SOURCE_ID = 3;
    private static final int MENU_SETTINGS = 4;
    private static final int MENU_RESET = 5;
    private static final int MENU_LOAD = 6;
    private static final int MAPSDIR_FILETREE = 777;

    private ResourcesManager resourcesManager;

    private final int RETURNCODE_BROWSE_FOR_NEW_PREOJECT = 665;
    private final int RETURNCODE_NOTES = 666;
    private final int RETURNCODE_PICS = 667;
    private final int RETURNCODE_SKETCH = 668;

    private BroadcastReceiver gpsServiceBroadcastReceiver;
    private GpsServiceStatus lastGpsServiceStatus = GpsServiceStatus.GPS_OFF;
    private GpsLoggingStatus lastGpsLoggingStatus = GpsLoggingStatus.GPS_DATABASELOGGING_OFF;
    private double[] lastGpsPosition;

    private static boolean checkedGps = false;
    private OrientationSensor orientationSensor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkIncomingProject();

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = new OrientationSensor(sensorManager, null);

        GpsServiceUtilities.startGpsService(this);
        gpsServiceBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                onGpsServiceUpdate(intent);
                checkFirstTimeGps(context);
            }
        };
        GpsServiceUtilities.registerForBroadcasts(this, gpsServiceBroadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(this);

        try {
            checkMockLocations();
            // clearCacheIfneeded();
            initializeResourcesManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
        checkIncomingGeosms();
        checkIncomingSmsData();
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            *//* Create an Intent that will start the Menu-Activity. *//*
                Intent mainIntent = new Intent(EclipsMainActivity.this, MapsActivity.class);
                startActivity(mainIntent);
                //finish();
            }
        }, 10000);*/
    }

    private void checkMockLocations() {
        /*
         * check mock locations availability
         */
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
        if (isMockMode) {
            if (!TestMock.isMockEnabled(getContentResolver())) {
                Utilities
                        .messageDialog(
                                this,
                                getString(R.string.enable_mock_locations_for_demo),
                                null);
                Editor edit = preferences.edit();
                edit.putBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
                edit.commit();
            }
        }

    }

    private void checkIncomingProject() {
        Uri data = getIntent().getData();
        if (data != null) {
            String path = data.getEncodedPath();
            if (path.endsWith(LibraryConstants.GEOPAPARAZZI_DB_EXTENSION)) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                Editor editor = preferences.edit();
                editor.putString(PREFS_KEY_DATABASE_TO_LOAD, path);
                editor.commit();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    /**
     * Checks if it was opened for a link of the kind:<br>
     * http://maps.google.com/maps?q=46.068941,11.169849&GeoSMS#usertext<br>
     * or<br>
     * http://www.openstreetmap.org/#map=19/46.67695/11.12605&layers=N#usertext<br>
     * in which case the point is imported as bookmark.
     */
    private void checkIncomingGeosms() {
        Uri data = getIntent().getData();
        if (data != null) {
            try {
                String path = data.toString();
                if (path.toLowerCase().contains(SmsUtilities.SMSHOST)) {
                    return;
                }
                // try google
                String[] latLonTextFromGmapUrl = Utilities.getLatLonTextFromGmapUrl(path);
                if (latLonTextFromGmapUrl != null) {
                    double lat = Double.parseDouble(latLonTextFromGmapUrl[0]);
                    double lon = Double.parseDouble(latLonTextFromGmapUrl[1]);
                    DaoBookmarks.addBookmark(lon, lat, latLonTextFromGmapUrl[2], 16, -1, -1, -1, -1);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    PositionUtilities.putMapCenterInPreferences(preferences, lon, lat, 16);

                    checkMapsLoadingFinished();
                    Intent mapIntent = new Intent(this, MapsActivity.class);
                    startActivity(mapIntent);
                } else {
                    // try osm
                    String[] latLonTextFromOsmUrl = Utilities.getLatLonTextFromOsmUrl(path);
                    if (latLonTextFromOsmUrl != null) {
                        double lat = Double.parseDouble(latLonTextFromOsmUrl[0]);
                        double lon = Double.parseDouble(latLonTextFromOsmUrl[1]);
                        int zoom = (int) Double.parseDouble(latLonTextFromOsmUrl[3]);
                        DaoBookmarks.addBookmark(lon, lat, latLonTextFromOsmUrl[2], zoom, -1, -1, -1, -1);
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                        PositionUtilities.putMapCenterInPreferences(preferences, lon, lat, 16);
                        checkMapsLoadingFinished();
                        Intent mapIntent = new Intent(this, MapsActivity.class);
                        startActivity(mapIntent);
                    }
                }

            } catch (IOException e) {
                GPLog.error(this, "Error parsing URI", e); //$NON-NLS-1$
                Utilities
                        .messageDialog(
                                this,
                                getString(R.string.could_not_open_geosms) + " ...&q=46.068941,11.169849&GeoSMS",
                                null);
            }
        }
    }

    private void checkIncomingSmsData() {
        /*
         * check if it was opened for a link of the kind
         *
         * http://maps.google.com/maps?q=46.068941,11.169849&GeoSMS
         */
        Uri data = getIntent().getData();
        if (data != null) {
            try {
                String path = data.toString();
                String scheme = data.getScheme(); // "http"
                if (scheme != null && scheme.equals("http")) { //$NON-NLS-1$
                    String host = data.getHost();
                    if (host.equals(SmsUtilities.SMSHOST)) {

                        List<SmsData> sms2Data = SmsUtilities.sms2Data(path);
                        int notesNum = 0;
                        int bookmarksNum = 0;
                        if (sms2Data.size() > 0) {
                            for (SmsData smsData : sms2Data) {
                                String text = smsData.text.replaceAll("\\_", " "); //$NON-NLS-1$//$NON-NLS-2$
                                if (smsData.TYPE == SmsData.NOTE) {
                                    long timestamp = new Date().getTime();
                                    DaoNotes.addNote(smsData.x, smsData.y, smsData.z, timestamp, text, "SMS", null,
                                            null);
                                    notesNum++;
                                } else if (smsData.TYPE == SmsData.BOOKMARK) {
                                    DaoBookmarks.addBookmark(smsData.x, smsData.y, text, smsData.z, -1, -1, -1, -1);
                                    bookmarksNum++;
                                }
                            }
                        }

                        Utilities.messageDialog(this, MessageFormat.format(
                                getString(eu.hydrologis.geopaparazzi.R.string.imported_notes_and_bookmarks), notesNum,
                                bookmarksNum), null);
                    }
                }
            } catch (Exception e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
                Utilities.messageDialog(this, getString(eu.hydrologis.geopaparazzi.R.string.could_not_open_sms), null);
            }
        }
    }


    private void checkMapsLoadingFinished() {
        int maxTimes = 0;
        while (!MapsDirManager.getInstance().finishedLoading() && maxTimes++ < 200) { // max wait 60 seconds
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationSensor.unregister();


    }


    protected void onResume() {
        super.onResume();
        orientationSensor.register(this, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // avoid oncreate call when rotating device
        super.onConfigurationChanged(newConfig);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }



    private void initializeResourcesManager() throws Exception {
        ResourcesManager.resetManager();
        resourcesManager = ResourcesManager.getInstance(this);

        if (resourcesManager == null) {
            Utilities.yesNoMessageDialog(this, getString(eu.hydrologis.geopaparazzi.R.string.no_sdcard_use_internal_memory),
                    new Runnable() {
                        public void run() {
                            ResourcesManager.setUseInternalMemory(true);
                            try {
                                resourcesManager = ResourcesManager.getInstance(EclipsMainActivity.this);
                                initIfOk();

                            } catch (Exception e) {
                                GPLog.error(this, null, e); //$NON-NLS-1$
                            }
                        }
                    }, new Runnable() {
                        public void run() {
                            finish();
                        }
                    }
            );
        } else {
            // create the default mapsforge data extraction db
            File mapsDir = resourcesManager.getMapsDir();
            File newDbFile = new File(mapsDir, MAPSFORGE_EXTRACTED_DB_NAME);
            if (!newDbFile.exists()) {
                AssetManager assetManager = this.getAssets();
                InputStream inputStream = assetManager.open(MAPSFORGE_EXTRACTED_DB_NAME);
                FileUtilities.copyFile(inputStream, new FileOutputStream(newDbFile));
            }
            // initialize rest of resources
            initIfOk();
        }

    }

    private void initIfOk() {
        if (resourcesManager == null) {
            Utilities.messageDialog(this, R.string.sdcard_notexist, new Runnable() {
                public void run() {
                    finish();
                }
            });
            return;
        }
        setContentView(R.layout.eclips_main);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        GPLogPreferencesHandler.checkLog(preferences);
        GPLogPreferencesHandler.checkLogHeavy(preferences);
        GPLogPreferencesHandler.checkLogAbsurd(preferences);


        /*
         * the buttons
         */

        final int apoioButtonId = R.id.apoio_item_button;
        ImageButton apoioButton = (ImageButton) findViewById(apoioButtonId);
        apoioButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(apoioButtonId, v);
            }
        });


        final int coletaButtonId = R.id.coleta_item_button;
        ImageButton coletaButton = (ImageButton) findViewById(coletaButtonId);
        coletaButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(coletaButtonId, v);
            }
        });


        final int enviarButtonId = R.id.enviar_item_button;
        ImageButton enviarButton = (ImageButton) findViewById(enviarButtonId);
        enviarButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(enviarButtonId, v);
            }
        });


        final int receberButtonId = R.id.receber_item_button;
        ImageButton receberButton = (ImageButton) findViewById(receberButtonId);
        receberButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(receberButtonId, v);
            }
        });


        final int bussolaButtonId = R.id.bussola_item_button;
        ImageButton bussolaButton = (ImageButton) findViewById(bussolaButtonId);
        bussolaButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(bussolaButtonId, v);
            }
        });


        final int configButtonId = R.id.config_item_button;
        ImageButton configButton = (ImageButton) findViewById(configButtonId);
        configButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(configButtonId, v);
            }
        });



        boolean doOsmPref = preferences.getBoolean(Constants.PREFS_KEY_DOOSM, false);
        if (doOsmPref)
            OsmUtilities.handleOsmTagsDownload(this);

        //        Utilities.toast(this, getString(eu.hydrologis.geopaparazzi.R.string.loaded_project_in)
        //                + resourcesManager.getDatabaseFile().getAbsolutePath(), Toast.LENGTH_LONG);

        // check for screen on
        boolean keepScreenOn = preferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        try {
            GeopaparazziApplication.getInstance().getDatabase();
            checkMapsAndLogsVisibility();

            HashMap<String, String> projectMetadata = DaoMetadata.getProjectMetadata();
            String projectName = projectMetadata.get(TableDescriptions.MetadataTableFields.KEY_NAME.getFieldName());
            if (projectName.length() == 0) {
                File dbFile = resourcesManager.getDatabaseFile();
                String dbName = FileUtilities.getNameWithoutExtention(dbFile);
                DaoMetadata.setValue(TableDescriptions.MetadataTableFields.KEY_NAME.getFieldName(), dbName);
            }

            initMapsDirManager();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
            Utilities.toast(this, R.string.databaseError, Toast.LENGTH_LONG);
        }
    }

    private void initMapsDirManager() throws jsqlite.Exception, IOException {
        MapsDirManager.reset();

//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        final ProgressDialog initMapsdirDialog = new ProgressDialog(this);
        initMapsdirDialog.setCancelable(true);
        initMapsdirDialog.setTitle(getString(R.string.maps_manager));
        initMapsdirDialog.setMessage(getString(R.string.loading_maps));
        initMapsdirDialog.setCancelable(false);
        initMapsdirDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        initMapsdirDialog.setIndeterminate(true);
        initMapsdirDialog.show();

        AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                try {
                    //MapsDirManager.reset();
                    //criado = MapsDirManager.getInstance().getCriado();
                    MapsDirManager.getInstance().init(EclipsMainActivity.this, null);

                    return "";
                } catch (Exception e) {

                    GPLog.error(this, null, e); //$NON-NLS-1$

                    // reset mapdirsfolder
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EclipsMainActivity.this);
                    Editor editor = preferences.edit();
                    editor.putString(LibraryConstants.PREFS_KEY_CUSTOM_MAPSFOLDER, "asdasdpoipoi");
                    editor.commit();

                    return "ERROR: " + e.getLocalizedMessage();
                }


            }

            protected void onPostExecute(String response) { // on UI thread!
                Utilities.dismissProgressDialog(initMapsdirDialog);
                if (response.startsWith("ERROR")) {
                    String kitkatErrorSdcardCheck = "not an error (code 0): Could not open the database in read/write mode";
                    if (response.contains(kitkatErrorSdcardCheck)) {
                        response = response + "\n\nIf your data are on the sdcard and your Android version is KitKat, then Geopaparazzi has no write permissions (read for more info: https://code.google.com/p/android/issues/detail?id=67570)";
                    }
                    Utilities.messageDialog(EclipsMainActivity.this, response, null);
                }

//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        };
        try {
            asyncTask.execute((String) null);
            Log.i("ASYNCTASK", getClass().getSimpleName());
            Log.i("ASYNCTASK", asyncTask.toString());
            Log.i("ASYNCTASK", asyncTask.getStatus().toString());
            Log.i("ASYNCTASK", asyncTask.getClass().toString());

        } catch (Exception e) {

            Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
        }
        // MapsDirManager will read the preferences values for the last map
        // - it will collect all information about the maps on the sdcard/maps
        // -- when the preferred map is found, this data will be stored
        // --- when the Map-Activity is created:
        // --- the selected map will be loaded with
        // MapsDirManager.load_Map(map_view,mapCenterLocation);
        //asyncTask.cancel(true);
    }

    /**
     * Push action.
     *
     * @param id the calling id.
     * @param v  parent view.
     */
    public void push(int id, View v) {
        switch (id) {

            case R.id.coleta_item_button: {
                Intent i = new Intent("com.eclips.collect.android.activities.GeoODKMapThemeActivity");
                startActivity(i);
                break;
            }


            case R.id.apoio_item_button: {
                if (!MapsDirManager.getInstance().finishedLoading()) {
                    Utilities.messageDialog(this, getString(R.string.maps_loading_not_finished_yet), null);
                    return;
                }
                Intent mapIntent = new Intent(this, MapsActivity.class);
                mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mapIntent);

                break;
            }

            case R.id.bussola_item_button: {


                Context context = this.getApplicationContext();
                openCompass(context);

                break;
            }

            case R.id.config_item_button: {
                Intent i = new Intent("eu.hydrologis.geopaparazzi.EclipsConfigActivity");
                startActivity(i);
                break;
            }

            case R.id.enviar_item_button: {
                Intent i = new Intent("com.eclips.collect.android.activities.InstanceUploaderList");
                startActivity(i);
                break;
            }

            case R.id.receber_item_button: {
                Intent i = new Intent("com.eclips.collect.android.activities.UpdateActivity");
                startActivity(i);
                break;
            }


            default:
                break;
        }
    }


    @Override
    public void onBackPressed() {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, MENU_TILE_SOURCE_ID, 0, R.string.mapsactivity_menu_tilesource).setIcon(R.drawable.ic_menu_tilesource);
        menu.add(Menu.NONE, MENU_RESET, 1, R.string.reset).setIcon(android.R.drawable.ic_menu_revert);
        menu.add(Menu.NONE, MENU_LOAD, 2, R.string.load).setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(Menu.NONE, MENU_SETTINGS, 3, R.string.mainmenu_preferences).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, MENU_ABOUT, 4, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, MENU_EXIT, 45, R.string.exit).setIcon(android.R.drawable.ic_lock_power_off);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ABOUT:
                Intent intent = new Intent(this, AboutActivity.class);
                intent.putExtra(LibraryConstants.PREFS_KEY_TEXT, "eu.hydrologis.geopaparazzi"); //$NON-NLS-1$
                startActivity(intent);
                return true;
            case MENU_SETTINGS:
                Intent preferencesIntent = new Intent(this, PreferencesActivity.class);
                startActivity(preferencesIntent);
                return true;
            case MENU_RESET:
                createNewProject();
                return true;
            case MENU_LOAD:
                Intent browseIntent = new Intent(this, DirectoryBrowserActivity.class);
                browseIntent.putExtra(DirectoryBrowserActivity.EXTENTION, LibraryConstants.GEOPAPARAZZI_DB_EXTENSION);
                browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, resourcesManager.getSdcardDir()
                        .getAbsolutePath());
                startActivityForResult(browseIntent, RETURNCODE_BROWSE_FOR_NEW_PREOJECT);
                return true;
            case MENU_EXIT:
                finish();
                //super.finish();
                return true;
            case MENU_TILE_SOURCE_ID:
                startMapsDirTreeViewList();
        }

        return super.onContextItemSelected(item);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // force to exit through the exit button
        // System.out.println(keyCode + "/" + KeyEvent.KEYCODE_BACK);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:

                /*backCount++;
                long backTime = System.currentTimeMillis();

                if (backTime - previousBackTime < 1000) {
                    if (backCount > 3) {*/
                   //    super.finish();
                /*+    }
                } else {
                    backCount = 0;
                }
                previousBackTime = backTime;*/
                //return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Start the Dialog to select a map
     * <p/>
     * <p>
     * MapDirManager creates a static-list of maps and sends it to the MapsDirTreeViewList class
     * - when first called this list will build a directory/file list AND a map-type/Directory/File list
     * - once created, this list will be retained during the Application
     * - the user can switch from a sorted list as Directory/File OR Map-Type/Directory/File view
     * </p>
     * result will be sent to MapDirManager and saved there and stored to preferences
     * - when the MapView is created, this stored value will be read and loaded
     */
    private void startMapsDirTreeViewList() {
        try {
            // startActivityForResult(new Intent(this, MapsDirTreeViewList.class),
            // MAPSDIR_FILETREE);
            startActivityForResult(new Intent(this, SourcesTreeListActivity.class), MAPSDIR_FILETREE);
        } catch (Exception e) {
            GPLog.error(this, "GeoPaparazziActivity -E-> failed[startActivity(new Intent(this,MapsDirTreeViewList.class));]", e); //$NON-NLS-1$
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MAPSDIR_FILETREE: {
                if (resultCode == Activity.RESULT_OK) {
                    // String s_SELECTED_FILE=data.getStringExtra(MapsDirTreeViewList.SELECTED_FILE);
                    // String s_SELECTED_TYPE=data.getStringExtra(MapsDirTreeViewList.SELECTED_TYPE);
                    // selected_classinfo will contain all information about the map
                    // MapsDirManager will store this internally and store the values to preferences
                    // - if this is called from a non-Map-Activity:
                    // -- the map_view parameter and the position parameter MUST be null
                    // - if this is called from a Map-Activity:
                    // -- the map_view parameter and the position parameter should be given
                    // --- the position parameter is not given [null], it will use the position of the
                    // map_view
                    // -- if not null : selected_MapClassInfo() will call
                    // MapsDirManager.load_Map(map_view,mapCenterLocation);

                    // if (MapsDirTreeViewList.selected_classinfo != null) {
                    // MapsDirManager.getInstance().selectMapClassInfo(this,
                    // MapsDirTreeViewList.selected_classinfo, null, null);
                    // }
                }
            }
            break;
            case (RETURNCODE_BROWSE_FOR_NEW_PREOJECT): {
                if (resultCode == Activity.RESULT_OK) {
                    String databasePathToLoad = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                    if (databasePathToLoad != null && new File(databasePathToLoad).exists()) {
                        setNewDatabase(databasePathToLoad);

                        recreateActivity();
                    }
                }
                break;
            }
            case (RETURNCODE_NOTES): {
                if (resultCode == Activity.RESULT_OK) {
                    String[] noteArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_NOTE);
                    if (noteArray != null) {
                        try {
                            double lon = Double.parseDouble(noteArray[0]);
                            double lat = Double.parseDouble(noteArray[1]);
                            double elev = Double.parseDouble(noteArray[2]);
                            DaoNotes.addNote(lon, lat, elev, Long.parseLong(noteArray[3]), noteArray[4], "POI", null,
                                    null);
                        } catch (Exception e) {
                            GPLog.error(this, null, e); //$NON-NLS-1$
                            Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                        }
                    }
                }
                break;
            }
        }
    }

    private void setNewDatabase(String databasePathToLoad) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            Editor editor = preferences.edit();
            editor.putString(LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD, databasePathToLoad);
            editor.commit();
            GeopaparazziApplication.getInstance().closeDatabase();
            ResourcesManager.resetManager();
            GeopaparazziApplication.getInstance().getDatabase();
        } catch (IOException e) {
            Utilities.errorDialog(this, e, null);
        }
    }

    private int backCount = 0;
    private long previousBackTime = System.currentTimeMillis();



    public void finish() {
        //try {
            //if (GPLog.LOG)
                Log.w("ECLIPSMAPACT", "Finish called!"); //$NON-NLS-1$ //$NON-NLS-2$

            // set last closing timestamp
          //  DaoMetadata.setValue(TableDescriptions.MetadataTableFields.KEY_LASTTS.getFieldName(), new Date().getTime() + "");

          //  TagsManager.reset(this);

            // save last location just in case
            if (resourcesManager == null) {
                super.finish();
                return;
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (lastGpsPosition != null) {
                PositionUtilities.putGpsLocationInPreferences(preferences, lastGpsPosition[0], lastGpsPosition[1],
                        lastGpsPosition[2]);
            }
            Utilities.toast(this, R.string.loggingoff, Toast.LENGTH_LONG);

            if (gpsServiceBroadcastReceiver != null) {
                GpsServiceUtilities.stopDatabaseLogging(this);
                GpsServiceUtilities.stopGpsService(this);
                GpsServiceUtilities.unregisterFromBroadcasts(this, gpsServiceBroadcastReceiver);

            }
            //try {
                MapsDirManager.getInstance().finish();

            //} catch (Exception e) {
            //    GPLog.error(this, null, e); //$NON-NLS-1$
            //}
            // try {

            GeopaparazziApplication.getInstance().closeDatabase();

            //} catch (Exception e) {
            //    e.printStackTrace();
            //}
            ResourcesManager.resetManager();
            resourcesManager = null;

            // Editor edit = preferences.edit();
            // edit.putBoolean("EXIT_THROUGH_FINISH", true);
            // edit.commit();
    //    } catch (Exception e1) {
    //        GPLog.error(this, null, e1); //$NON-NLS-1$
    //    } finally {
            checkedGps = false;
        super.finish();

    //    }


    }

    private void createNewProject() {
        final String enterNewProjectString = getString(eu.hydrologis.geopaparazzi.R.string.enter_a_name_for_the_new_project);
        final String projectExistingString = getString(eu.hydrologis.geopaparazzi.R.string.chosen_project_exists);

        final File sdcardDir = resourcesManager.getSdcardDir();
        final String newGeopaparazziProjectName = Constants.GEOPAPARAZZI
                + "_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(eu.geopaparazzi.library.R.layout.inputdialog);
        final TextView text = (TextView) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogtext);
        text.setText(enterNewProjectString);
        CheckBox checkBox = (CheckBox) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogcheckBox);
        checkBox.setVisibility(View.GONE);
        final EditText editText = (EditText) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogEdittext);
        final Button yesButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonOK);
        editText.setText(newGeopaparazziProjectName);
        editText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            public void afterTextChanged(Editable s) {
                String newName = s.toString();
                if (!newName.endsWith(LibraryConstants.GEOPAPARAZZI_DB_EXTENSION)) {
                    newName = newName + LibraryConstants.GEOPAPARAZZI_DB_EXTENSION;
                }
                File newProjectFile = new File(sdcardDir, newName);
                if (newName.length() < 1) {
                    text.setText(enterNewProjectString);
                    yesButton.setEnabled(false);
                } else if (newProjectFile.exists()) {
                    yesButton.setEnabled(false);
                    text.setText(projectExistingString);
                } else {
                    text.setText(enterNewProjectString);
                    yesButton.setEnabled(true);
                }
            }
        });
        yesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Editable value = editText.getText();
                    String newName = value.toString();
                    GeopaparazziApplication.getInstance().closeDatabase();
                    File newGeopaparazziFile = new File(sdcardDir.getAbsolutePath(), newName + LibraryConstants.GEOPAPARAZZI_DB_EXTENSION);
                    setNewDatabase(newGeopaparazziFile.getAbsolutePath());
                    dialog.dismiss();

                    recreateActivity();

                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    Toast.makeText(EclipsMainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        Button cancelButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void recreateActivity() {
        if (gpsServiceBroadcastReceiver != null) {
            GpsServiceUtilities.stopDatabaseLogging(this);
            GpsServiceUtilities.stopGpsService(this);
            GpsServiceUtilities.unregisterFromBroadcasts(this, gpsServiceBroadcastReceiver);
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 11) {
                    recreate();
                } else {
                    Intent intent = getIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);

                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
            }
        }, 10);


    }

    /**
     * Check for:
     * visibility of gpslogs (turned on or off?)
     */
    private static void checkMapsAndLogsVisibility() throws IOException {
        List<LogMapItem> maps = DaoGpsLog.getGpslogs();
        boolean oneVisible = false;
        for (LogMapItem item : maps) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setLogsVisible(oneVisible);
    }

    public static void openCompass(final Context context) {
        String gpsStatusAction = "com.eclipsim.gpsstatus.VIEW";
        String gpsStatusPackage = "com.eclipsim.gpsstatus";
        boolean hasGpsStatus = false;
        List<PackageInfo> installedPackages = new ArrayList<PackageInfo>();

        { // try to get the installed packages list. Seems to have troubles over different
            // versions, so trying them all
            try {
                installedPackages = context.getPackageManager().getInstalledPackages(0);
            } catch (Exception e) {
                // ignore
            }
            if (installedPackages.size() == 0)
                try {
                    installedPackages = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
                } catch (Exception e) {
                    // ignore
                }
        }

        if (installedPackages.size() > 0) {
            // if a list is available, check if the status gps is installed
            for (PackageInfo packageInfo : installedPackages) {
                String packageName = packageInfo.packageName;
                    GPLog.addLogEntry("ACTIONBAR", packageName);
                if (packageName.startsWith(gpsStatusPackage)) {
                    hasGpsStatus = true;
                        GPLog.addLogEntry("ACTIONBAR", "Found package: " + packageName);
                    break;
                }
            }
        } else {
            /*
             * if no package list is available, for now try to fire it up anyways.
             * This has been a problem for a user on droidx with android 2.2.1.
             */
            hasGpsStatus = true;
        }

        if (hasGpsStatus) {
            Intent intent = new Intent(gpsStatusAction);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            new AlertDialog.Builder(context).setTitle(context.getString(R.string.installgpsstatus_title))
                    .setMessage(context.getString(R.string.installgpsstatus_message)).setIcon(android.R.drawable.ic_dialog_info)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // ignore
                        }
                    }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://search?q=com.eclipsim.gpsstatus2"));
                    context.startActivity(intent);
                }
            }).show();
        }
    }



    private void onGpsServiceUpdate(Intent intent) {
        lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        lastGpsPosition = GpsServiceUtilities.getPosition(intent);
        float[] lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
        int[] lastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
        long lastPositiontime = GpsServiceUtilities.getPositionTime(intent);

    }

    private void checkFirstTimeGps(Context context) {
        if (!checkedGps) {
            checkedGps = true;
            if (lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                String prompt = getResources().getString(R.string.prompt_gpsenable);
                Utilities.yesNoMessageDialog(context, prompt, new Runnable() {
                    public void run() {
                        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }
                }, null);
            }
        }
    }

}
