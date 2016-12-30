package jp.techacademy.takafumi.matsushima.qa_app;

import android.*;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class QuestionSendActivity extends AppCompatActivity implements View.OnClickListener, DatabaseReference.CompletionListener {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int CHOOSER_REQUEST_CODE = 100;

    private ProgressDialog mProgress;
    private EditText mTitleText;
    private EditText mBodyText;
    private ImageView mImageView;
    private Button mSendButton;

    private int mGenre;
    private Uri mPictureUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_send);

        // 渡ってきたジャンルの番号GET
        Bundle extras = getIntent().getExtras();
        mGenre = extras.getInt("genre");

        setTitle("質問作成");

        mTitleText = (EditText) findViewById(R.id.titleText);
        mBodyText = (EditText) findViewById(R.id.bodyText);

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(this);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setOnClickListener(this);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("投稿してる...");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != RESULT_OK) {
                if (mPictureUri != null) {
                    getContentResolver().delete(mPictureUri, null, null);
                    mPictureUri = null;
                }
                return;
            }

            // 画像を取得
            Uri uri = (data == null || data.getData() == null) ? mPictureUri : data.getData();

            // URIからBitmapを取得
            Bitmap image;
            try {
                ContentResolver contentResolver = getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(uri);
                image = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            } catch (Exception e) {
                return;
            }

            // 取得したBimapの長辺を500ピクセルに変更
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            float scale = Math.min((float) 500 / imageWidth, (float) 500 / imageHeight); // (1)

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            Bitmap resizedImage =  Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true);

            // BitmapをImageViewに設定
            mImageView.setImageBitmap(resizedImage);

            mPictureUri = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mImageView) {
            // パーミッションの許可状態を確認
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 許可されている
                    showChooser();
                } else {
                    // 許可なし=>許可ダイアログを表示
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);

                    return;
                }
            } else {
                showChooser();
            }
        } else if (v == mSendButton) {
            // キーボードが出てたら
            InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            // ↑　FirebaseDatabaseのインスタンスを取得
            DatabaseReference refEmail = database.getReference("email");
            String st = (String)refEmail.toString();
            Log.d("TESTJAVA",st);
            // ↑　(Email)のデータベースへの参照を取得
            String  email = "TEST";

            refEmail.setValue(email);
            // ↑　setValueを呼び、値をデータベースに即時登録


            mProgress.show();
            Log.d("TESTJAVA","PASS");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したとき
                    showChooser();
                }
                return;
            }
        }
    }

    private void showChooser() {
        // ギャラリーから選択するIntent
        Intent garallyIntent = new Intent(Intent.ACTION_GET_CONTENT);
        garallyIntent.setType("image/*");
        garallyIntent.addCategory(Intent.CATEGORY_OPENABLE);

        // カメラで撮影するIntent
        String filename = System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        mPictureUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        Intent chooserIntent = Intent.createChooser(garallyIntent, "画像を取得");

        // EXTRA_INITIAL_INTENTS にカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE);
    }

    @Override
    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        mProgress.dismiss();

        if (databaseError == null) {
            finish();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "投稿に失敗", Snackbar.LENGTH_LONG).show();
        }
    }
}