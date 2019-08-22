package com.example.snapchatclone;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    private static final int NUM_PAGES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager pager = findViewById(R.id.pager);
        FragmentStatePagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        Log.i(TAG,"onCreate!");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"onPause!");
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch(i) {
                case 0:
                    return new CameraFragment();
                case 1:
                    return new FollowerFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
