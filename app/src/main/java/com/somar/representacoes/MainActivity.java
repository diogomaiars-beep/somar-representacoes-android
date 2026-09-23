package com.somar.representacoes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String START_URL = "file:///android_asset/www/index.html";
    private static final String WHATSAPP_HOST = "wa.me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        showSplash();
    }

    private void showSplash() {
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setPadding(48, 48, 48, 48);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.somar.representacoes.R.mipmap.icon_192);
        logo.setAdjustViewBounds(true);
        splash.addView(logo, new LinearLayout.LayoutParams(180, 180));

        TextView title = new TextView(this);
        title.setText("Somar Representações");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 24, 0, 0);
        splash.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Catálogos para sua equipe comercial");
        subtitle.setTextSize(15);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 10, 0, 0);
        splash.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        setContentView(splash);
        new Handler().postDelayed(this::openApp, 1000);
    }

    private void openApp() {
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUserAgentString(settings.getUserAgentString() + " SomarRepresentacoesAndroid/2.0");

        webView.addJavascriptInterface(new AndroidShareBridge(this), "AndroidShare");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternalUrl(url);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                String fileName = "Somar-Representacoes.pdf";
                if (contentDisposition != null) {
                    String marker = "filename=";
                    int i = contentDisposition.indexOf(marker);
                    if (i >= 0) {
                        String candidate = contentDisposition.substring(i + marker.length()).replace("\"", "").trim();
                        if (!candidate.isEmpty()) fileName = candidate;
                    }
                }
                if (!fileName.toLowerCase().endsWith(".pdf")) fileName += ".pdf";
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType != null ? mimeType : "application/pdf");
                request.addRequestHeader("User-Agent", userAgent);
                request.setTitle(fileName);
                request.setDescription("Baixando catálogo da Somar Representações");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            } catch (Exception ignored) {
            }
        });

        webView.loadUrl(START_URL);
    }

    private boolean handleExternalUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            if (WHATSAPP_HOST.equalsIgnoreCase(host) || (host != null && host.toLowerCase().contains("whatsapp"))) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                } catch (Exception ignored) {
                }
            }
            return false;
        }

        if ("tel".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static class AndroidShareBridge {
        private final Context context;
        AndroidShareBridge(Context context) { this.context = context; }

        @JavascriptInterface
        public void shareText(String text) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
            context.startActivity(Intent.createChooser(intent, "Compartilhar catálogo"));
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
