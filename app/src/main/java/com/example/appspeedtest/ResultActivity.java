package com.example.appspeedtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    TextView resultText, pingValue, downloadValue, uploadValue, qualityRating, testInfo, jitterValue;
    ProgressBar progressBar;
    LinearLayout detailedResultsContainer, jitterContainer;
    Button retryButton, backButton;
    ImageView serviceIcon;
    String service, domain, testType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Initialize views
        resultText = findViewById(R.id.resultText);
        progressBar = findViewById(R.id.progressBar);
        detailedResultsContainer = findViewById(R.id.detailedResultsContainer);
        jitterContainer = findViewById(R.id.jitterContainer);
        pingValue = findViewById(R.id.pingValue);
        downloadValue = findViewById(R.id.downloadValue);
        uploadValue = findViewById(R.id.uploadValue);
        qualityRating = findViewById(R.id.qualityRating);
        retryButton = findViewById(R.id.retryButton);
        backButton = findViewById(R.id.backButton);
        testInfo = findViewById(R.id.testInfo);
        serviceIcon = findViewById(R.id.serviceIcon);
        jitterValue = findViewById(R.id.jitterValue);

        service = getIntent().getStringExtra("service");
        domain = getIntent().getStringExtra("domain");
        testType = getIntent().getStringExtra("testType"); // "content" or "general"

        // Set service icon based on selected service
        if ("general".equals(testType)) {
            // For general test, show network icon or hide
            if (serviceIcon != null) {
                serviceIcon.setVisibility(View.GONE);
            }
            // Show jitter for general test
            if (jitterContainer != null) {
                jitterContainer.setVisibility(View.VISIBLE);
            }
        } else {
            setServiceIcon(service);
        }

        // Show network type
        testInfo.setText("Testing on " + getNetworkType());
        testInfo.setVisibility(View.VISIBLE);

        resultText.setText("Preparing to test " + service + "...\nPlease wait...");

        // Button listeners
        backButton.setOnClickListener(v -> finish());
        retryButton.setOnClickListener(v -> {
            retryButton.setVisibility(View.GONE);
            detailedResultsContainer.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            resultText.setText("Restarting test...");
            new SpeedTestTask().execute();
        });

        // Start the test
        new SpeedTestTask().execute();
    }

    private void setServiceIcon(String service) {
        if (serviceIcon != null) {
            serviceIcon.setVisibility(View.VISIBLE);
            switch (service) {
                case "WhatsApp":
                    serviceIcon.setImageResource(R.drawable.ic_whatsapp);
                    break;
                case "Facebook":
                    serviceIcon.setImageResource(R.drawable.ic_facebook);
                    break;
                case "TikTok":
                    serviceIcon.setImageResource(R.drawable.ic_tiktok);
                    break;
                case "YouTube":
                    serviceIcon.setImageResource(R.drawable.ic_youtube);
                    break;
            }
        }
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
        return "Unknown Network";
    }

    private class SpeedTestTask extends AsyncTask<Void, TestProgress, TestResult> {

        @Override
        protected TestResult doInBackground(Void... voids) {
            SpeedTestManager stm = new SpeedTestManager();
            TestResult result = new TestResult();

            boolean isGeneralTest = "general".equals(testType);
            String testDomain = isGeneralTest ? "speed.cloudflare.com" : domain;

            try {
                // Test ping/latency
                publishProgress(new TestProgress("ping", "Testing connection latency..."));
                Thread.sleep(500);

                if (isGeneralTest) {
                    // For general test, get detailed latency info
                    SpeedTestManager.LatencyResult latency = stm.testLatency(testDomain);
                    result.ping = latency.average;
                    result.jitter = latency.jitter;
                    publishProgress(new TestProgress("ping", result.ping + " ms", result.ping));
                } else {
                    result.ping = stm.getPing(testDomain);
                    publishProgress(new TestProgress("ping", result.ping + " ms", result.ping));
                }

                // Test download speed
                publishProgress(new TestProgress("download", "Testing download speed..."));
                Thread.sleep(500);

                if (isGeneralTest) {
                    // Use general test method with larger file
                    result.download = stm.testGeneralDownloadSpeed();
                } else {
                    // Use content-specific test
                    try {
                        result.download = stm.testDownloadSpeedOkHttp("https://" + testDomain);
                        if (result.download == 0) {
                            result.download = stm.testDownloadSpeed("https://" + testDomain);
                        }
                    } catch (Exception e) {
                        result.download = stm.testDownloadSpeed("https://" + testDomain);
                    }
                }

                publishProgress(new TestProgress("download", String.format("%.2f", result.download) + " Mbps", result.download));

                // Test upload speed
                publishProgress(new TestProgress("upload", "Testing upload speed..."));
                Thread.sleep(500);

                if (isGeneralTest) {
                    // Use general upload test with larger file
                    result.upload = stm.testGeneralUploadSpeed();
                } else {
                    // Use standard upload test
                    try {
                        result.upload = stm.testUploadSpeedOkHttp();
                        if (result.upload == 0) {
                            result.upload = stm.testUploadSpeed();
                        }
                    } catch (Exception e) {
                        result.upload = stm.testUploadSpeed();
                    }
                }

                publishProgress(new TestProgress("upload", String.format("%.2f", result.upload) + " Mbps", result.upload));

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onProgressUpdate(TestProgress... values) {
            if (values.length > 0) {
                TestProgress progress = values[0];
                resultText.setText(progress.message);

                // Update individual values if test is complete
                if (progress.value >= 0) {
                    switch (progress.testType) {
                        case "ping":
                            pingValue.setText(progress.displayValue);
                            break;
                        case "download":
                            downloadValue.setText(progress.displayValue);
                            break;
                        case "upload":
                            uploadValue.setText(progress.displayValue);
                            break;
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(TestResult result) {
            progressBar.setVisibility(View.GONE);

            // Update all values
            pingValue.setText(result.ping >= 0 ? result.ping + " ms" : "Failed");
            downloadValue.setText(String.format("%.2f Mbps", result.download));
            uploadValue.setText(String.format("%.2f Mbps", result.upload));

            // Show jitter for general test
            if ("general".equals(testType) && jitterValue != null && result.jitter >= 0) {
                jitterValue.setText(result.jitter + " ms");
            }

            // Set quality rating
            String quality = getQualityRating(result.download, result.ping);
            qualityRating.setText(quality);

            // Build summary text
            StringBuilder summary = new StringBuilder();
            summary.append("✅ Test Complete!\n\n");
            summary.append("Test Type: ").append("general".equals(testType) ? "General Mobile Data" : service).append("\n");
            summary.append("Network: ").append(getNetworkType()).append("\n");

            if ("general".equals(testType) && result.jitter >= 0) {
                summary.append("Jitter: ").append(result.jitter).append(" ms\n");
            }

            resultText.setText(summary.toString());

            // Show detailed results
            detailedResultsContainer.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);
        }

        private String getQualityRating(double downloadSpeed, long ping) {
            if (downloadSpeed >= 25 && ping > 0 && ping < 50) {
                return "⭐⭐⭐⭐⭐ Excellent";
            } else if (downloadSpeed >= 10 && ping < 100) {
                return "⭐⭐⭐⭐ Good";
            } else if (downloadSpeed >= 5 && ping < 150) {
                return "⭐⭐⭐ Fair";
            } else if (downloadSpeed >= 2) {
                return "⭐⭐ Poor";
            } else {
                return "⭐ Very Poor";
            }
        }
    }

    // Helper classes for passing data
    private static class TestProgress {
        String testType;
        String message;
        String displayValue;
        double value;

        TestProgress(String testType, String message) {
            this.testType = testType;
            this.message = message;
            this.value = -1;
        }

        TestProgress(String testType, String displayValue, double value) {
            this.testType = testType;
            this.displayValue = displayValue;
            this.value = value;
            this.message = "";
        }
    }

    private static class TestResult {
        long ping = -1;
        double download = 0;
        double upload = 0;
        long jitter = -1;
    }
}