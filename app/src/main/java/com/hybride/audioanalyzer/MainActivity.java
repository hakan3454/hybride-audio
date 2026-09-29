package com.hybride.audioanalyzer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setDomStorageEnabled(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Metinleri DOM'u yeniden oluşturmadan değiştiriyoruz.
                // Böylece input/change/click event listener'ları kaybolmuyor.
                String js = "(function(){" +
                        "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);" +
                        "var n;while(n=w.nextNode()){if(n.nodeValue&&n.nodeValue.indexOf('Xilica')!==-1){" +
                        "n.nodeValue=n.nodeValue.replace(/Xilica EQ analizi/g,'parametrik EQ analizi')" +
                        ".replace(/Önerilen Xilica EQ/g,'Önerilen Parametrik EQ')" +
                        ".replace(/Xilica/g,'');}}" +
                        "})();";
                view.evaluateJavascript(js, null);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;

                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                    startActivityForResult(Intent.createChooser(intent, "Fotoğraf veya video seç"), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) {
            return;
        }

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        }

        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }
}
