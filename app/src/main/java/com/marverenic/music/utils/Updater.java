package com.marverenic.music.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.marverenic.music.BuildConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

public class Updater extends AsyncTask<Void, Void, String[]> {

    // BUILD CHANNEL
    private static final String CHANNEL = "alpha";
    //private static final String CHANNEL = "beta";
    //private static final String CHANNEL = "gamma";

    // Indices used in the result String[]
    private static final short DOWNLOAD_URL_INDEX = 0;
    private static final short RELEASE_NOTES_INDEX = 1;

    public static boolean hasRun = false; // Only run once per app start

    private Context context; // A context for internet communications
    private View view; // A view to put a Snackbar in

    /**
     * An Async Task used to check for updates
     * @param context A {@link Context} used for Internet communication
     * @param view A {@link View} used to display a {@link android.support.design.widget.Snackbar} with the result
     */
    public Updater(Context context, View view) {
        this.context = context;
        this.view = view;
    }

    @Override
    protected String[] doInBackground(Void... params) {
        if (hasRun) return null;
        hasRun = true;

        // Check with a URL to see if Jockey has an update
        ConnectivityManager network = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        NetworkInfo info = network.getActiveNetworkInfo();

        // Only check for an update if a valid network is present
        if(info != null && info.isAvailable() && !info.isRoaming() && (prefs.getBoolean("prefUseMobileData", true) || info.getType() != ConnectivityManager.TYPE_MOBILE)) {

            try {
                URL versionURL = new URL("https://raw.githubusercontent.com/marverenic/Jockey/build/" + CHANNEL + "/version");
                BufferedReader in = new BufferedReader(new InputStreamReader(versionURL.openStream()));

                final String code = in.readLine();
                final String downloadURL = in.readLine();
                final String notesURL = in.readLine();

                in.close();

                if (Integer.parseInt(code) > BuildConfig.VERSION_CODE) {
                    Scanner releaseNotesScanner = new Scanner(new URL(notesURL).openStream(), "UTF-8").useDelimiter("\\A");
                    String releaseNotes = releaseNotesScanner.next();
                    releaseNotesScanner.close();
                    return new String[]{downloadURL, releaseNotes};
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final String[] result) {
        super.onPostExecute(result);
        if (result == null) return;

        //TODO String resources
        Snackbar.make(view, "A new version of Jockey is available", Snackbar.LENGTH_LONG).setAction("Info", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alert = new AlertDialog.Builder(context)
                        .setTitle("Update Jockey")
                        .setMessage(result[RELEASE_NOTES_INDEX])
                        .setPositiveButton("Download now", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent downloadIntent = new Intent(Intent.ACTION_VIEW);
                                downloadIntent.setData(Uri.parse(result[DOWNLOAD_URL_INDEX]));
                                context.startActivity(downloadIntent);
                            }
                        })
                        .setNegativeButton("Remind me later", null)
                        .show();

                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Themes.getAccent());
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Themes.getAccent());
            }
        }).show();
    }
}
