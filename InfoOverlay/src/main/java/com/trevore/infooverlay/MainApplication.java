package com.trevore.infooverlay;

import android.app.Application;
import android.content.Context;

import dagger.ObjectGraph;

/**
 * Created by trevor on 6/11/14.
 */
public class MainApplication extends Application {

    private ObjectGraph objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        objectGraph = ObjectGraph.create(new BaseModule(this));
    }

    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    public static MainApplication from(Context context) {
        return (MainApplication) context.getApplicationContext();
    }
}
