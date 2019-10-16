package shenye.vpn.android.activities;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import java.io.File;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import shenye.vpn.android.BuildConfig;
import shenye.vpn.android.Preferences;
import shenye.vpn.android.R;
import shenye.vpn.android.UpdateApp;
import shenye.vpn.android.fragment.dialog.DownloadingDialogFragment;
import shenye.vpn.android.fragment.dialog.LoadingDialogFragment;
import shenye.vpn.android.network.VPNService;
import shenye.vpn.android.network.responses.ServersResponse;
import shenye.vpn.android.utils.PrefUtils;

public class LoginActivity extends BaseActivity implements Validator.ValidationListener {

    public static final String PROCEED_TO_MAIN = "return", STATE_USERNAME = "state_username", STATE_PASSWORD = "state_password";
    private static final int PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED;
    private Validator mValidator;
    private boolean mDoNotSave = false;
    String currentLanguage = "en", currentLang;
    Typeface mFont;

    @NotEmpty
    @Bind(R.id.usernameEdit)
    EditText mUsername;
    @NotEmpty
    @Bind(R.id.passwordEdit)
    EditText mPassword;
    @NotEmpty
    @Bind(R.id.loginButton)
    Button mLoginButton;
    @NotEmpty
    @Bind(R.id.registerButton)
    Button mRegisterButton;
    @NotEmpty
    @Bind(R.id.versionText)
    TextView mVersionText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_login);
        mValidator = new Validator(this);
        mValidator.setValidationListener(this);

        currentLanguage = getIntent().getStringExtra(currentLang);

        if (PrefUtils.get(this, Preferences.LANGUAGE, "").isEmpty())
            selectLanguage();

        //mFont = Typeface.createFromAsset(getAssets(), "font/wts.ttf");
        //mUsername.setTypeface(mFont);
        //mPassword.setTypeface(mFont);
        //mLoginButton.setTypeface(mFont);
        //mRegisterButton.setTypeface(mFont);
        mUsername.setTextSize(22.0f);
        mPassword.setTextSize(22.0f);
        mLoginButton.setTextSize(22.0f);
        mRegisterButton.setTextSize(22.0f);
        mVersionText.setText("v " + BuildConfig.VERSION_NAME);
        if(savedInstanceState != null)
            onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUsername.setText(PrefUtils.get(this, STATE_USERNAME, ""));
        mPassword.setText(PrefUtils.get(this, STATE_PASSWORD, ""));
        PrefUtils.remove(this, STATE_USERNAME);
        PrefUtils.remove(this, STATE_PASSWORD);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!mDoNotSave) {
            PrefUtils.save(this, STATE_USERNAME, mUsername.getText().toString());
            PrefUtils.save(this, STATE_PASSWORD, mPassword.getText().toString());
        }
    }

    @OnClick(R.id.loginButton)
    public void loginClick(View v) {
        mValidator.validate(true);
    }

    @OnClick(R.id.registerButton)
    public void registerClick(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.shenyeymz.com/shenye/Home/sign_up"));
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        try {
            if (!getIntent().getBooleanExtra(PROCEED_TO_MAIN, true)) {
                setResult(RESULT_CANCELED);
            }
            super.onBackPressed();
        } catch (IllegalStateException e) {
            // catch activity close
        }
    }

    @Override
    public void onValidationSucceeded() {
        LoadingDialogFragment.show(getSupportFragmentManager());
        VPNService.get(mUsername.getText().toString(), mPassword.getText().toString()).servers(new Callback<ServersResponse>() {
            @Override
            public void success(ServersResponse serversResponse, Response response) {
                LoadingDialogFragment.dismiss(getSupportFragmentManager());
                PrefUtils.save(LoginActivity.this, Preferences.USERNAME, mUsername.getText().toString());
                PrefUtils.save(LoginActivity.this, Preferences.PASSWORD, mPassword.getText().toString());

                mDoNotSave = true;
                if (getIntent().getBooleanExtra(PROCEED_TO_MAIN, true)) {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                LoadingDialogFragment.dismiss(getSupportFragmentManager());
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this).setTitle("Note");
                builder.setIcon(R.mipmap.ic_launcher);

                if(error.getResponse() != null && error.getResponse().getStatus() == 401)
                    builder.setMessage(R.string.response_401).show();
                else if(error.getResponse() != null && error.getResponse().getStatus() == 402)
                    builder.setMessage(R.string.response_402);
                else if(error.getResponse() != null && error.getResponse().getStatus() == 409)
                {

                    builder.setMessage(R.string.response_409);
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                askPermission();
                            }
                        }
                    ).setNegativeButton(android.R.string.no, null).show();

                }
                else if(error.getResponse() != null && error.getResponse().getStatus() == 423)
                    builder.setMessage(R.string.response_423).show();
                else
                    builder.setMessage(R.string.response_unknown).show();
            }
        });
    }

    private void askPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int permission_code = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission_code == PERMISSION_GRANTED)
                OnPermissionAllowed();
            else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        } else
            OnPermissionAllowed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PERMISSION_GRANTED)
            OnPermissionAllowed();
        else
            finish();
    }

    private void OnPermissionAllowed()
    {
        autoUpdate();
        /*UpdateApp atualizaApp = new UpdateApp();
        atualizaApp.setContext(LoginActivity.this);
        atualizaApp.execute(getString(R.string.download_app_url));*/
    }

    public void autoUpdate() {
        DownloadingDialogFragment.show(getSupportFragmentManager());
        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        String fileName = "ShenyeVPN.apk";
        destination += fileName;
        final Uri uri = Uri.parse("file://" + destination);

        //Delete update file if exists
        File file = new File(destination);
        if (file.exists())
            //file.delete() - test this, I think sometimes it doesnt work
            file.delete();

        //get url of app on server
        String url = this.getString(R.string.download_app_url);

        //set downloadmanager
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(this.getString(R.string.notification_description));
        request.setTitle(this.getString(R.string.app_name));

        //set destination
        request.setDestinationUri(uri);

        // get download service and enqueue file
        final DownloadManager manager = (DownloadManager) getSystemService(this.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        //set BroadcastReceiver to install app when .apk is downloaded
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            String PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/ShenyeVPN.apk";
            File appfile = new File(PATH);
            public void onReceive(Context ctxt, Intent intent) {
                DownloadingDialogFragment.dismiss(getSupportFragmentManager());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri apkUri = FileProvider.getUriForFile(LoginActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", appfile);
                    Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    install.setData(apkUri);
                    install.setFlags(install.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(install);
                } else {
                    Uri apkUri = Uri.fromFile(appfile);
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(install);
                }

                unregisterReceiver(this);
                finish();
            }
        };
        //register receiver for when .apk download is compete
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);

            if (view instanceof EditText) {
                ((EditText) view).setError(message);
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectLanguage()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert);
        CharSequence[] langs = new CharSequence[]{"中文", "English"};
        final String[] codes = new String[]{"cn", "en"};

        builder.setItems( langs, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int idx) {

                Resources res = getResources();
                DisplayMetrics dm = res.getDisplayMetrics();
                Configuration conf = res.getConfiguration();
                conf.locale = new Locale(codes[idx]);

                res.updateConfiguration(conf, dm);
                Intent refresh = new Intent(LoginActivity.this, LoginActivity.class);
                PrefUtils.save(LoginActivity.this, Preferences.LANGUAGE, codes[idx]);

                startActivity(refresh);

                dialogInterface.dismiss();
            }
        });
        AlertDialog create = builder.create();
        if (Build.VERSION.SDK_INT >= 26) {
            builder.create().getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY); // 2038
        } else {
            builder.create().getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT); // 2003
        }
        create.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_USERNAME, mUsername.getText().toString());
        outState.putString(STATE_PASSWORD, mPassword.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUsername.setText(savedInstanceState.getString(STATE_USERNAME, ""));
        mPassword.setText(savedInstanceState.getString(STATE_PASSWORD, ""));
    }
}
