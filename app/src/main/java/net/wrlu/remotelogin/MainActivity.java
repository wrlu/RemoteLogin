package net.wrlu.remotelogin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import net.wrlu.remotelogin.transfer.Role;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat switchSuperHost;
    private EditText etCustomRole;
    private Button btnSetRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
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

    private void setupListeners() {
        // 自定义写入按钮的点击事件
        btnSetRole.setOnClickListener(v -> {
            String customRole = etCustomRole.getText().toString().trim();

            // 增加非空校验，防止用户直接点击提交导致写入空字符串
            if (customRole.isEmpty()) {
                Toast.makeText(MainActivity.this, "输入不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 写入 SP
            Role.setRoleLocal(MainActivity.this, customRole);
            Toast.makeText(MainActivity.this, "已写入: " + customRole, Toast.LENGTH_SHORT).show();

            // 刷新 UI (刷新逻辑中包含了清空文本和更新 Hint 的操作)
            updateUI();
        });
    }

    /**
     * 统一的 UI 刷新方法
     */
    private void updateUI() {
        // 读取当前 SP 中的真实 role
        String currentRole = Role.getRole(this);

        // 1. 同步输入框内容：改为修改 Hint，并清空 Text
        if (currentRole == null || currentRole.isEmpty()) {
            etCustomRole.setHint("当前无自定义 Role");
        } else {
            etCustomRole.setHint("当前值: " + currentRole);
        }
        // 提交或刷新后，确保输入框被清空
        etCustomRole.setText("");

        // 2. 同步 Switch 状态
        // 关键步骤：先移除监听器，防止触发回调导致死循环写入
        switchSuperHost.setOnCheckedChangeListener(null);

        switchSuperHost.setChecked(Role.SUPER_HOST.equals(currentRole));

        // 恢复监听器
        switchSuperHost.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Role.setRoleLocal(MainActivity.this, Role.SUPER_HOST);
            } else {
                Role.setRoleLocal(MainActivity.this, Role.CLIENT);
            }
            // 状态改变后，再次刷新 UI
            updateUI();
        });
    }
}