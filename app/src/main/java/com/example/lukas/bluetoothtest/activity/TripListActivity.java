package com.example.lukas.bluetoothtest.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.trip.TripProvider;
import com.pdfjet.*;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Lukas on 30.12.2017.
 */

public class TripListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_trip_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.triplist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.pdf:
                try {
                    pdfExport();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void pdfExport() throws Exception {
        Log.e(getClass().getName(), "PDF-Export");
        final Cursor cursor;

        String state = Environment.getExternalStorageState();
        //check if the external directory is availabe for writing
        File exportDir;
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            throw new Exception("External directory is not available for writing");
        }
        else {
            exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }

        //if the external storage directory does not exists, we create it
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File file;
        file = new File(exportDir, getResources().getString(R.string.list_pdf_doc_name));

        //PDF is a class of the PDFJET library
        PDF pdf = null;
        pdf = new PDF(new FileOutputStream(file));

        //instructions to create the pdf file content
        //first we create a page with portrait orientation
        Page page = new Page(pdf, Letter.PORTRAIT);

        // TITEL
        //font of the title
        Font f1 = new Font(pdf, CoreFont.HELVETICA_BOLD);
        f1.setSize(7.0f);
        //title: font f1 and color blue
        TextLine title = new TextLine(f1, getResources().getString(R.string.list_pdf_title));
        title.setFont(f1);
        title.setColor(Color.blue);
        //center the title horizontally on the page
        title.setPosition(page.getWidth()/2 - title.getWidth()/2, 40f);
        //draw the title on the page
        title.drawOn(page);


        pdf.flush();

        //cursor = this.getContentResolver().query(TripProvider.CONTENT_URI, null, null, null, null);

        //while(cursor.moveToNext()) {

        //}

    }
}
