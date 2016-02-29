package com.bkozyrev.yotatest;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int CONNECT_TIMEOUT = 4000;

    EditText mUrlEditText;              //Поле для ввода урла
    TextView mHtmlCodeTextView;         //Текст с кодом со страницы
    Button mBtnClear, mBtnSendRequest;
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUrlEditText = (EditText) findViewById(R.id.url);
        mHtmlCodeTextView = (TextView) findViewById(R.id.html_code);
        mBtnClear = (Button) findViewById(R.id.clear_text);
        mBtnSendRequest = (Button) findViewById(R.id.send_request);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mBtnClear.setOnClickListener(this);
        mBtnSendRequest.setOnClickListener(this);

        trustAllHosts();
    }

    /*
     * Обработка нажатий
     * @param view - нажатый элемент
     */

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.clear_text:
                mHtmlCodeTextView.setText("");
                break;
            case R.id.send_request:
                Task task = new Task(mUrlEditText.getText().toString());
                task.execute();
                break;
            default:
                break;
        }
    }

    /*
     *  Подготовка, выполнение запроса и обработка полученного результата
     */

    class Task extends AsyncTask<Void, Void, String> {

        private String url;

        public Task(String url){
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            return sendRequest(url);
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressBar.setVisibility(View.GONE);
            if(result != null) {
                mHtmlCodeTextView.setText(result.substring(0, 5000)); //Выводит первые 5000 символов
                Toast.makeText(getBaseContext(), "Показаны первые 5000 символов", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getBaseContext(), "Произошла ошибка", Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(result);
        }
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
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.connect();

            is = urlConnection.getInputStream();
            int symbol;
            while ((symbol = is.read()) != -1) {
                sb.append((char) symbol);
            }
            result = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
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
