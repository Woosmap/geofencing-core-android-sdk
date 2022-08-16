package com.webgeoservices.samplecore;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.webgeoservices.samplecore.model.PlaceData;
import com.webgeoservices.samplecore.model.PlaceDataAdapter;
import com.webgeoservices.samplecore.model.PlaceDataComparator;
import com.webgeoservices.woosmapgeofencingcore.PositionsManagerCore;
import com.webgeoservices.woosmapgeofencingcore.WoosmapCore;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;

import java.util.ArrayList;
import java.util.Collections;

public class VisitFragment extends Fragment {

    PlaceDataAdapter adapter;
    ListView lvVisit;
    PositionsManagerCore mPositionsManager;
    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPositionsManager = new PositionsManagerCore(getContext(), WoosmapDb.getInstance(getContext()), WoosmapCore.getInstance());
        mContext = getContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate( R.layout.visit, container, false );
        lvVisit = (ListView) view.findViewById(R.id.lvVisit);
        lvVisit.setAdapter(adapter);
        return  view;
    }

    public void loadDataFromRegionLog(ArrayList<PlaceData> arrayOfPlaceData) {
        int index = lvVisit.getFirstVisiblePosition();
        View v = lvVisit.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - lvVisit.getPaddingTop());

        if(adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                PlaceData place = adapter.getItem( i );
                if (place.getType() == PlaceData.dataType.visit) {
                    arrayOfPlaceData.add( place );
                }
            }
        }

        adapter = new PlaceDataAdapter(mContext, arrayOfPlaceData);
        Collections.sort( arrayOfPlaceData, new PlaceDataComparator());

        lvVisit.setAdapter(adapter);
        lvVisit.setSelectionFromTop(index, top);
    }

    public void loadDataFromVisit(ArrayList<PlaceData> arrayOfPlaceData) {
        int index = lvVisit.getFirstVisiblePosition();
        View v = lvVisit.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - lvVisit.getPaddingTop());

        if(adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                PlaceData place = adapter.getItem( i );
                if (place.getType() == PlaceData.dataType.regionLog) {
                    arrayOfPlaceData.add( place );
                }
            }
        }

        adapter = new PlaceDataAdapter(mContext, arrayOfPlaceData);
        Collections.sort( arrayOfPlaceData, new PlaceDataComparator());

        lvVisit.setAdapter(adapter);
        lvVisit.setSelectionFromTop(index, top);
    }


    public void loadData(ArrayList<PlaceData> arrayOfPlaceData) {
        int index = lvVisit.getFirstVisiblePosition();
        View v = lvVisit.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - lvVisit.getPaddingTop());

        adapter = new PlaceDataAdapter(mContext, arrayOfPlaceData);
        Collections.sort( arrayOfPlaceData, new PlaceDataComparator());

        lvVisit.setAdapter(adapter);
        lvVisit.setSelectionFromTop(index, top);
    }

    public void clearData() {
        adapter.clear();
    }


}
