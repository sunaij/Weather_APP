package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.weatherapp.models.Weather;
import com.example.weatherapp.parser.JSONWeatherParser;
import com.example.weatherapp.service.WeatherService;

import org.json.JSONException;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mCityNameText;
    private TextView mCondDescrTxt;
    private TextView mTempTxt;
    private TextView mPressTxt;
    private TextView mWindSpeedTxt;
    private TextView mWindDegTxt;

    private TextView mHumTxt;

    public static final String INTENT_ACTION = "com.weather.broadcast";
    public static final String ACTION_ID = "5ad7218f2e11df834b0eaf3a33a39d2a";
    private static final int REPEAT_HOUR = 2 ;


    //BroadcastReceiver listening to the broadcast from job scheduler
    private BroadcastReceiver weatherBroadCast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //schedule the job each 2 hrs
            JSONWeatherTask task = new JSONWeatherTask();
            task.execute(new String[]{ACTION_ID});
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCityNameText = findViewById(R.id.cityText);
        mCondDescrTxt = findViewById(R.id.condDescr);
        mTempTxt = findViewById(R.id.temp);
        mHumTxt = findViewById(R.id.hum);
        mPressTxt = findViewById(R.id.press);
        mWindSpeedTxt = findViewById(R.id.windSpeed);
        mWindDegTxt = findViewById(R.id.windDeg);

        IntentFilter inf = new IntentFilter();
        inf.addAction(MainActivity.INTENT_ACTION);

        //Register the broadcast
        registerReceiver(weatherBroadCast, inf);

        JSONWeatherTask task = new JSONWeatherTask();
        task.execute(new String[]{ACTION_ID});
    }

    //scheduling the jobs
    private void scheduleJob(int jobId) {
        final JobScheduler jobScheduler = (JobScheduler) getSystemService(
                Context.JOB_SCHEDULER_SERVICE);

        // The JobService that we want to run
        final ComponentName name = new ComponentName(this, WeatherService.class);

        // Schedule the job
        final int result = jobScheduler.schedule(getJobInfo(jobId, REPEAT_HOUR, name));

        // If successfully scheduled, log this thing
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Scheduled job successfully!");
        }

    }
    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        unregisterReceiver(weatherBroadCast);
        super.onStop();
    }
    private JobInfo getJobInfo(final int id, final long hour, final ComponentName name) {
        final long interval = TimeUnit.HOURS.toMillis(hour); // run every hour
        final boolean isPersistent = true; // persist through boot
        final int networkType = JobInfo.NETWORK_TYPE_ANY; // Requires some sort of connectivity

        final JobInfo jobInfo;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobInfo = new JobInfo.Builder(id, name)
                    .setMinimumLatency(interval)
                    .setRequiredNetworkType(networkType)
                    .setPersisted(isPersistent)
                    .build();
        } else {
            jobInfo = new JobInfo.Builder(id, name)
                    .setPeriodic(interval)
                    .setRequiredNetworkType(networkType)
                    .setPersisted(isPersistent)
                    .build();
        }

        return jobInfo;
    }

    /**
     * Class used to fetch the weather details
     */
    public class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

        @Override
        protected Weather doInBackground(String... params) {
            Weather weather = new Weather();
            String data = ( (new WeatherRequest()).getWeatherData(params[0]));

            try {
                weather = JSONWeatherParser.getWeather(data);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return weather;

        }

        @Override
        protected void onPostExecute(Weather weather) {
            super.onPostExecute(weather);
            // updating the UI
            mCityNameText.setText(weather.locationDetails.getCity() + "," + weather.locationDetails.getCountry());
            mCondDescrTxt.setText(weather.currentCondition.getCondition() + "(" + weather.currentCondition.getDescr() + ")");
            mTempTxt.setText("(Max: " + weather.temperature.getMaxTemp() +" ) " + "(Normal:" + weather.temperature.getTemp() +" ) " +"(Min:" + weather.temperature.getMinTemp() +" ) "  );
            mHumTxt.setText("" + weather.currentCondition.getHumidity() + "%");
            mPressTxt.setText("" + weather.currentCondition.getPressure());
            mWindSpeedTxt.setText("" + weather.wind.getSpeed());
            mWindDegTxt.setText("" + weather.wind.getDeg());

            scheduleJob((int)System.currentTimeMillis());

        }

    }

}
