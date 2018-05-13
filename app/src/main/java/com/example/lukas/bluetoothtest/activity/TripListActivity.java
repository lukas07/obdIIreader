package com.example.lukas.bluetoothtest.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.trip.TripOpenHelper;
import com.example.lukas.bluetoothtest.trip.TripProvider;
import com.example.lukas.bluetoothtest.trip.TripsAdapter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Created by Lukas on 30.12.2017.
 */

public class TripListActivity extends AppCompatActivity {
    private static final String CLASS = TripListActivity.class.getName();
    final private int REQUEST_CODE_ASK_PERMISSIONS = 111;
    private File pdfFile;
    private Document document;
    private PdfPTable table;
    private Font times12, times12Bold, times16Bold;

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
                    createPdfWrapper();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Vor der PDF-Erzeugung werden Berechtigungen für das Lesen/Schreiben des Speichers überprüft
    private void createPdfWrapper() throws FileNotFoundException {

        int hasWriteStoragePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)) {
                    showMessageOKCancel(getResources().getString(R.string.list_storage_access),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                }
                            });
                    return;
                }

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
            return;
        }else {
            try {
                createPdf();
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    try {
                        createPdfWrapper();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Permission Denied
                    Toast.makeText(this, getResources().getString(R.string.list_storage_denied), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(getResources().getString(R.string.list_storage_ok), okListener)
                .setNegativeButton(getResources().getString(R.string.list_storage_cancel), null)
                .create()
                .show();
    }

    private void createPdf() throws DocumentException {

        File docsFolder = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        if (!docsFolder.exists()) {
            docsFolder.mkdir();
            Log.i(CLASS, "Created a new directory for PDF");
        }

        Date date = new Date() ;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);

        pdfFile = new File(docsFolder.getAbsolutePath(),getResources().getString(R.string.list_pdf_doc_name) + timeStamp + ".pdf");
        OutputStream output = null;
        try {
            output = new FileOutputStream(pdfFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        document = new Document();
        // Dokument-Eigenschaften
        document.setPageSize(PageSize.A4.rotate());
        document.setMargins(20, 20, 40, 20);

        // Header des PDF's
        PdfWriter writer = PdfWriter.getInstance(document, output);
        PdfHeader header = new PdfHeader();
        writer.setPageEvent(header);

        document.open();

        // Inhalt einfügen
        try {
            pdfAddContent();
        } catch (IOException e) {
            e.printStackTrace();
        }


        document.close();
        PdfWriter.getInstance(document, output).close();
        previewPdf();

    }


    private void pdfAddContent() throws IOException, DocumentException {
        // Schriftarten des Dokumentes
        times16Bold = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
        times12Bold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        times12 = new Font(Font.FontFamily.TIMES_ROMAN, 12);

        // Titel einfügen
        Paragraph title = new Paragraph(getResources().getString(R.string.list_pdf_title), times16Bold);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        // Leerzeile einfügen
        document.add(new Paragraph(" "));

        // Tabelle einfügen
        table  = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{1,2,2,2,1,1,2,2,3,3});
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_counter), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_driver), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_mode), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_reason), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_odo_start), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_odo_end), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_date_start), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_date_end), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_address_start), times12Bold));
        table.addCell(getCell(getResources().getString(R.string.list_pdf_table_address_end), times12Bold));
        table.setHeaderRows(1);

        // DB auslesen
        final Cursor cursor;
        cursor = this.getContentResolver().query(TripProvider.CONTENT_URI, null, null, null, null);

        int counter = 0;
        while(cursor.moveToNext()) {
            counter++;

            table.addCell(getCell(String.valueOf(counter), times12));
            table.addCell(getCell(cursor.getString(TripOpenHelper.COL_ID_DRIVER), times12));
            table.addCell(getCell(cursor.getString(TripOpenHelper.COL_ID_MODE), times12));
            table.addCell(getCell(cursor.getString(TripOpenHelper.COL_ID_REASON), times12));
            table.addCell(getCell(cursor.getString(TripOpenHelper.COL_ID_STARTMIL), times12));
            table.addCell(getCell(cursor.getString(TripOpenHelper.COL_ID_ENDMIL), times12));
            String startts = TripsAdapter.convertDate(cursor.getLong(TripOpenHelper.COL_ID_STARTTS));
            table.addCell(getCell(startts, times12));
            String endts = TripsAdapter.convertDate(cursor.getLong(TripOpenHelper.COL_ID_STARTTS));
            table.addCell(getCell(endts, times12));
            table.addCell(getCell(cursor.getString(TripOpenHelper.COL_ID_STARTADD), times12));
            table.addCell(getCell(cursor.getString(TripOpenHelper.COL_ID_ENDADD), times12));

        }
        // Falls noch keine Trips aufgezeichnet wurden
        if (counter == 0) {
            PdfPCell cell = new PdfPCell(new Phrase(getResources().getString(R.string.list_pdf_table_noData)));
            cell.setColspan(9);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Paragraph tableContainer = new Paragraph();
        tableContainer.add(table);
        document.add(table);
    }

    // Erzeugt eine Zelle des Tabellenkopfes und formatiert diese
    private PdfPCell getCell (String string, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(string, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }


    // Anzeige des erzeugten PDF's
    private void previewPdf() {
        PackageManager packageManager = getPackageManager();
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        testIntent.setType("application/pdf");
        List list = packageManager.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(pdfFile);
            intent.setDataAndType(uri, "application/pdf");

            startActivity(intent);
        }else{
            Toast.makeText(this,"Download a PDF Viewer to see the generated PDF",Toast.LENGTH_SHORT).show();
        }
    }


    // Erstellt die Kopfzeile des PDF'S, die dem Dokument beigefügt werden kann
    class PdfHeader extends PdfPageEventHelper {
        Font timesHeader = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.ITALIC, BaseColor.GRAY);

        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            // Aktuelles Datum einfügen (links)
            Date date = new Date() ;
            String timeStamp = new SimpleDateFormat("dd.MM.yyyy").format(date);
            Phrase headerDate = new Phrase(timeStamp, timesHeader);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    headerDate,
                    (document.left() + document.leftMargin() * 2),
                    (document.top() + 15), 0);
            // App-Name einfügen (zentriert)
            Phrase headerName = new Phrase(getResources().getString(R.string.list_pdf_header_app), timesHeader);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    headerName,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.top() + 15, 0);
            // Seitenzahl einfügen (rechts)
            String page = String.valueOf(document.getPageNumber());
            Phrase headerPage = new Phrase(page, timesHeader);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    headerPage,
                    (document.right() - document.rightMargin() *2),
                    document.top() + 15, 0);
        }
    }

}
