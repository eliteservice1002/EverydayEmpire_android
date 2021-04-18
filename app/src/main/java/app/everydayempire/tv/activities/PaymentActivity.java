package app.everydayempire.tv.activities;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import app.everydayempire.tv.R;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        findViewById(R.id.header_back).setOnClickListener(v -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.payment_label);
        findViewById(R.id.header_more).setVisibility(View.GONE);
        WebView webview = findViewById(R.id.webview);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.v(TAG, "Page " + url + " has finished loading.");
                String path = Uri.parse(url).getPath();
                if (path.contains("wallet/response")) {
                    finish();
                }
            }
        });
        webview.loadUrl(getIntent().getDataString());
    }
}
