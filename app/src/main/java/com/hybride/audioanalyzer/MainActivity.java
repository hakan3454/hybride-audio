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

                String js = "(function(){" +
                        "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);" +
                        "var n;while(n=w.nextNode()){if(n.nodeValue){" +
                        "n.nodeValue=n.nodeValue.replace(/Xilica EQ analizi/g,'parametrik EQ analizi')" +
                        ".replace(/Önerilen Xilica EQ/g,'Frekans Analizi ve Öneriler')" +
                        ".replace(/Önerilen Parametrik EQ/g,'Frekans Analizi ve Öneriler')" +
                        ".replace(/Göreli parametrik kesimler/g,'31 bant / göreli')" +
                        ".replace(/Xilica/g,'');}}" +

                        "var bands=[20,25,31.5,40,50,63,80,100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000,6300,8000,10000,12500,16000,20000];" +
                        "function avg(a){return a.length?a.reduce(function(s,v){return s+v},0)/a.length:0;}" +
                        "function med(a){var b=a.slice().sort(function(x,y){return x-y});var m=Math.floor(b.length/2);return b.length%2?b[m]:(b[m-1]+b[m])/2;}" +
                        "function bandValue(curve,f){var lo=f/Math.pow(2,1/6),hi=f*Math.pow(2,1/6),v=[];for(var i=0;i<curve.length;i++){var ff=20*Math.pow(1000,i/(curve.length-1));if(ff>=lo&&ff<=hi)v.push(curve[i]);}return avg(v);}" +
                        "function makeBandRows(curve){var vals=bands.map(function(f){return bandValue(curve,f)});var ref=med(vals);return bands.map(function(f,i){var rel=Math.max(-6,Math.min(6,(vals[i]-ref)*12));var rec=Math.max(-3,Math.min(3,-rel*0.65));if(Math.abs(rec)<0.8)rec=0;var txt=rec===0?'Değiştirme':(rec>0?rec.toFixed(1)+' dB artır':Math.abs(rec).toFixed(1)+' dB azalt');return {f:f,rel:rel,rec:rec,text:txt};});}" +
                        "var oldRender=window.render;" +
                        "window.render=function(a){oldRender(a);var t=document.getElementById('eqTable');if(!t||!a.bandRows)return;t.innerHTML='<div class=\"row head\"><div>Frekans</div><div>Göreli</div><div>Öneri</div></div>';a.bandRows.forEach(function(r){var f=r.f>=1000?((r.f/1000)+(r.f%1000?'':'')).replace('.0','')+' kHz':r.f+' Hz';var rel=(r.rel>=0?'+':'')+r.rel.toFixed(1)+' dB';t.innerHTML+='<div class=\"row\"><div>'+f+'</div><div>'+rel+'</div><div>'+r.text+'</div></div>';});var note=document.getElementById('note');if(note)note.innerHTML='<b>Göreli analiz:</b> 31 nominal frekans bandının tamamı gösterilir. “Göreli” sütunu, ekran görüntüsündeki eğriye göre 0 dB referansına normalize edilmiştir. Öneriler kesin ölçüm değil; güvenli başlangıç ayarı olarak en fazla ±3 dB ile sınırlandırılmıştır.';};" +
                        "window.runAnalysis=async function(){if(!file.files.length){st.textContent='Önce bir ölçüm dosyası seç';return;}st.textContent='RTA eğrisi okunuyor…';try{var src;if(file.files[0].type.startsWith('video/')){if(videoPreview.readyState<2)throw new Error('Video karesi henüz hazır değil');videoPreview.pause();src=videoPreview;}else{if(!imagePreview.complete)await new Promise(function(res,rej){imagePreview.onload=res;imagePreview.onerror=rej});src=imagePreview;}var ext=extractCurve(src);var a=analyzeCurve(ext.curve,ext.coverage);a.bandRows=makeBandRows(ext.curve);render(a);}catch(err){st.textContent='Analiz yapılamadı: '+(err.message||'görsel okunamadı');}};" +
                        "var b=document.getElementById('analyze');if(b)b.onclick=window.runAnalysis;" +
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
