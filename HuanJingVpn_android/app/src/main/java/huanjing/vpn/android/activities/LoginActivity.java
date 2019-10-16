package huanjing.vpn.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import huanjing.vpn.android.Preferences;
import huanjing.vpn.android.R;
import huanjing.vpn.android.fragment.dialog.LoadingDialogFragment;
import huanjing.vpn.android.network.VPNService;
import huanjing.vpn.android.network.responses.ServersResponse;
import huanjing.vpn.android.utils.PrefUtils;

public class LoginActivity extends BaseActivity {

    public static final String PROCEED_TO_MAIN = "return", STATE_USERNAME = "state_username",
                        STATE_PASSWORD = "state_password", STATE_SAVE_PWD = "save_password", STATE_AUTO_CONNECT = "auto_connect";

    /*private Validator mValidator;*/
    private boolean mSavePwd = false;
    private boolean mAutoConnect = false;

    /*String currentLanguage = "en", currentLang;
    Typeface mFont;*/

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
    @Bind(R.id.savePwdButton)
    Button mSavePwdButton;
    @NotEmpty
    @Bind(R.id.autoConnectButton)
    Button mAutoConnectButton;
    @NotEmpty
    @Bind(R.id.savePwdText)
    TextView mSavePwdText;
    @NotEmpty
    @Bind(R.id.autoConnectText)
    TextView mAutoConnectText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_login);

        mSavePwd = PrefUtils.get(this, STATE_SAVE_PWD, false);
        mAutoConnect = PrefUtils.get(this, STATE_AUTO_CONNECT, false);
        setSavePwdCheck();
        if (mSavePwd) {
            String uname = PrefUtils.get(this, Preferences.USERNAME, "");
            String upwd = PrefUtils.get(this, Preferences.PASSWORD, "");
            mUsername.setText(uname);
            mPassword.setText(upwd);
            setAutoConnectCheck();
            if (!uname.isEmpty() && !upwd.isEmpty())
                login();
        } else {
            mAutoConnectButton.setVisibility(View.GONE);
            mAutoConnectText.setVisibility(View.GONE);
        }

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
        if(mSavePwd) {
            PrefUtils.save(this, STATE_USERNAME, mUsername.getText().toString());
            PrefUtils.save(this, STATE_PASSWORD, mPassword.getText().toString());
        }
    }

    @OnClick(R.id.loginButton)
    public void loginClick(View v) {
        //mValidator.validate(true);
        login();
    }

    public void login(){
        if (mUsername.getText().toString().isEmpty())
        {
            mUsername.setError("Username must be entered!"); return;
        }
        if (mPassword.getText().toString().isEmpty())
        {
            mPassword.setError("Password must be entered!"); return;
        }

        onValidationSucceeded();
    }

    @OnClick(R.id.savePwdButton)
    public void savedPwdButtonClick(View v) {
        mSavePwd = !mSavePwd;
        setSavePwdCheck();
    }

    @OnClick(R.id.savePwdText)
    public void savePwdTextClick(View v) {
        mSavePwd = !mSavePwd;
        setSavePwdCheck();
    }

    public void setSavePwdCheck() {

        int check_drawable = (mSavePwd)? R.drawable.check: R.drawable.uncheck;

        final int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mSavePwdButton.setBackgroundDrawable(ContextCompat.getDrawable(this, check_drawable) );
        } else {
            mSavePwdButton.setBackground(ContextCompat.getDrawable(this, check_drawable));
        }

        if (!mSavePwd) {
            mAutoConnectButton.setVisibility(View.GONE);
            mAutoConnectText.setVisibility(View.GONE);
        } else {
            mAutoConnectButton.setVisibility(View.VISIBLE);
            mAutoConnectText.setVisibility(View.VISIBLE);
        }

        PrefUtils.save(this, STATE_SAVE_PWD, mSavePwd);
    }

    @OnClick(R.id.autoConnectButton)
    public void autoConnectButtonClick(View v) {
        mAutoConnect = !mAutoConnect;
        setAutoConnectCheck();
    }

    @OnClick(R.id.autoConnectText)
    public void saveAutoConnectTextClick(View v) {
        mAutoConnect = !mAutoConnect;
        setAutoConnectCheck();
    }

    public void setAutoConnectCheck() {

        int check_drawable = (mAutoConnect)? R.drawable.check: R.drawable.uncheck;

        final int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mAutoConnectButton.setBackgroundDrawable(ContextCompat.getDrawable(this, check_drawable) );
        } else {
            mAutoConnectButton.setBackground(ContextCompat.getDrawable(this, check_drawable));
        }

        PrefUtils.save(this, STATE_AUTO_CONNECT, mAutoConnect);

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

    public void onValidationSucceeded() {
        LoadingDialogFragment.show(getSupportFragmentManager());
        VPNService.get(mUsername.getText().toString(), mPassword.getText().toString()).servers(new Callback<ServersResponse>() {
            @Override
            public void success(ServersResponse serversResponse, Response response) {
                LoadingDialogFragment.dismiss(getSupportFragmentManager());
                PrefUtils.save(LoginActivity.this, Preferences.USERNAME, mUsername.getText().toString());
                PrefUtils.save(LoginActivity.this, Preferences.PASSWORD, mPassword.getText().toString());

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                LoadingDialogFragment.dismiss(getSupportFragmentManager());
                if(error.getResponse() != null && error.getResponse().getStatus() == 401)
                    Toast.makeText(LoginActivity.this, R.string.response_401, Toast.LENGTH_LONG).show();
                else if(error.getResponse() != null && error.getResponse().getStatus() == 402)
                    Toast.makeText(LoginActivity.this, R.string.response_402, Toast.LENGTH_LONG).show();
                else if(error.getResponse() != null && error.getResponse().getStatus() == 409)
                    Toast.makeText(LoginActivity.this, R.string.response_409, Toast.LENGTH_LONG).show();
                else if(error.getResponse() != null && error.getResponse().getStatus() == 423)
                    Toast.makeText(LoginActivity.this, R.string.response_423, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(LoginActivity.this, R.string.response_unknown, Toast.LENGTH_LONG).show();
            }
        });
    }

    /*@Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            Log.d("View Id", String.valueOf(view.getId()));
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
    }*/

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
