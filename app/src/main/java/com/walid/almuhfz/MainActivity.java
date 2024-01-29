package com.walid.almuhfz;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.walid.almuhfz.learning.LearningActivity;
import com.walid.almuhfz.learning.models.LearningData;
import com.walid.almuhfz.learning.models.Reciter;
import com.walid.almuhfz.learning.models.ReciterResponse;
import com.walid.almuhfz.learning.models.Sora;
import com.walid.almuhfz.learning.models.SoraDetails;
import com.walid.almuhfz.learning.models.SoraDetailsResponse;
import com.walid.almuhfz.learning.networking.QuranUtils;
import com.walid.almuhfz.learning.networking.RetrofitClient;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import guy4444.smartrate.SmartRate;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import softpro.naseemali.ShapedNavigationView;
import softpro.naseemali.ShapedViewSettings;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    private final List<Sora> soraList = QuranUtils.getSoraList();
    ImageView rateee;
    List<Reciter> reciterList;
    SoraDetails soraDetails;
    TextView reciterTextView, soraTextView, startLearn, fromAyaTextView, repeatSoraTextView, repeatAyaTextView, toAyaTextView;
    int lastReciter = 0;
    Reciter selectedReciter;
    int selectedSoraNumber = 1;
    int selectedFrom = 1;
    int selectedTo = 1;
    int repeatAya = 1;
    int repeatSora = 1;
    private FirebaseAnalytics mFirebaseAnalytics;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainbar);

        initViews();
        setListeners();
        getListOfReciters();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        ShapedNavigationView shapedNavigationView = findViewById(R.id.nav_view);
        shapedNavigationView.getSettings().setShapeType(ShapedViewSettings.ROUNDED_RECT);
        shapedNavigationView.setNavigationItemSelectedListener(this);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (this != null && !isFinishing()) {
                    SmartRate.Rate(MainActivity.this
                            , "تقييم التطبيق"
                            , "تقييمك للتطبيق يساعدنا علي التطوير المستمر وتقديم المزيد"
                            , "تقييم الان"
                            , "حسنا يمكنك تقيممنا الان علي جوجل بلاي"
                            , "اضغط هنا"
                            , "ليس الان"
                            , "Thanks "
                            , Color.parseColor("#305A23"), 2);


                }
            }
        }, 50000);


        if (!isNetworkConnected()) {
            Toast.makeText(this, "من فضلك تأكد من اتصالك بالإنترنت لتشغيل المصحف المعلم", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
        }

        FirebaseMessaging.getInstance().subscribeToTopic("all");

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);


    }



    private void setListeners() {
        reciterTextView.setOnClickListener(view -> {
            selectReciter();
        });

        soraTextView.setOnClickListener(view -> {
            selectSora();
        });

        fromAyaTextView.setOnClickListener(view -> {
            selectFromAya();
        });

        toAyaTextView.setOnClickListener(view -> {
            selectToAya();
        });

        repeatAyaTextView.setOnClickListener(view -> {
            selectAyaRepeat();
        });

        repeatSoraTextView.setOnClickListener(view -> {
            selectSoraRepeat();
        });

        startLearn.setOnClickListener(view -> {
            Intent intent = new Intent(this, LearningActivity.class);
            LearningData data = new LearningData(
                    selectedReciter,
                    soraDetails,
                    selectedSoraNumber,
                    selectedFrom,
                    selectedTo,
                    repeatAya,
                    repeatSora
            );
            intent.putExtra("learning_data", data);
            startActivity(intent);
        });

        rateee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.walid.almuhfz")));
            }
        });

    }


    private void initViews() {
        reciterTextView = findViewById(R.id.reciter_text_view);
        soraTextView = findViewById(R.id.sora_text_view);
        fromAyaTextView = findViewById(R.id.fromAyaTextView);
        toAyaTextView = findViewById(R.id.toAyaTextView);
        repeatAyaTextView = findViewById(R.id.repeatAyaTextView);
        repeatSoraTextView = findViewById(R.id.repeatSoraTextView);
        startLearn = findViewById(R.id.startLearn);
        rateee = findViewById(R.id.rating);
    }

    private void getListOfReciters() {

        RetrofitClient.getInstance().getApi().getListOfReciter().enqueue(new Callback<ReciterResponse>() {
            @Override
            public void onResponse(Call<ReciterResponse> call, Response<ReciterResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, ": size = " + response.body().getData().size());
                    reciterList = response.body().getData();
                    reciterTextView.setText(reciterList.get(0).getName());
                    selectedReciter = reciterList.get(0);
                    getSoraDetails();
                } else {
                    Log.d(TAG, ": " + response.message());
                    Log.d(TAG, ": " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ReciterResponse> call, Throwable t) {
                Log.d(TAG, ": " + t.getLocalizedMessage());
            }
        });

    }

    private void getSoraDetails() {
        startLearn.setVisibility(View.GONE);
        RetrofitClient.getInstance().getApi().getSoraDetails(selectedSoraNumber, selectedReciter.getIdentifier())
                .enqueue(new Callback<SoraDetailsResponse>() {
                    @Override
                    public void onResponse(Call<SoraDetailsResponse> call, Response<SoraDetailsResponse> response) {
                        if (response.isSuccessful()) {
                            soraDetails = response.body().getData();
                            soraTextView.setText(soraDetails.getName());
                            toAyaTextView.setText(String.valueOf(soraDetails.getAyahs().size()));
                            selectedTo = soraDetails.getAyahs().size();
                            fromAyaTextView.setText("1");
                            startLearn.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<SoraDetailsResponse> call, Throwable t) {
                    }
                });
    }

    private void selectReciter() {
        if (reciterList == null) return;
        CharSequence[] items = new CharSequence[reciterList.size()];
        for (int i = 0; i <= reciterList.size() - 1; i++) {
            items[i] = reciterList.get(i).getName();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, lastReciter, (d, n) -> {
            selectedReciter = reciterList.get(n);
            reciterTextView.setText(reciterList.get(n).getName());
            lastReciter = n;
            getSoraDetails();
            d.dismiss();
        });
        adb.setNegativeButton("الغاء", null);
        adb.setTitle("اختر القارئ");
        adb.show();
    }


    private void selectSora() {

        CharSequence[] items = new CharSequence[114];
        for (int i = 0; i <= 113; i++) {
            items[i] = soraList.get(i).getName();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, 0, (d, n) -> {
            Log.d(TAG, "onClick: " + 2);
            String name = soraList.get(n).getName();
            soraTextView.setText(name);
            selectedSoraNumber = n + 1;
            getSoraDetails();
            d.dismiss();
        });
        adb.setNegativeButton("الغاء", null);
        adb.setTitle("اختر سورة");
        adb.show();

    }

    private void selectFromAya() {
        NumberFormat nf = NumberFormat.getInstance(new Locale("ar", "EG"));
        CharSequence[] items = new CharSequence[soraDetails.getAyahs().size()];
        for (int i = 0; i <= soraDetails.getAyahs().size() - 1; i++) {

            items[i] = nf.format(i + 1) + " = " + soraDetails.getAyahs().get(i).getText();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, selectedFrom - 1, (d, n) -> {
            selectedFrom = n + 1;

            if (selectedTo >= selectedFrom) {
                fromAyaTextView.setText(String.valueOf(n + 1));
            } else {
                Toast.makeText(this, "يجب ان يكون الايه النهائيه بعد الايه الابتدائيه", Toast.LENGTH_LONG).show();
            }

            d.dismiss();
        });
        adb.setNegativeButton("الغاء", null);
        adb.setTitle("من الايه");
        adb.show();

    }

    private void selectToAya() {
        NumberFormat nf = NumberFormat.getInstance(new Locale("ar", "EG"));
        CharSequence[] items = new CharSequence[soraDetails.getAyahs().size()];
        for (int i = 0; i <= soraDetails.getAyahs().size() - 1; i++) {

            items[i] = nf.format(i + 1) + " = " + soraDetails.getAyahs().get(i).getText();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, selectedTo - 1, (d, n) -> {
            selectedTo = n + 1;


            if (selectedTo >= selectedFrom) {
                toAyaTextView.setText(String.valueOf(n + 1));
            } else {
                Toast.makeText(this, "يجب ان يكون الايه النهائيه بعد الايه الابتدائيه", Toast.LENGTH_LONG).show();
            }
            d.dismiss();
        });
        adb.setNegativeButton("الغاء", null);
        adb.setTitle("الى الايه");
        adb.show();

    }

    private void selectAyaRepeat() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        edittext.setGravity(Gravity.CENTER);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);

        edittext.setHint("ادخل رقم التكرار");
        alert.setTitle("عدد تكرار الآيه الواحده");

        alert.setView(edittext);

        alert.setPositiveButton("تم", (dialog, whichButton) -> {
            String textValue = edittext.getText().toString();
            if (textValue.equals("") || textValue.equals("0") || Integer.parseInt(textValue) > 604) {
                Toast.makeText(this, "برجاء اختيار رقم صفحة صالح", Toast.LENGTH_SHORT).show();
            } else {
                repeatAya = Integer.parseInt(textValue);
                repeatAyaTextView.setText("عدد التكرار لكل آيه" + " = " + textValue);
            }
        });

        alert.setNegativeButton("الغاء", (dialog, whichButton) -> {
            dialog.dismiss();
            // what ever you want to do with No option.
        });

        alert.show();
    }

    private void selectSoraRepeat() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        edittext.setGravity(Gravity.CENTER);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);

        edittext.setHint("ادخل رقم التكرار");
        alert.setTitle("عدد تكرار السورة");

        alert.setView(edittext);

        alert.setPositiveButton("تم", (dialog, whichButton) -> {
            String textValue = edittext.getText().toString();
            if (textValue.equals("") || textValue.equals("0") || Integer.parseInt(textValue) > 604) {
                Toast.makeText(this, "برجاء اختيار رقم صفحة صالح", Toast.LENGTH_SHORT).show();
            } else {
                repeatSora = Integer.parseInt(textValue);
                repeatSoraTextView.setText("عدد التكرار " + " = " + textValue);
            }
        });

        alert.setNegativeButton("الغاء", (dialog, whichButton) -> {
            dialog.dismiss();
            // what ever you want to do with No option.
        });

        alert.show();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.maktbtiplus) {
            Intent o = new Intent(Intent.ACTION_VIEW);
            o.setData(Uri.parse("https://walid-fekry.com/maktbti-plus/"));
            startActivity(o);
        } else if (id == R.id.facegroub) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/groups/440403217380641")));
        } else if (id == R.id.teleee) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/App_Maktbti")));
        }else if (id == R.id.ayak) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.co/2xVh8rqVLS")));
        } else if (id == R.id.ratee) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.walid.almuhfz")));
        } else if (id == R.id.shareee) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "المٌحفظ للقرآن - المصحف المعلم 💙");
            sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.aboutapp) + "\n" +
                    "\n" +
                    "حمل التطبيق من خلال هذا الرابط أو من متجر جوجل بلاي \uD83D\uDC47  https://play.google.com/store/apps/details?id=com.walid.almuhfz \n");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else if (id == R.id.mass) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "prowalidfekry@gmail.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "رسالة إلى مبرمج تطبيق المٌحفظ 📲");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "");
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        } else if (id == R.id.sysa) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/almuhfz/home")));

        } else if (id == R.id.walidmore) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.co/US34fsUZeW")));
        }


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;


    }


}