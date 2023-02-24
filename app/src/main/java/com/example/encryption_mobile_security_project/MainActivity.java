
package com.example.encryption_mobile_security_project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    //initialize variable
    MaterialButton main_btn_select_picture;

    EditText main_edt_enter_string;
    ImageView main_img_image_view,main_img_share;
    String sImage;
    private static final int PICK_FROM_GALLERY = 1;

    Uri selectImage;
    Bitmap bitmap, bitmapForShare;

    FileOutputStream fileOutputStream;




    // for encrypt string 31.1.23
    // test work need to delete the tvVar1 from xml!!!
    // TextView tvVar1;
    //ClipboardManager cpb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        findViews();
        attachListeners();
    }


    private void findViews() {
        main_btn_select_picture = findViewById(R.id.main_btn_select_picture);
        main_img_image_view= findViewById(R.id.main_img_image_view);
        main_img_share = findViewById(R.id.main_img_share);
        main_edt_enter_string = findViewById(R.id.main_edt_enter_string);

        // for encrypt string 31.1.23
       // tvVar1 = findViewById(R.id.tvVar1);
       // cpb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private void attachListeners() {
        // select Picture from gallery
        main_btn_select_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check condition
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                    } else {
                        //select photo from gallery
                       selectPhoto();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        //share the image with the encrypted text
        //after we check if we have the string and an image
        main_img_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //convert edt to string
              //  String enterString = MainActivity.this.main_edt_enter_string.getText().toString();


                //19.2
                // check if image view is not null
                if(selectImage == null) {
                    Toast.makeText(MainActivity.this, "You must select a picture ", Toast.LENGTH_LONG).show();
                } else if (main_edt_enter_string.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "The string must contain characters ", Toast.LENGTH_LONG).show();
                }else {
                    shareImage();
                }


                // check if String is between 0-10
//                else if (enterString.length() <= 0 || enterString.length() >10) {
//                    Toast.makeText(MainActivity.this, "The string must contain more than 0 and not more than 10 characters ", Toast.LENGTH_LONG).show();








//                // for encrypt string 31.1.23
//                String temp =enterString;
//                // pass the string to the encryption
//                // algorithm and get the encrypted code
//                String rv = Encode.encode(temp);
//
//                // set the code to the edit text
//                tvVar1.setText(rv);





            }

        });
    }

    private void changeBitmapPixelsByEncryption(Bitmap bitmap, String str) {
        byte[] stringBytes = str.getBytes(StandardCharsets.UTF_8);
        int bitIndex = 0;
        for(int i = 0; i < stringBytes.length; i++) {
            int charByte = stringBytes[i];

            for(int j = 0; j < 8; j++) {
                int charBitValue =  charByte & 1;
                int imageRow = bitIndex / bitmap.getWidth();
                int imageCol = bitIndex % bitmap.getWidth();
                int imagePixelValue = bitmap.getPixel(imageCol, imageRow);

                byte imageRedComponent = (byte)((imagePixelValue >> 16) & 0xFF);
                byte newRedComponentValue
                        = (byte)(charBitValue == 1 && imagePixelValue % 2 != 0
                        ? imageRedComponent + 100
                        : imageRedComponent);
                int newPixelValue = (imagePixelValue & 0xFF00FFFF) | (newRedComponentValue << 16);
                bitmapForShare.setPixel(imageCol, imageRow, newPixelValue);
                charByte >>= 1;
                bitIndex++;
            }
        }
    }

    private void shareImage() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        BitmapDrawable drawable = (BitmapDrawable) main_img_image_view.getDrawable();
        bitmapForShare = drawable.getBitmap().copy(drawable.getBitmap().getConfig(), true);
        changeBitmapPixelsByEncryption(bitmapForShare,  main_edt_enter_string.getText().toString());

        File file = new File(getExternalCacheDir() + "/" + "Picture " + ".png");
        Intent intent;
        try {
            fileOutputStream = new FileOutputStream(file);
            bitmapForShare.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        startActivity(Intent.createChooser(intent, "share image via"));
    }



   //select image from gallery
    private void selectPhoto() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PICK_FROM_GALLERY:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   selectPhoto();
                } else {
                    //make toast permisions denaid
                    //do something like displaying a message that he didn`t allow the app to access gallery and you wont be able to let him select from gallery
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       //check condition
        if(resultCode == RESULT_OK && data != null){
            // result is ok
            // initial uri
            //selectImage is the picture that chose from gallery


            selectImage = data.getData();
           //befor-   Uri selectImage = data.getData();

            ImageDecoder.Source source = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                source = ImageDecoder.createSource(this.getContentResolver(),selectImage);
                try {
                    bitmap = ImageDecoder.decodeBitmap(source);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100, stream);
                    byte[] bytes = stream.toByteArray();
                    //this is the encrypt Picture string that we need to enter the string that the user write (input string)
                    sImage = Base64.encodeToString(bytes, Base64.DEFAULT);
                    //show the selectImage in imageView
                    main_img_image_view.setImageURI(selectImage);
                    Toast.makeText(this, "image selected", Toast.LENGTH_SHORT).show();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

        }
    }
}
