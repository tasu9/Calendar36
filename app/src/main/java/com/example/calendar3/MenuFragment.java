package com.example.calendar3;


import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException; // 追加
import java.text.SimpleDateFormat; // 追加
import java.util.Date; // 追加
import java.util.Locale; // 追加
import java.util.List; // 追加


public class MenuFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private ActivityResultLauncher<Intent> _cameraLauncher;
    private ActivityResultLauncher<String> _pickImageLauncher;
    private ImageView imageView;
    private Button buttonClearImage;
    private Button cameraButton;
    private Button pickImageButton;
    private Button inButton;
    private Bitmap bitmap;
    private com.example.calendar3.EventRepository EventRepository;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Fragment のレイアウトを膨らませる
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        //EventRepositoryの初期化
        EventRepository  = new EventRepository(requireContext()); // EventRepositoryの初期化

        // 各UI要素を初期化する
        buttonClearImage = view.findViewById(R.id.button_clear_image);
        imageView = view.findViewById(R.id.imageView);
        cameraButton = view.findViewById(R.id.button_take_picture);
        pickImageButton = view.findViewById(R.id.button_pick_image);
        inButton = view.findViewById(R.id.button_to_api);

        // 各ボタンにクリックリスナーを設定する
        buttonClearImage.setOnClickListener(v -> clearImage());
        cameraButton.setOnClickListener(v -> onCameraButtonClick());
        pickImageButton.setOnClickListener(v -> onPickImageButtonClick());
        inButton.setOnClickListener(v -> {
                if (bitmap != null) {
                    byte[] imgBytes = convertBitmapToByteArray(bitmap);
                    VisionHelper.detectTextFromByteArray(imgBytes, new VisionHelper.TextDetectionCallback() {
                        @Override
                        public void onTextDetected(String text) {
                            Log.d("MainActivity", "Detected text: " + text);
                        }

                        @Override
                        public void onDatesExtracted(List<String> dates) {
                            Log.d("MainActivity", "Extracted dates: " + dates);
                            addEventsToCalendar(dates); // 日付をカレンダーに追加
                        }

                        @Override
                        public void onAnnouncementsExtracted(List<String> announcements) {
                            Log.d("MainActivity", "Extracted announcements: " + announcements);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("MainActivity", "Error detecting text", e);
                        }
                    });
                } else {
                    Log.d("MainActivity", "画像が選択されていません。");
                }
            });

            // カメラアクティビティ結果ランチャーを登録する
        _cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Bundle extras = data.getExtras();
                            if (extras != null) {
                                Bitmap bitmap = (Bitmap) extras.get("data");
                                if (bitmap != null) {
                                    this.bitmap = bitmap; // グローバル変数にセット
                                    imageView.setImageBitmap(bitmap);
                                    imageView.setVisibility(View.VISIBLE);
                                    buttonClearImage.setVisibility(View.VISIBLE);
                                    inButton.setVisibility(View.VISIBLE);
                                    cameraButton.setVisibility(View.GONE);
                                    pickImageButton.setVisibility(View.GONE);
                                }
                            }
                        }
                    }
                }
        );

        // 画像選択アクティビティ結果ランチャーを登録する
        _pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleImageUri(uri);
                    }
                }
        );

        return view;
    }

    private void onPickImageButtonClick() {
        _pickImageLauncher.launch("image/*");
    }

    private void onCameraButtonClick() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            _cameraLauncher.launch(cameraIntent);
        }
    }

    private void clearImage() {
        imageView.setImageURI(null);
        imageView.setVisibility(View.GONE);
        buttonClearImage.setVisibility(View.GONE);
        cameraButton.setVisibility(View.VISIBLE);
        pickImageButton.setVisibility(View.VISIBLE);
        inButton.setVisibility(View.GONE);
        bitmap = null; // グローバル変数をクリア
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onCameraButtonClick();
            } else {
                Log.d("MenuFragment", "カメラのパーミッションが拒否されました。");
            }
        }
    }
    private void addEventsToCalendar(List<String> dates) {
        for (String dateStr : dates) {
            // 日付文字列をUNIXタイムスタンプに変換
            long timestamp = convertDateToTimestamp(dateStr);

            // 抽出した文字をカレンダーに追加
            EventRepository.addEvent(timestamp, "抽出された予定", "ここに説明を追加");
        }
        Log.d("MenuFragment", "予定がカレンダーに追加されました。");
    }
    private long convertDateToTimestamp(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            return date.getTime();
        } catch (ParseException e) {
            Log.e("MenuFragment", "日付の解析に失敗しました", e);
            return 0;
        }
    }



    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // PNG形式で圧縮
        return stream.toByteArray();
    }

    private Bitmap getScaledBitmap(Uri uri) {
        try {
            // ストリームを開いて画像のサイズを取得
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // サイズ取得のみ
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // サンプルサイズの計算
            final int REQUIRED_SIZE = 1024; // 必要なサイズに合わせて調整
            int width_tmp = options.outWidth;
            int height_tmp = options.outHeight;
            int scale = 1;

            while (width_tmp / 2 >= REQUIRED_SIZE && height_tmp / 2 >= REQUIRED_SIZE) {
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            // 実際に画像を読み込み、スケーリングする
            BitmapFactory.Options scaledOptions = new BitmapFactory.Options();
            scaledOptions.inSampleSize = scale;
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, scaledOptions);
            inputStream.close();

            return bitmap;
        } catch (Exception e) {
            Log.e("MenuFragment", "Error scaling image", e);
        }
        return null;
    }

    private void handleImageUri(Uri uri) {
        Bitmap bitmap = getScaledBitmap(uri);
        if (bitmap != null) {
            this.bitmap = bitmap; // グローバル変数にセット
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
            buttonClearImage.setVisibility(View.VISIBLE);
            inButton.setVisibility(View.VISIBLE);
            cameraButton.setVisibility(View.GONE);
            pickImageButton.setVisibility(View.GONE);
        }
    }
}
