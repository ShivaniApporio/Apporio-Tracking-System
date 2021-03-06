package com.apporioinfolabs.ats_sdk;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.apporioinfolabs.ats_sdk.di.DaggerMainComponent;
import com.apporioinfolabs.ats_sdk.di.MainComponent;
import com.apporioinfolabs.ats_sdk.di.MainModule;
import com.apporioinfolabs.ats_sdk.managers.AtsLocationManager;
import com.apporioinfolabs.ats_sdk.managers.DatabaseManager;
import com.apporioinfolabs.ats_sdk.managers.NotificationManager;
import com.apporioinfolabs.ats_sdk.managers.SharedPrefrencesManager;
import com.apporioinfolabs.ats_sdk.managers.SocketManager;
import com.apporioinfolabs.ats_sdk.models.ModelLocation;
import com.apporioinfolabs.ats_sdk.utils.ATSConstants;
import com.apporioinfolabs.ats_sdk.utils.AppUtils;
import com.apporioinfolabs.ats_sdk.utils.LOGS;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

public abstract class AtsLocationServiceClass  extends Service {

    private static final String TAG = "AtsLocationServiceClass";
    MainComponent mainComponent ;
    private Timer mTimer = null;


//    @Inject SocketManager socketManager;
    @Inject SharedPrefrencesManager sharedPrefrencesManager;
    @Inject NotificationManager notificationManager;
    @Inject AtsLocationManager atsLocationManager ;
    @Inject DatabaseManager databaseManager ;



    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);
        if(ATS.mBuilder.AppId.equals("NA")){
            Toast.makeText(this, "Found No Valid App ID: ", Toast.LENGTH_SHORT).show();
        }else{
            mainComponent = DaggerMainComponent.builder().mainModule(new MainModule(this)).build();
            mainComponent.inject(this);

            if (mTimer != null) { mTimer.cancel(); }
            else { mTimer = new Timer(); }

            // starting timer if fetch location while driver is standing on one place (i.e. location not fetching on interval )
            if(ATS.mBuilder.FetchLocationWhenVehicleIsStopped){ mTimer.scheduleAtFixedRate(new FetchLocationOnStandingTask(), 0, ATS.mBuilder.LocationInterval); }

            super.onCreate();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGS.d(TAG , "SDK Service is started.");
        try {
            notificationManager.startNotificationView(this);

        }
        catch (Exception e){ LOGS.e(TAG, ""+e.getMessage()); }
        atsLocationManager.startLocationUpdates();
        return START_NOT_STICKY;
    }


    @Override
    public void onStart(Intent intent, int startId) { super.onStart(intent, startId); }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String socketConnectivity){
        if(socketConnectivity.equals(""+ SocketManager.SOCKET_CONNECTED)){
            notificationManager.updateNotificationSocketConnectivity(socketConnectivity);
        }if(socketConnectivity.equals(""+SocketManager.SOCKET_DISCONNECTED)){
            notificationManager.updateNotificationSocketConnectivity(socketConnectivity);
        }if(socketConnectivity.equals(""+SocketManager.LOG_STASH_UPDATED)){
            String locationString = sharedPrefrencesManager.getLastSavedLocationObject().getLatitude()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getLongitude()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getAccuracy()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getBearing()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getTime();
            locationString = AppUtils.getLocationString(locationString)+
                    " SQL Location stash:"+(databaseManager.getAllLogsFromTable().size() + 1)+
                    " Battery: "+ATS.mBatteryLevel +
                    " State: "+(ATS.app_foreground ? "Foreground":"Background");
            notificationManager.updateRunningNotificationView(""+locationString+
                    " Battery: "+ATS.mBatteryLevel+
                    " State: "+(ATS.app_foreground?"Foreground":"Background")+
                    " Tag: "+sharedPrefrencesManager.fetchData(ATSConstants.KEYS.TAG),false);
        }if(socketConnectivity.equals(""+SocketManager.TAG_UPDATED)){
            String locationString = sharedPrefrencesManager.getLastSavedLocationObject().getLatitude()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getLongitude()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getAccuracy()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getBearing()+"_"+sharedPrefrencesManager.getLastSavedLocationObject().getTime();
            locationString = AppUtils.getLocationString(locationString)+
                    " SQL Location stash:"+(databaseManager.getAllLogsFromTable().size() + 1)+
                    " Battery: "+ATS.mBatteryLevel +
                    " State: "+(ATS.app_foreground ? "Foreground":"Background");
            notificationManager.updateRunningNotificationView(""+locationString+
                    " Battery: "+ATS.mBatteryLevel+
                    " State: "+(ATS.app_foreground?"Foreground":"Background")+
                    " Tag: "+sharedPrefrencesManager.fetchData(ATSConstants.KEYS.TAG),false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(Location location){
        onReceiveLocation(location);
    }



    public abstract void onReceiveLocation(Location location);


    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    class FetchLocationOnStandingTask extends TimerTask {
        @Override
        public void run() {
            Location location = sharedPrefrencesManager.getLastSavedLocationObject();

            if(location != null){ onReceiveLocation(location); }
            else{ LOGS.e(TAG, "Getting Null Location"); }

            notificationManager.updateRunningNotificationView("SOME data according to time goes here : "+System.currentTimeMillis(), false);

        }
    }

}
