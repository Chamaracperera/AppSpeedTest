package com.example.appspeedtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    Spinner serviceSpinner;
    TextView ispText;
    Button startTestBtn, generalTestBtn;
    RadioGroup testTypeGroup;

    HashMap<String, String> serviceDomains = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceSpinner = findViewById(R.id.serviceSpinner);
        ispText = findViewById(R.id.ispText);
        startTestBtn = findViewById(R.id.startTestBtn);
        generalTestBtn = findViewById(R.id.generalTestBtn);
        testTypeGroup = findViewById(R.id.testTypeGroup);

        // Load ISP and network type
        String networkInfo = Utils.getISPName(this) + " (" + getNetworkType() + ")";
        ispText.setText("Current ISP: " + networkInfo);

        // Populate service list
        String[] services = {"WhatsApp", "Facebook", "TikTok", "YouTube", "Instagram", "Twitter"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, services);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceSpinner.setAdapter(adapter);

        // Service domains - using CDN domains for more accurate testing
        serviceDomains.put("WhatsApp", "web.whatsapp.com");
        serviceDomains.put("Facebook", "scontent.xx.fbcdn.net");
        serviceDomains.put("TikTok", "v16m.tiktokcdn.com");
        serviceDomains.put("YouTube", "googlevideo.com");
        serviceDomains.put("Instagram", "scontent.cdninstagram.com");
        serviceDomains.put("Twitter", "pbs.twimg.com");

        // Test type radio group listener
        testTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioContentTest) {
                serviceSpinner.setVisibility(View.VISIBLE);
                startTestBtn.setVisibility(View.VISIBLE);
                generalTestBtn.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioGeneralTest) {
                serviceSpinner.setVisibility(View.GONE);
                startTestBtn.setVisibility(View.GONE);
                generalTestBtn.setVisibility(View.VISIBLE);
            }
        });

        // Content-specific test button
        startTestBtn.setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                Toast.makeText(MainActivity.this,
                        "No network connection available!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedService = serviceSpinner.getSelectedItem().toString();
            String domain = serviceDomains.get(selectedService);

            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra("testType", "content");
            intent.putExtra("service", selectedService);
            intent.putExtra("domain", domain);
            startActivity(intent);
        });

        // General mobile data test button
        generalTestBtn.setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                Toast.makeText(MainActivity.this,
                        "No network connection available!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra("testType", "general");
            intent.putExtra("service", "Mobile Data");
            intent.putExtra("domain", "general");
            startActivity(intent);
        });
    }

    private String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WiFi";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Mobile Data";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return "Ethernet";
                }
            }
        }
        return "Unknown";
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }
}