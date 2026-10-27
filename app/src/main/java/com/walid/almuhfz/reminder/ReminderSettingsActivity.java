package com.walid.almuhfz.reminder;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.walid.almuhfz.R;
import com.walid.almuhfz.utils.ThemeManager;

import java.util.Locale;

public class ReminderSettingsActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 2001;

    private SwitchCompat switchReminderEnabled;
    private TextView tvSelectedTime;
    private Button btnSelectTime, btnBatteryOptimization, btnTestNotification, btnSaveReminder;
    private RadioGroup rgMessageStyle;
    private RadioButton rbMsgStyle0, rbMsgStyle1, rbMsgStyle2;
    private View cardTimeSettings, cardMessageSettings;

    private int selectedHour = 20;
    private int selectedMinute = 0;
    private int selectedMsgIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_settings);

        initViews();
        loadSavedSettings();
        setupListeners();
    }

    private void initViews() {
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        switchReminderEnabled = findViewById(R.id.switchReminderEnabled);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnBatteryOptimization = findViewById(R.id.btnBatteryOptimization);
        btnTestNotification = findViewById(R.id.btnTestNotification);
        btnSaveReminder = findViewById(R.id.btnSaveReminder);

        rgMessageStyle = findViewById(R.id.rgMessageStyle);
        rbMsgStyle0 = findViewById(R.id.rbMsgStyle0);
        rbMsgStyle1 = findViewById(R.id.rbMsgStyle1);
        rbMsgStyle2 = findViewById(R.id.rbMsgStyle2);

        cardTimeSettings = findViewById(R.id.cardTimeSettings);
        cardMessageSettings = findViewById(R.id.cardMessageSettings);
    }

    private void loadSavedSettings() {
        boolean enabled = ReminderManager.isReminderEnabled(this);
        switchReminderEnabled.setChecked(enabled);
        updateEnabledViewState(enabled);

        selectedHour = ReminderManager.getSavedHour(this);
        selectedMinute = ReminderManager.getSavedMinute(this);
        selectedMsgIndex = ReminderManager.getSavedMessageIndex(this);

        updateTimeDisplay(selectedHour, selectedMinute);

        if (selectedMsgIndex == 1) {
            rbMsgStyle1.setChecked(true);
        } else if (selectedMsgIndex == 2) {
            rbMsgStyle2.setChecked(true);
        } else {
            rbMsgStyle0.setChecked(true);
        }

        updateBatteryStatus();
    }

    private void updateEnabledViewState(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.4f;
        cardTimeSettings.setAlpha(alpha);
        cardMessageSettings.setAlpha(alpha);

        btnSelectTime.setEnabled(enabled);
        rbMsgStyle0.setEnabled(enabled);
        rbMsgStyle1.setEnabled(enabled);
        rbMsgStyle2.setEnabled(enabled);
    }

    private void updateTimeDisplay(int hour, int minute) {
        int displayHour = (hour == 0 || hour == 12) ? 12 : (hour % 12);
        String amPm = (hour >= 12) ? "مساءً" : "صباحاً";
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm);
        tvSelectedTime.setText(timeStr);
    }

    private void updateBatteryStatus() {
        boolean isIgnored = ReminderManager.isIgnoringBatteryOptimizations(this);
        if (isIgnored) {
            btnBatteryOptimization.setText("التطبيق مستثنى من قيود البطارية ✅");
            btnBatteryOptimization.setEnabled(false);
            btnBatteryOptimization.setAlpha(0.7f);
        } else {
            btnBatteryOptimization.setText("استثناء التطبيق من قيود البطارية 🔋");
            btnBatteryOptimization.setEnabled(true);
            btnBatteryOptimization.setAlpha(1.0f);
        }
    }

    private void setupListeners() {
        switchReminderEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> updateEnabledViewState(isChecked));

        btnSelectTime.setOnClickListener(v -> showTimePickerDialog());

        btnBatteryOptimization.setOnClickListener(v -> {
            ReminderManager.requestIgnoreBatteryOptimizations(this);
            btnBatteryOptimization.postDelayed(this::updateBatteryStatus, 1000);
        });

        btnTestNotification.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                    return;
                }
            }
            saveCurrentMsgIndex();
            ReminderManager.sendInstantTestNotification(this);
            Toast.makeText(this, "تم إرسال إشعار تجريبي الآن 🔔", Toast.LENGTH_SHORT).show();
        });

        btnSaveReminder.setOnClickListener(v -> saveReminderSettings());
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateTimeDisplay(selectedHour, selectedMinute);
                },
                selectedHour,
                selectedMinute,
                false
        );
        timePickerDialog.setTitle("اختر وقت التذكير اليومي 🕒");
        timePickerDialog.show();
    }

    private void saveCurrentMsgIndex() {
        int checkedId = rgMessageStyle.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMsgStyle1) {
            selectedMsgIndex = 1;
        } else if (checkedId == R.id.rbMsgStyle2) {
            selectedMsgIndex = 2;
        } else {
            selectedMsgIndex = 0;
        }
    }

    private void saveReminderSettings() {
        boolean enabled = switchReminderEnabled.isChecked();
        saveCurrentMsgIndex();

        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                    return;
                }
            }

            ReminderManager.scheduleDailyReminder(this, selectedHour, selectedMinute, selectedMsgIndex);
            Toast.makeText(this, "تم حفظ وتفعيل التذكير اليومي بنجاح", Toast.LENGTH_SHORT).show();
        } else {
            ReminderManager.cancelReminder(this);
            Toast.makeText(this, "تم تعطيل التذكير اليومي", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "تم منح إذن الإشعارات بنجاح", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "يلزم منح إذن الإشعارات لعمل التذكير اليومي", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBatteryStatus();
    }
}
