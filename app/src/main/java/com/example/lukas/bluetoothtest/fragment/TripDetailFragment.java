package com.example.lukas.bluetoothtest.fragment;


import android.support.v4.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.trip.TripOpenHelper;
import com.example.lukas.bluetoothtest.trip.TripProvider;
import com.example.lukas.bluetoothtest.trip.TripsAdapter;
import com.google.android.gms.maps.GoogleMap;

public class TripDetailFragment extends Fragment  {

    private TextView tv_detail_driver, tv_detail_mode, tv_detail_startTs,
            tv_detail_endTs, tv_detail_startMil, tv_detail_endMil,
            tv_detail_startAdd, tv_detail_endAdd, tv_detail_reason;


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
            args.putString(TripOpenHelper.COL_DRIVER_NAME, cursor.getString(TripOpenHelper.COL_ID_DRIVER));
            args.putString(TripOpenHelper.COL_DRIVE_MODE, cursor.getString(TripOpenHelper.COL_ID_MODE));
            args.putString(TripOpenHelper.COL_TS_START, TripsAdapter.convertDate(cursor.getLong(TripOpenHelper.COL_ID_STARTTS)));
            args.putString(TripOpenHelper.COL_TS_END, TripsAdapter.convertDate(cursor.getLong(TripOpenHelper.COL_ID_ENDTS)));
            args.putString(TripOpenHelper.COL_MILEAGE_START, cursor.getString(TripOpenHelper.COL_ID_STARTMIL));
            args.putString(TripOpenHelper.COL_MILEAGE_END, cursor.getString(TripOpenHelper.COL_ID_ENDMIL));
            args.putString(TripOpenHelper.COL_ADDRESS_START, cursor.getString(TripOpenHelper.COL_ID_STARTADD));
            args.putString(TripOpenHelper.COL_ADDRESS_END, cursor.getString(TripOpenHelper.COL_ID_ENDADD));
            args.putString(TripOpenHelper.COL_ROUTE_POINTS, cursor.getString(TripOpenHelper.COL_ID_ROUTE));
            args.putString(TripOpenHelper.COL_REASON, cursor.getString(TripOpenHelper.COL_ID_REASON));
        }

        detailFragment.setArguments(args);
        return detailFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.fragment_trip_detail, container, false);

        // Referenzvariablen zu den Feldern deklarieren
        tv_detail_driver = (TextView) v.findViewById(R.id.tv_detail_driver);
        tv_detail_mode = (TextView) v.findViewById(R.id.tv_detail_mode);
        tv_detail_reason = (TextView) v.findViewById(R.id.tv_detail_reason);
        tv_detail_startTs = (TextView) v.findViewById(R.id.tv_detail_startTs);
        tv_detail_endTs = (TextView) v.findViewById(R.id.tv_detail_endTs);
        tv_detail_startMil = (TextView) v.findViewById(R.id.tv_detail_startMil);
        tv_detail_endMil = (TextView) v.findViewById(R.id.tv_detail_endMil);
        tv_detail_startAdd = (TextView) v.findViewById(R.id.tv_detail_startAdd);
        tv_detail_endAdd = (TextView) v.findViewById(R.id.tv_detail_endAdd);


        // TextViews mit den Werten füllen
        tv_detail_driver.setText(getArguments().getString(TripOpenHelper.COL_DRIVER_NAME));
        tv_detail_mode.setText(getArguments().getString(TripOpenHelper.COL_DRIVE_MODE));
        tv_detail_startTs.setText(getArguments().getString(TripOpenHelper.COL_TS_START));
        tv_detail_endTs.setText(getArguments().getString(TripOpenHelper.COL_TS_END));
        tv_detail_startMil.setText(getArguments().getString(TripOpenHelper.COL_MILEAGE_START));
        tv_detail_endMil.setText(getArguments().getString(TripOpenHelper.COL_MILEAGE_END));
        tv_detail_startAdd.setText(getArguments().getString(TripOpenHelper.COL_ADDRESS_START));
        tv_detail_endAdd.setText(getArguments().getString(TripOpenHelper.COL_ADDRESS_END));
        tv_detail_reason.setText(getArguments().getString(TripOpenHelper.COL_REASON));

        return v;
    }

    public int getShownIndex() {
        return getArguments().getInt("position", 0);
    }

}
