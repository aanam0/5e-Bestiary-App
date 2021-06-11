package com.example.alexander.mywebviewapp2;

import android.os.Environment;
import android.service.autofill.CharSequenceTransformation;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.widget.ImageButton;
import android.widget.Toast;
import android.annotation.TargetApi;
import java.util.ArrayList;
import android.graphics.Bitmap;
import java.io.*;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends AppCompatActivity {

    private static WebView mWebView;
    private static NavigationView navView;
    private static DrawerLayout drawerLayout;
    private static ArrayList<String> favourites = new ArrayList<>();
    private static boolean isFav=false;
    private static ImageButton button2;
    private static ImageButton button3;
    private static File file;
    private static File path;
    private static String filename = "favourites.txt";
    private static FileOutputStream outputStream;
    private static Context context;
    private static HashMap<String,String> menuSelections;
    private static AppCompatActivity activity;

    /**
     * @param savedInstanceState
     * Entry point
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Init file and favourites
        path = this.getFilesDir();
        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),filename);
        menuSelections = new HashMap<>();

        // Init android app stuff
        context = this;
        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.activity_main_webview);
        navView = findViewById(R.id.nav_view);
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(Gravity.START);
        activity = this;

        // Init favourited items
        readFromFile();
        writeMenu(navView);

        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Setup favourite button events
        button2 = findViewById(R.id.imageButton2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                setFavourites(mWebView);
            }
        });

        // Setup navigation menu events
        navView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        mWebView.loadUrl(menuSelections.get(menuItem.getTitle()));
                        drawerLayout.closeDrawer(Gravity.START);
                        return true;
                    }
                }
        );

        // Setup recall favourites button
        button3 = findViewById(R.id.imageButton3);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                String text="";
                for (int i=0; i < favourites.size(); i++) {
                    text += favourites.get(i).split(",")[0] + "\n";
                }

                // Toggle drawer open/close on click
                if (drawerLayout.isDrawerOpen(Gravity.START))
                    drawerLayout.closeDrawer(Gravity.START);
                else
                    drawerLayout.openDrawer(Gravity.START);
            }
        });


        // Actions for the webview client
        mWebView.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
            }
            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                checkFavourite(view);
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                checkFavourite(view);
            }
        });

        // Load the base url
        mWebView.loadUrl("https://aanam0.github.io/bestiary/");
    }

    /**
     * @param view
     * Clean the url of #top
     * Checks if the current webpage exists in the favourites, if it exists then set the star to filled
     * If the current webpage isn't in the favourites, set to empty star
     */
    public void checkFavourite (WebView view) {
        String truncatedURL = view.getUrl();
        if (truncatedURL.contains("#"))
            truncatedURL = view.getUrl().substring(0,view.getUrl().indexOf("#"));

        if (!favourites.contains(view.getTitle() + ","+ truncatedURL)) {
            button2.setImageResource(R.drawable.ic_star_border_green);
        }
        else if (favourites.contains(view.getTitle() + "," + truncatedURL)) {
            button2.setImageResource(R.drawable.ic_star_green_fill);
        }
    }

    /**
     * @param view
     * Cleanse the URL of #top
     * If favourites does not contain the current page, add it to favourites ArrayList
     *  Write to the persistent file
     *  Add to the menu
     * If favourites DOES contain the current page, remove from favourites ArrayList
     *  Re-write the persistent file
     *  Re-draw the menu
     */
    public void setFavourites (WebView view) {
       String truncatedURL = view.getUrl();
       if (truncatedURL.contains("#"))
           truncatedURL = truncatedURL.substring(0, truncatedURL.indexOf("#"));

       // If the current page is not in the favourites
       if (!favourites.contains(mWebView.getTitle() + "," + truncatedURL)) {
           favourites.add(mWebView.getTitle() + "," + truncatedURL);
           Toast.makeText(activity, "Added to favourites", Toast.LENGTH_SHORT).show();
           writeToFile();
           writeMenu(navView);
       }
       else {
           favourites.remove(mWebView.getTitle() + "," + truncatedURL);
           Toast.makeText(activity, "Removed to favourites", Toast.LENGTH_SHORT).show();
           writeToFile();
           writeMenu(navView);
       }
       checkFavourite(view);
    }

    /**
     * Writes the current set of favourites to the persistent file
     * If the current web page has a '#', expect it to be "{URL}#top" so we ignore everything after #
     */
    private void writeToFile() {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            for (String favourite : favourites) {
                if (favourite.contains("#"))
                    favourite = favourite.substring(0, favourite.indexOf("#"));
                fos.write(favourite.getBytes());
                fos.write("\n".getBytes());
            }
            fos.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    /**
     * Read from the persistent file
     * Clear then setup ArrayList favourites
     */
    private void readFromFile() {
        FileInputStream is;
        BufferedReader reader;
        favourites.clear();
        try {

            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while (line != null) {
                    Log.d("StackOverflow", line);
                    favourites.add(line);
                    line = reader.readLine();
                }
            }
        } catch (Exception e){
            Log.d("ExceptionHELLO",e.toString());
        }
    }

    /**
     * @param navigationView
     * Re-write the menu
     * Clear all menu items and selections
     * Sync the menu items with ArrayList favourites
     */
    private void writeMenu(NavigationView navigationView) {
        Menu menu = navigationView.getMenu();
        menu.clear();
        menuSelections.clear();
        String menuTitle="";
        String menuURL="";
        for (String favourite : favourites) {
            menuTitle = favourite.split(",")[0];
            menuURL = favourite.split(",")[1];
            menu.add(menuTitle);
            menuSelections.put(menuTitle,menuURL);
        }

    }

    /**
     * Back button functionality
     */
    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else if (drawerLayout.isDrawerOpen(Gravity.START)){
            drawerLayout.closeDrawer(Gravity.START);
        }
        else {
            super.onBackPressed();
        }
    }

}
