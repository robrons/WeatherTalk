package com.robinronson.android.weathertalk;

import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.Voice;
import com.robinronson.android.weathertalk.utilities.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String COGNITO_POOL_ID = "us-west-2:0baf8b7e-1efb-470e-9b3c-37840002bb81";

    private static final Regions MY_REGION = Regions.US_WEST_2;

    CognitoCachingCredentialsProvider credentialsProvider;

    private AmazonPollyPresigningClient client;

    private List<Voice> voices;

    MediaPlayer mediaPlayer;

    String speech;


    EditText search_edit;
    TextView  temperature;
    TextView weather_desc;
    TextView weather_minmax;
    ImageView imageView;
    ImageButton alexa;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        search_edit = (EditText) findViewById(R.id.editText);
        temperature = (TextView) findViewById(R.id.temp_text);
        weather_desc = (TextView) findViewById(R.id.weather_description);
        weather_minmax = (TextView) findViewById(R.id.textView2);
        initPollyClient();
        new GetPollyVoices().execute();
        alexa = (ImageButton) findViewById(R.id.alexaButton);
        alexa.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest
                        = new SynthesizeSpeechPresignRequest()
                        .withText(speech)
                        .withVoiceId(voices.get(0).getId())
                        .withOutputFormat(OutputFormat.Mp3);
                URL presignedSynthesizeSpeechUrl = client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

                Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    // Set media player's data source to previously obtained URL.
                    mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
                }

                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }
                });

            }
                                 });
        imageView = (ImageView) findViewById(R.id.image_weather);
        imageView.setImageResource(R.drawable.ic_clear);
        search_edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    makeSearchQuery();
                  search_edit.getText().clear();
                }
                return false;
            }
        });


    }

    private class GetPollyVoices extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... params) {
            if (voices != null){
                return null;
            }

            DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
            DescribeVoicesResult describeVoicesResult;

            try{
                describeVoicesResult = client.describeVoices(describeVoicesRequest);
            } catch (RuntimeException e){
                Log.e(TAG, "Unable to get available voices. " + e.getMessage());
                return  null;
            }

            voices = describeVoicesResult.getVoices();

            Log.i(TAG,"Available Polly voices: " + voices);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (voices == null){
                return;
            }
        }
    }

    void initPollyClient() {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        client = new AmazonPollyPresigningClient(credentialsProvider);
    }



    public class WeatherQueryTask extends AsyncTask<URL, Void, String>{

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];
            String weatherSearchResults = null;
            try {
                weatherSearchResults = NetworkUtils.getResponceFromHttpURL(searchUrl);
            } catch (IOException e){
                e.printStackTrace();
            }
            return weatherSearchResults;
        }

        @Override
        protected void onPostExecute(String weatherSearchResults) {
            super.onPostExecute(weatherSearchResults);
            if(weatherSearchResults != null && !weatherSearchResults.equals("")){
                try {
                    speech = "Here in ";
                    JSONObject parentObject = new JSONObject(weatherSearchResults);
                    JSONArray weather = parentObject.getJSONArray("weather");
                    JSONObject main0 = weather.getJSONObject(0);
                    JSONObject sys = parentObject.getJSONObject("sys");
                    search_edit.append("  "+parentObject.getString("name")+", "+sys.getString("country"));
                    JSONObject main = parentObject.getJSONObject("main");
                    temperature.setText(main.getString("temp").substring(0,4)+"\u2103");
                    weather_desc.setText(main0.getString("main"));
                    weather_minmax.setText("Day " + main.getString("temp_max") + "°⬆ • Night " + main.getString("temp_min")+"°⬇");
                    String dt = parentObject.getString("dt");
                    String sr = sys.getString("sunrise");
                    String ss = sys.getString("sunset");
                    if(dt.compareTo(ss) < 0 && dt.compareTo(sr) > 0){

                    } else {
                    }
                    speech = speech.concat(parentObject.getString("name"));
                    speech = speech.concat(" the temperature is ");
                    speech = speech.concat(main.getString("temp") + " degree celcius");
                    speech = speech.concat("and is " + main0.getString("description"));

                    imageView.setImageResource(R.drawable.ic_light_clouds);
                    if(main0.getString("main").compareTo("Clear")==0){
                        imageView.setImageResource(R.drawable.ic_clear);
                    } else if(main0.getString("main").compareTo("Clouds")==0 || main0.getString("main").compareTo("scattered clouds")==0){
                        imageView.setImageResource(R.drawable.ic_light_clouds);
                    } else if(main0.getString("").compareTo("broken clouds")==0){
                        imageView.setImageResource(R.drawable.ic_cloudy);
                    } else if(main0.getString("main").compareTo("Rain")==0){
                        imageView.setImageResource(R.drawable.ic_rain);
                    }else if(main0.getString("main").compareTo("rain")==0){
                        imageView.setImageResource(R.drawable.ic_light_rain);
                    }else if(main0.getString("main").compareTo("Thunderstorm")==0){
                        imageView.setImageResource(R.drawable.ic_storm);
                    }else if(main0.getString("main").compareTo("Snow")==0){
                        imageView.setImageResource(R.drawable.ic_snow);
                    }else if(main0.getString("main").compareTo("Haze")==0){
                        imageView.setImageResource(R.drawable.ic_fog);
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            } else {
            }
        }
    }



    void makeSearchQuery(){
        String searchQuery = search_edit.getText().toString();
        URL searchURL = NetworkUtils.buildUrl(searchQuery);
        new WeatherQueryTask().execute(searchURL);
    }


}
