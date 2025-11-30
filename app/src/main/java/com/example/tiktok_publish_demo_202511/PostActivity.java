package com.example.tiktok_publish_demo_202511;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private EditText etContent;
    private TextView tvCharCount;
    private TextView tvLocation;
    private LinearLayout btnLocation;
    private Button btnAiGenerate;

    private final String[] TOPICS = {
            "#美好生活", "#旅行日记", "#美食分享", "#技术宅",
    };

    private final String[] USERS = {
            "@用户1", "@用户2", "@用户3", "@用户4",
    };

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    List<Uri> selected = new ArrayList<>();
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            selected.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        selected.add(result.getData().getData());
                    }
                    adapter.addImages(selected);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_images);
        etContent = findViewById(R.id.et_content);
        tvCharCount = findViewById(R.id.tv_char_count);
        tvLocation = findViewById(R.id.tv_location);
        btnLocation = findViewById(R.id.btn_location);
        btnAiGenerate = findViewById(R.id.btn_ai_generate);
    }

    private void setupRecyclerView() {
        adapter = new ImageAdapter(this, new ImageAdapter.OnItemClickListener() {
            @Override
            public void onAddClick() {
                openGallery();
            }

            @Override
            public void onImageClick(int position, Uri uri) {
                Toast.makeText(PostActivity.this, "点击了图片预览", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() == ImageAdapter.TYPE_ADD) {
                    return makeMovementFlags(0, 0);
                }
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                if (source.getItemViewType() == ImageAdapter.TYPE_ADD || target.getItemViewType() == ImageAdapter.TYPE_ADD) {
                    return false;
                }
                adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupListeners() {
        // --- 核心逻辑：TextWatcher 实现整体删除和字数统计 ---
        etContent.addTextChangedListener(new TextWatcher() {
            // 用来标记即将被删除的 Span
            private ForegroundColorSpan spanToRemove = null;
            // 防止递归调用（因为我们在 afterTextChanged 里修改了 text）
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (isFormatting) return;

                // 检测删除操作：count > 0 (有字符减少) 且 after == 0 (没有新字符增加)
                if (count > 0 && after == 0 && s instanceof Spannable) {
                    Spannable spannable = (Spannable) s;
                    // 检查被删除的范围 [start, start+count] 是否触碰到了我们的颜色标签
                    ForegroundColorSpan[] spans = spannable.getSpans(start, start + count, ForegroundColorSpan.class);

                    for (ForegroundColorSpan span : spans) {
                        // 如果碰到了任意一个颜色标签，我们就标记它，准备在删除后把它整个端掉
                        // 这里可以加额外的颜色判断，但简单起见，所有颜色 Span 都视为标签
                        spanToRemove = span;
                        break;
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不需要处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                // 1. 处理整体删除逻辑
                if (spanToRemove != null) {
                    isFormatting = true; // 开启锁，防止死循环

                    int start = s.getSpanStart(spanToRemove);
                    int end = s.getSpanEnd(spanToRemove);

                    // 先移除 Span 样式，避免干扰
                    s.removeSpan(spanToRemove);

                    // 如果该 Span 还有剩余文本（start < end），说明用户只删了一部分
                    // 我们帮你把剩下的也删了
                    if (start >= 0 && end >= 0 && start < end) {
                        s.delete(start, end);
                    }

                    spanToRemove = null;
                    isFormatting = false; // 解锁
                }

                // 2. 字数统计与超限提示
                int length = s.length();
                tvCharCount.setText(length + "/2000");

                if (length > 2000) {
                    tvCharCount.setTextColor(Color.RED);
                    // 只有当刚好超过 2000 那个瞬间提示一次，或者每次输入都提示
                    // 这里做一个简单的去抖动逻辑：如果当前状态已经是红色了就不弹 Toast，避免刷屏
                    // 但为了演示效果，我们简单判断一下
                    // 注意：afterTextChanged 会频繁触发，不建议在这里疯狂弹 Toast
                } else {
                    tvCharCount.setTextColor(Color.parseColor("#666666"));
                }

                // 专门检测是否超限并提示
                if (length > 2000 && !isToastShown) {
                    Toast.makeText(PostActivity.this, "字数已超过 2000 字上限", Toast.LENGTH_SHORT).show();
                    isToastShown = true; // 标记已提示
                } else if (length <= 2000) {
                    isToastShown = false; // 重置
                }
            }
        });

        findViewById(R.id.btn_topic).setOnClickListener(v -> showTopicDialog());
        findViewById(R.id.btn_at_user).setOnClickListener(v -> showUserDialog());

        btnLocation.setOnClickListener(v -> getCurrentLocation());
        btnAiGenerate.setOnClickListener(v -> generateAiCaption());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // 用于控制 Toast 不频繁弹出
    private boolean isToastShown = false;

    private void showTopicDialog() {
        new AlertDialog.Builder(this)
                .setTitle("选择话题")
                .setItems(TOPICS, (dialog, which) -> {
                    insertStyledText(TOPICS[which] + " ", Color.parseColor("#FACE15"));
                })
                .show();
    }

    private void showUserDialog() {
        new AlertDialog.Builder(this)
                .setTitle("选择好友")
                .setItems(USERS, (dialog, which) -> {
                    insertStyledText(USERS[which] + " ", Color.parseColor("#2B5CFF"));
                })
                .show();
    }

    private void insertStyledText(String text, int color) {
        int start = Math.max(etContent.getSelectionStart(), 0);
        int end = Math.max(etContent.getSelectionEnd(), 0);

        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(
                new ForegroundColorSpan(color),
                0,
                text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        etContent.getText().replace(Math.min(start, end), Math.max(start, end), spannableString);
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
                return;
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImageLauncher.launch(intent);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
            return;
        }

        tvLocation.setText("定位中...");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    LocationManager.GPS_PROVIDER,
                    new CancellationSignal(),
                    ContextCompat.getMainExecutor(this),
                    new androidx.core.util.Consumer<Location>() {
                        @Override
                        public void accept(Location location) {
                            if (location != null) {
                                updateLocationUI(location);
                            } else {
                                Location lastKnown = null;
                                try {
                                    lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                } catch (SecurityException e) {
                                    e.printStackTrace();
                                }

                                if (lastKnown != null) {
                                    updateLocationUI(lastKnown);
                                } else {
                                    tvLocation.setText("无法获取位置，请打开GPS");
                                }
                            }
                        }
                    }
            );

        } catch (Exception e) {
            e.printStackTrace();
            tvLocation.setText("定位服务异常");
        }
    }

    private void updateLocationUI(Location loc) {
        new Thread(() -> {
            String locationText;
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            try {
                List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (address.getLocality() != null) sb.append(address.getLocality());
                    else if (address.getAdminArea() != null) sb.append(address.getAdminArea());

                    if (address.getSubLocality() != null) {
                        if (sb.length() > 0) sb.append(" · ");
                        sb.append(address.getSubLocality());
                    }

                    if (sb.length() == 0 && address.getFeatureName() != null) sb.append(address.getFeatureName());

                    if (sb.length() > 0) locationText = "📍 " + sb.toString();
                    else locationText = "📍 " + String.format("%.2f, %.2f", loc.getLatitude(), loc.getLongitude());

                } else {
                    locationText = "📍 未知地名 (" + String.format("%.2f", loc.getLatitude()) + ")";
                }
            } catch (IOException e) {
                e.printStackTrace();
                locationText = "📍 网络异常，仅显示坐标: " + String.format("%.1f, %.1f", loc.getLatitude(), loc.getLongitude());
            }

            String finalLocationText = locationText;
            runOnUiThread(() -> {
                tvLocation.setText(finalLocationText);
                tvLocation.setTextColor(0xFFFFFFFF);
            });

        }).start();
    }

    private static class AiResult {
        String description;
        List<String> hashtags;

        public AiResult(String description, List<String> hashtags) {
            this.description = description;
            this.hashtags = hashtags;
        }
    }

    interface AiCallback {
        void onSuccess(AiResult result);
        void onError(String error);
    }

    private void generateAiCaption() {
        List<Uri> images = adapter.getData();
        if (images.isEmpty()) {
            Toast.makeText(this, "请先上传图片供AI分析", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiGenerate.setText("分析中...");
        btnAiGenerate.setEnabled(false);

        simulateAiAnalysis(images, new AiCallback() {
            @Override
            public void onSuccess(AiResult result) {
                insertStyledText(result.description + "\n", Color.WHITE);
                for (String tag : result.hashtags) {
                    insertStyledText(tag + " ", Color.parseColor("#FACE15"));
                }
                btnAiGenerate.setText("✨ AI配文");
                btnAiGenerate.setEnabled(true);
                Toast.makeText(PostActivity.this, "AI文案已生成", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                btnAiGenerate.setText("✨ AI配文");
                btnAiGenerate.setEnabled(true);
                Toast.makeText(PostActivity.this, "生成失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void simulateAiAnalysis(List<Uri> imageUris, AiCallback callback) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean isLandscape = Math.random() > 0.5;
            String description;
            List<String> tags = new ArrayList<>();

            if (isLandscape) {
                description = "被眼前的风景治愈了，风很温柔，阳光正好。记录下这一刻的美好时光。";
                tags.add("#风景党");
                tags.add("#治愈系");
                tags.add("#生活碎片");
            } else {
                description = "于平淡日子里寻找一些小确幸，保持热爱，奔赴山海。";
                tags.add("#今日份开心");
                tags.add("#日常");
                tags.add("#plog");
            }

            AiResult result = new AiResult(description, tags);
            callback.onSuccess(result);

        }, 1500);
    }
}