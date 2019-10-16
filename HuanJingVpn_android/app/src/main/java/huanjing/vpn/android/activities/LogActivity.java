package huanjing.vpn.android.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import huanjing.vpn.android.R;
import huanjing.vpn.android.fragment.LogFragment;

public class LogActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_frame);

        setupAppBar();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new LogFragment())
                    .commit();
        }
    }

    private void setupAppBar() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            actionBar.setTitle(R.string.logs);
        }
    }
}
