package com.example.lukas.bluetoothtest;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

public class TripDetailFragment extends Fragment {
    private TextView tv_detail_driver, tv_detail_mode, tv_detail_startTs,
            tv_detail_endTs, tv_detail_startMil, tv_detail_endMil,
            tv_detail_startAdd, tv_detail_endAdd;

    private static final int col_driver = 1, col_mode = 2, col_startMil = 3,
            col_endMil = 4, col_startTs = 5, col_endTs = 6,
            col_startAdd = 7, col_endAdd = 8;

    public static TripDetailFragment newInstance(Context context, int position, long rowid) {
        TripDetailFragment detailFragment = new TripDetailFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putLong("rowid", rowid);
        // Daten des Trips aus DB lesen
        if(rowid != 0) {
            final Cursor cursor;
            cursor = context.getContentResolver().query(TripProvider.CONTENT_URI, null, "_id=?", new String[]{Long.toString(rowid)}, null);
            if (!cursor.moveToFirst()) {
                Log.e(TripDetailFragment.class.getName(), "Result set is empty");
                return null;
            }
            args.putString("driver", cursor.getString(col_driver));
            args.putString("mode", cursor.getString(col_mode));
            args.putString("startTs", TripsAdapter.convertDate(cursor.getLong(col_startTs)));
            args.putString("endTs", TripsAdapter.convertDate(cursor.getLong(col_endTs)));
            args.putString("startMil", cursor.getString(col_startMil));
            args.putString("endMil", cursor.getString(col_endMil));
            args.putString("startAdd", cursor.getString(col_startAdd));
            args.putString("endAdd", cursor.getString(col_endAdd));
        }

        detailFragment.setArguments(args);
        return detailFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.activity_trip_detail, container, false);

        // Referenzvariablen zu den Feldern deklarieren
        tv_detail_driver = (TextView) v.findViewById(R.id.tv_detail_driver);
        tv_detail_mode = (TextView) v.findViewById(R.id.tv_detail_mode);
        tv_detail_startTs = (TextView) v.findViewById(R.id.tv_detail_startTs);
        tv_detail_endTs = (TextView) v.findViewById(R.id.tv_detail_endTs);
        tv_detail_startMil = (TextView) v.findViewById(R.id.tv_detail_startMil);
        tv_detail_endMil = (TextView) v.findViewById(R.id.tv_detail_endMil);
        tv_detail_startAdd = (TextView) v.findViewById(R.id.tv_detail_startAdd);
        tv_detail_endAdd = (TextView) v.findViewById(R.id.tv_detail_endAdd);

        // TextViews mit den Werten füllen
        tv_detail_driver.setText(getArguments().getString("driver"));
        tv_detail_mode.setText(getArguments().getString("mode"));
        tv_detail_startTs.setText(getArguments().getString("startTs"));
        tv_detail_endTs.setText(getArguments().getString("endTs"));
        tv_detail_startMil.setText(getArguments().getString("startMil"));
        tv_detail_endMil.setText(getArguments().getString("endMil"));
        tv_detail_startAdd.setText(getArguments().getString("startAdd"));
        tv_detail_endAdd.setText(getArguments().getString("endAdd"));

        return v;
    }

    public int getShownIndex() {
        return getArguments().getInt("position", 0);
    }
}
