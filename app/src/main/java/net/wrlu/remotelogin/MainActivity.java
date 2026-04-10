package net.wrlu.remotelogin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat switchSuperHost;
    private EditText etCustomRole;
    private Button btnSetRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void initViews() {
        switchSuperHost = findViewById(R.id.switchSuperHost);
        etCustomRole = findViewById(R.id.etCustomRole);
        btnSetRole = findViewById(R.id.btnSetRole);
    }

    /**
     * 统一的 UI 刷新方法
     */
    private void updateUI() {
        String currentRole = Config.get(this, Config.NAME_ROLE);
        boolean isLocked = Config.isLockedByProperty(Config.NAME_ROLE);

        etCustomRole.setEnabled(!isLocked);
        switchSuperHost.setEnabled(!isLocked);
        btnSetRole.setEnabled(!isLocked);

        if (currentRole == null || currentRole.isEmpty()) {
            etCustomRole.setHint("当前无角色配置");
        } else {
            etCustomRole.setHint("当前角色: " + currentRole);
        }
        etCustomRole.setText("");

        switchSuperHost.setOnCheckedChangeListener(null);

        // 如果被系统属性锁定了，处理锁定状态的静态 UI 显示，并直接退出
        if (isLocked) {
            btnSetRole.setOnClickListener(null);
            switchSuperHost.setChecked(Config.Role.SUPER_HOST.equals(currentRole));

            Toast.makeText(this, "已从Properties中配置角色，只能使用adb进行编辑。",
                    Toast.LENGTH_LONG).show();
            return;
        }


        btnSetRole.setOnClickListener(v -> {
            String customRole = etCustomRole.getText().toString().trim();

            if (customRole.isEmpty()) {
                Toast.makeText(MainActivity.this, "输入不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            Config.setToPreference(MainActivity.this, Config.NAME_ROLE, customRole);
            Toast.makeText(MainActivity.this, "已写入: " + customRole, Toast.LENGTH_SHORT).show();

            updateUI();
        });

        switchSuperHost.setChecked(Config.Role.SUPER_HOST.equals(currentRole));
        switchSuperHost.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Config.setToPreference(MainActivity.this, Config.NAME_ROLE,
                        Config.Role.SUPER_HOST);
            } else {
                Config.setToPreference(MainActivity.this, Config.NAME_ROLE,
                        Config.Role.CLIENT);
            }

            updateUI();
        });
    }
}