package com.grizzhacks.dartreader;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Build;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MainActivity extends AppCompatActivity {

    private AnimationDrawable animationDrawable;

    // Credit to this StackOverflow post for the NFC boilerplate code
    // https://stackoverflow.com/questions/5546932/how-to-read-and-write-android-nfc-tags

    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mAdapter == null) {
            // TODO: Show an error message if device does not support NFC
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
                this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.constraintLayout);

        // TODO: Animated gradient is currently broken
        // animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        // animationDrawable.setEnterFadeDuration(3000);
        // animationDrawable.setExitFadeDuration(2000);



    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);

        if (animationDrawable != null && !animationDrawable.isRunning()) {
            animationDrawable.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);

            if (animationDrawable != null && animationDrawable.isRunning()) {
                animationDrawable.stop();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {
            final MifareUltralight ultralight = MifareUltralight.get(tag);

            if (ultralight != null) {

                // You can only run one of these. You can't open two connections simultaneously.

                // WARNING: The writeTag() method will overwrite activation information.
                // Do not use on a DART pass you wish to keep.

                // writeTagInfo(ultralight);

                DartCard dartCard = getTagInfo(ultralight);


                TextView txtCardNumber = findViewById(R.id.txtCardNumber);
                TextView txtValidFrom = findViewById(R.id.txtValidFrom);
                TextView txtValidThru = findViewById(R.id.txtValidThru);
                TextView txtDaysLeft = findViewById(R.id.txtDaysLeft);

                txtCardNumber.setText(dartCard.cardNumber);
                txtValidFrom.setText("Valid From: " + dartCard.activationDateString);
                txtValidThru.setText("Valid Thru: " + dartCard.dateValidThru);
                txtDaysLeft.setText("This Card Expires in " + dartCard.dateDaysLeft + " Days");
            }
        }
    }

    private void writeTagInfo(final MifareUltralight ultralight) {
        Thread thread = new Thread(
                new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {
                        try {
                            ultralight.connect();

                            // Test writing data to a userdata page on the NFC tag
                            ultralight.writePage(3, "test".getBytes(StandardCharsets.UTF_8));

                            ultralight.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        thread.start();
    }

    private DartCard getTagInfo(final MifareUltralight ultralight) {

        // TODO: Mock data. This information should be on the card
        final DartCard dartCard = new DartCard();
        dartCard.cardNumber = "0100013400";
        dartCard.activationDateString = "20200901";

        Thread thread = new Thread(
                new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {
                        try {
                            ultralight.connect();
                            byte[] byteArray = ultralight.readPages(3);
                            Log.d("DATA: ", new String(byteArray, StandardCharsets.UTF_8));
                            ultralight.close();

                            DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
                            LocalDate activationDate = LocalDate.parse(
                                    dartCard.activationDateString, formatter);

                            LocalDate expirationDate = activationDate.plusDays(31);
                            dartCard.dateValidThru = expirationDate.toString();

                            LocalDate localDate = LocalDate.now();

                            long daysLeft = localDate.until(expirationDate, ChronoUnit.DAYS);
                            dartCard.dateDaysLeft = String.valueOf(daysLeft);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        thread.start();
        return dartCard;
    }

    static class DartCard {
        String cardNumber = "0000000000";
        String activationDateString = "2020-03-11";
        String dateValidThru = "";
        String dateDaysLeft = "";
    }
}