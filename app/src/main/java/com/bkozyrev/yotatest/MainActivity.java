package com.bkozyrev.yotatest;

import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private Subscription mSubscription;

    @Bind(R.id.url) EditText mUrlEditText;              //Поле для ввода урла
    @Bind(R.id.html_code) TextView mHtmlCodeTextView;   //Текст с кодом со страницы

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        trustAllHosts();
    }

    /*
     *  Метод по очистке текста
     */
    @OnClick(R.id.clear_text) void clearText(){
        mHtmlCodeTextView.setText("");
    }

    /*
     *  Подготовка запроса и обработка полученного результата
     */
    @OnClick(R.id.send_request) void handleClick(){

        mSubscription = Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        subscriber.onNext(sendRequest(mUrlEditText.getText().toString()));
                    }
                })
                .subscribeOn(Schedulers.newThread())  //Запрос будет создан в отдельном потоке
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getBaseContext(), "An error occured", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(String response) {
                        mHtmlCodeTextView.setText(response.substring(0, 5000)); //Выводит первые 10000 символов
                        Toast.makeText(getBaseContext(), "Показаны первые 5000 символов", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
     * Формирование запроса и считывание результата
     * @param path урл для запроса
     * @return строка с ответом
     */
    public String sendRequest(String path){

        if(!path.startsWith("https://") && !path.startsWith("http://")){
            path = "https://" + path;
        }

        String result = null;
        StringBuilder sb = new StringBuilder();
        InputStream is;
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(path);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            is = urlConnection.getInputStream();
            int symbol;
            while ((symbol = is.read()) != -1) {
                sb.append((char) symbol);
            }
            result = sb.toString();
            Log.d("success", "result = " + result);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
    }

    protected void onStop() {
        unsubscribe();
        super.onStop();
    }

    private void unsubscribe() {
        if(mSubscription != null && !mSubscription.isUnsubscribed())
            mSubscription.unsubscribe();
    }

    /**
     * Trust every server - dont check for any certificate
     */
    public static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
