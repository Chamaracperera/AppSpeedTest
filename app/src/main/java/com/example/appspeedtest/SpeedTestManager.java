package com.example.appspeedtest;

import android.util.Log;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class SpeedTestManager {

    private static final String TAG = "SpeedTestManager";
    private static final int DOWNLOAD_TEST_DURATION_MS = 8000;
    private static final int UPLOAD_TEST_SIZE_KB = 512;

    // Public files for download testing
    private static final String DOWNLOAD_TEST_FILE = "https://speed.cloudflare.com/__down?bytes=10000000"; // 10MB file
    private static final String GENERAL_DOWNLOAD_TEST = "https://speed.cloudflare.com/__down?bytes=25000000"; // 25MB for general test

    // Multiple test servers for reliability
    private static final String[] TEST_SERVERS = {
            "https://speed.cloudflare.com/__down?bytes=",
            "https://proof.ovh.net/files/",
            "http://ipv4.download.thinkbroadband.com/"
    };

    /**
     * Improved ping test using InetAddress
     */
    public long getPing(String host) {
        try {
            // Remove protocol if present
            host = host.replace("https://", "").replace("http://", "").split("/")[0];

            Log.d(TAG, "Testing ping for: " + host);

            // Perform multiple pings and get average
            long totalTime = 0;
            int successfulPings = 0;

            for (int i = 0; i < 3; i++) {
                try {
                    long start = System.currentTimeMillis();
                    InetAddress address = InetAddress.getByName(host);
                    boolean reachable = address.isReachable(3000);
                    long end = System.currentTimeMillis();

                    if (reachable) {
                        long pingTime = end - start;
                        totalTime += pingTime;
                        successfulPings++;
                        Log.d(TAG, "Ping " + (i+1) + ": " + pingTime + " ms");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ping attempt " + (i+1) + " failed: " + e.getMessage());
                }

                Thread.sleep(200);
            }

            if (successfulPings > 0) {
                long avgPing = totalTime / successfulPings;
                Log.d(TAG, "Average ping: " + avgPing + " ms");
                return avgPing;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ping test error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Improved download speed test using HttpURLConnection
     */
    public double testDownloadSpeed(String url) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            // Use Cloudflare's speed test endpoint for reliable testing
            URL testUrl = new URL(DOWNLOAD_TEST_FILE);

            Log.d(TAG, "Starting download test from: " + testUrl);

            connection = (HttpURLConnection) testUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();

                byte[] buffer = new byte[8192];
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    long currentTime = System.currentTimeMillis();

                    // Stop after test duration
                    if (currentTime - startTime > DOWNLOAD_TEST_DURATION_MS) {
                        break;
                    }
                }

                long endTime = System.currentTimeMillis();
                double durationSec = (endTime - startTime) / 1000.0;

                if (durationSec > 0 && totalBytes > 0) {
                    double speedMbps = (totalBytes * 8.0) / (durationSec * 1_000_000.0);
                    Log.d(TAG, "Download: " + totalBytes + " bytes in " + durationSec + "s = " + speedMbps + " Mbps");
                    return speedMbps;
                }
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Download test error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * General download test - tests overall mobile data speed (not content-specific)
     * Uses larger file and longer duration for more accurate general speed measurement
     */
    public double testGeneralDownloadSpeed() {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            // Use larger file for general test
            URL testUrl = new URL(GENERAL_DOWNLOAD_TEST);

            Log.d(TAG, "Starting GENERAL download test from: " + testUrl);

            connection = (HttpURLConnection) testUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();

                byte[] buffer = new byte[8192];
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();
                int bytesRead;

                // Longer test duration for general test
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    long currentTime = System.currentTimeMillis();

                    // Log progress
                    if (totalBytes % (1024 * 1024 * 5) == 0) { // Every 5MB
                        double currentSpeed = (totalBytes * 8.0) / ((currentTime - startTime) / 1000.0 * 1_000_000.0);
                        Log.d(TAG, "Progress: " + (totalBytes / (1024 * 1024)) + " MB, Current speed: " + String.format("%.2f", currentSpeed) + " Mbps");
                    }

                    // 12 second test for general
                    if (currentTime - startTime > 12000) {
                        break;
                    }
                }

                long endTime = System.currentTimeMillis();
                double durationSec = (endTime - startTime) / 1000.0;

                if (durationSec > 0 && totalBytes > 0) {
                    double speedMbps = (totalBytes * 8.0) / (durationSec * 1_000_000.0);
                    Log.d(TAG, "GENERAL Download: " + totalBytes + " bytes in " + durationSec + "s = " + speedMbps + " Mbps");
                    return speedMbps;
                }
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "General download test error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * General upload test - larger file for accurate general speed
     */
    public double testGeneralUploadSpeed() {
        HttpURLConnection connection = null;

        try {
            URL url = new URL("https://httpbin.org/post");

            Log.d(TAG, "Starting GENERAL upload test to: " + url);

            // Larger file for general test (1MB)
            byte[] data = new byte[1024 * 1024];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i % 256);
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(data.length));

            long startTime = System.currentTimeMillis();

            // Upload data
            connection.getOutputStream().write(data);
            connection.getOutputStream().flush();

            // Wait for response
            int responseCode = connection.getResponseCode();
            long endTime = System.currentTimeMillis();

            Log.d(TAG, "Upload response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                double durationSec = (endTime - startTime) / 1000.0;
                double sizeMb = (data.length * 8.0) / 1_000_000.0;
                double speedMbps = sizeMb / durationSec;

                Log.d(TAG, "GENERAL Upload: " + data.length + " bytes in " + durationSec + "s = " + speedMbps + " Mbps");
                return speedMbps;
            }
        } catch (Exception e) {
            Log.e(TAG, "General upload test error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return 0;
    }

    /**
     * Test latency/jitter - multiple pings to measure consistency
     */
    public LatencyResult testLatency(String host) {
        try {
            host = host.replace("https://", "").replace("http://", "").split("/")[0];

            Log.d(TAG, "Testing latency for: " + host);

            long[] pings = new long[10];
            int successfulPings = 0;

            for (int i = 0; i < 10; i++) {
                try {
                    long start = System.currentTimeMillis();
                    InetAddress address = InetAddress.getByName(host);
                    boolean reachable = address.isReachable(3000);
                    long end = System.currentTimeMillis();

                    if (reachable) {
                        pings[successfulPings] = end - start;
                        successfulPings++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ping attempt " + (i+1) + " failed");
                }

                Thread.sleep(100);
            }

            if (successfulPings > 0) {
                // Calculate average, min, max, jitter
                long sum = 0;
                long min = Long.MAX_VALUE;
                long max = 0;

                for (int i = 0; i < successfulPings; i++) {
                    sum += pings[i];
                    if (pings[i] < min) min = pings[i];
                    if (pings[i] > max) max = pings[i];
                }

                long avg = sum / successfulPings;
                long jitter = max - min;

                return new LatencyResult(avg, min, max, jitter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Latency test error: " + e.getMessage());
        }
        return new LatencyResult(-1, -1, -1, -1);
    }

    /**
     * Helper class for latency results
     */
    public static class LatencyResult {
        public long average;
        public long min;
        public long max;
        public long jitter;

        public LatencyResult(long avg, long min, long max, long jitter) {
            this.average = avg;
            this.min = min;
            this.max = max;
            this.jitter = jitter;
        }
    }

    /**
     * Alternative download test using OkHttp (if you have the dependency)
     */
    public double testDownloadSpeedOkHttp(String url) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(DOWNLOAD_TEST_FILE)
                .addHeader("Cache-Control", "no-cache")
                .build();

        try {
            Log.d(TAG, "Starting OkHttp download test");

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                byte[] buffer = new byte[8192];
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();

                InputStream inputStream = response.body().byteStream();
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    totalBytes += read;
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - startTime > DOWNLOAD_TEST_DURATION_MS) {
                        break;
                    }
                }

                inputStream.close();
                response.close();

                long endTime = System.currentTimeMillis();
                double durationSec = (endTime - startTime) / 1000.0;

                if (durationSec > 0 && totalBytes > 0) {
                    double speedMbps = (totalBytes * 8.0) / (durationSec * 1_000_000.0);
                    Log.d(TAG, "Download: " + totalBytes + " bytes in " + durationSec + "s = " + speedMbps + " Mbps");
                    return speedMbps;
                }
            } else {
                Log.e(TAG, "Response not successful or body is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "OkHttp download error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Upload speed test using httpbin.org (no Firebase needed)
     */
    public double testUploadSpeed() {
        HttpURLConnection connection = null;

        try {
            // Using httpbin.org for upload testing (publicly available)
            URL url = new URL("https://httpbin.org/post");

            Log.d(TAG, "Starting upload test to: " + url);

            // Create test data
            byte[] data = new byte[1024 * UPLOAD_TEST_SIZE_KB];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i % 256);
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(data.length));

            long startTime = System.currentTimeMillis();

            // Upload data
            connection.getOutputStream().write(data);
            connection.getOutputStream().flush();

            // Wait for response
            int responseCode = connection.getResponseCode();
            long endTime = System.currentTimeMillis();

            Log.d(TAG, "Upload response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                double durationSec = (endTime - startTime) / 1000.0;
                double sizeMb = (data.length * 8.0) / 1_000_000.0;
                double speedMbps = sizeMb / durationSec;

                Log.d(TAG, "Upload: " + data.length + " bytes in " + durationSec + "s = " + speedMbps + " Mbps");
                return speedMbps;
            }
        } catch (Exception e) {
            Log.e(TAG, "Upload test error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return 0;
    }

    /**
     * Alternative upload test using OkHttp
     */
    public double testUploadSpeedOkHttp() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        try {
            Log.d(TAG, "Starting OkHttp upload test");

            // Create test data
            byte[] data = new byte[1024 * UPLOAD_TEST_SIZE_KB];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i % 256);
            }

            RequestBody body = RequestBody.create(
                    data,
                    MediaType.parse("application/octet-stream")
            );

            Request request = new Request.Builder()
                    .url("https://httpbin.org/post")
                    .post(body)
                    .build();

            long startTime = System.currentTimeMillis();

            Response response = client.newCall(request).execute();

            long endTime = System.currentTimeMillis();

            if (response.isSuccessful()) {
                double durationSec = (endTime - startTime) / 1000.0;
                double sizeMb = (data.length * 8.0) / 1_000_000.0;
                double speedMbps = sizeMb / durationSec;

                Log.d(TAG, "Upload: " + data.length + " bytes in " + durationSec + "s = " + speedMbps + " Mbps");
                response.close();
                return speedMbps;
            }
            response.close();
        } catch (Exception e) {
            Log.e(TAG, "OkHttp upload error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}