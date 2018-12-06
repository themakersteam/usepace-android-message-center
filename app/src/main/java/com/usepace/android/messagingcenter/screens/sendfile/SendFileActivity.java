package com.usepace.android.messagingcenter.screens.sendfile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.sendbird.android.SendBird;
import com.usepace.android.messagingcenter.R;
import com.usepace.android.messagingcenter.screens.sendbird.SendBirdChatActivity;
import com.usepace.android.messagingcenter.utils.FileUtils;
import com.usepace.android.messagingcenter.utils.ImageUtils;
import java.io.File;
import java.io.IOException;

public class SendFileActivity extends AppCompatActivity {

    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int REQUEST_GALLERY_CAPTURE = 2;

    private Uri currentBitmap = null;
    private String currentPhotoPath = null;
    private Toolbar toolbar;
    private ImageView mainImage;
    private EditText captionText;
    private ImageView send;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        init();
        if (getIntent() != null && getIntent().hasExtra("ACTION")) {
            if (getIntent().getIntExtra("ACTION", REQUEST_IMAGE_CAPTURE) == REQUEST_IMAGE_CAPTURE) {
                dispatchTakePictureIntent();
                SendBird.setAutoBackgroundDetection(false);
            }
            else if (getIntent().getIntExtra("ACTION", REQUEST_IMAGE_CAPTURE) == REQUEST_GALLERY_CAPTURE) {
                dispatchGalleryIntent();
                SendBird.setAutoBackgroundDetection(false);
            }
        }
        else {
            finish();
        }
    }

    private void init() {
        toolbar = findViewById(R.id.toolbar);
        captionText = findViewById(R.id.edittext_caption_message);
        send = findViewById(R.id.img_send_image);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImage();
            }
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void sendImage() {
        Intent intent = new Intent();
        intent.setData(currentBitmap);
        if (captionText.getText().toString().replaceAll("\n", "").replaceAll(" ", "").length() > 0) {
            intent.putExtra("CAPTION", captionText.getText().toString());
        }
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = ImageUtils.createImageFile(this);
                currentPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        SendBirdChatActivity.PACKAGE_NAME + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
            else {
                Toast.makeText(this, "Camera Error ! ", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void dispatchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY_CAPTURE);
    }

    private void loadImage() {
        if (mainImage == null) {
            mainImage = findViewById(R.id.main_image_view);
        }
        mainImage.setImageURI(currentBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SendBird.setAutoBackgroundDetection(true);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (currentPhotoPath != null) {
                File file = new File(currentPhotoPath);
                currentBitmap = FileUtils.getImageContentUri(this, file);
                if (currentBitmap != null) {
                    loadImage();
                }
                else {
                    finish();
                }
            }
        }
        else if (requestCode == REQUEST_GALLERY_CAPTURE && resultCode == RESULT_OK) {
            if (data == null) {
                return;
            }
            currentBitmap = data.getData();
            loadImage();
        }
        else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
