package me.islinjw.shadercamera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @Bind(R.id.preview)
    TextureView mPreview;

    @Bind(R.id.record)
    CheckBox mRecord;

    @Bind(R.id.switch_camera)
    Button mSwitchCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnCheckedChanged(R.id.record)
    public void record(boolean start) {
    }

    @OnClick(R.id.switch_camera)
    public void switchCamera() {
    }

    @Override
    protected void onResume() {
        super.onResume();

        makeFullscreen();
    }

    private void makeFullscreen() {
        getWindow().getDecorView()
            .setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
    }
}
