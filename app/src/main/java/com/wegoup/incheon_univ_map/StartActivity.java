package com.wegoup.incheon_univ_map;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 3000; // milliseconds (3 seconds)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Handler를 사용하여 일정 시간이 지난 후에 MainActivity로 이동합니다.
        Toast.makeText(StartActivity.this, "일반 지도와 7호관 정보기술대학 실내 정보를 확인할 수 있습니다", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // MainActivity를 시작합니다.
                Intent mainIntent = new Intent(StartActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish(); // 시작 페이지를 종료합니다.
            }
        }, SPLASH_DELAY);
    }
}