package com.trevore.infooverlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by trevor on 6/11/14.
 */
@Module(
        injects = {
                MainActivity.MainFragment.class,
                OverlayService.class
        }
)
public class BaseModule {

    private Context context;

    public BaseModule(Context context) {
        this.context = context.getApplicationContext();
    }

    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
