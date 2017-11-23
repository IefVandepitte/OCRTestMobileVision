package be.avondschool.vi.ocrtestappgooglemobilevisionapi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileNotFoundException;


public class MainActivity2 extends AppCompatActivity {



    private TextView mScanresults;
    private Uri mImageUri;
    private TextRecognizer mDetector;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final int PHOTO_REQUEST = 10;
    private static final String LOG_TAG = "Text API";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button);
        mScanresults = (TextView) findViewById(R.id.results);
        if (savedInstanceState != null){
            mImageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            mScanresults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }
        mDetector = new TextRecognizer.Builder(getApplicationContext()).build();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity2.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_PERMISSION);
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){








                    takePicture();
                } else {
                    Toast.makeText(MainActivity2.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }

        }
    }

    private void takePicture() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "ocr_picture.jpg");
        mImageUri = FileProvider.getUriForFile(MainActivity2.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(intent, PHOTO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK){
            launchMediaScanIntent();
        }
        try {
            Bitmap bitmap = decodeBitmapUri(this, mImageUri);
            if (mDetector.isOperational() && bitmap != null){
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = mDetector.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++){
                    //extract scanned blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    for (Text line : tBlock.getComponents()){
                        //extract scanned lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text element : line.getComponents()){
                            // extract scanned text words here
                            words = words + element.getValue() + ", ";

                        }
                    }
                }
                if (textBlocks.size() == 0){
                    mScanresults.setText("Scan Failed: Found nothing to scan");
                } else {
                    mScanresults.setText(mScanresults.getText() + "Blocks: " + "\n");
                    mScanresults.setText(mScanresults.getText() + blocks + "\n");
                    mScanresults.setText(mScanresults.getText() + "---------" + "\n");
                    mScanresults.setText(mScanresults.getText() + "Lines: " + "\n");
                    mScanresults.setText(mScanresults.getText() + lines + "\n");
                    mScanresults.setText(mScanresults.getText() + "---------" + "\n");
                    mScanresults.setText(mScanresults.getText() + "Words: " + "\n");
                    mScanresults.setText(mScanresults.getText() + words + "\n");
                    mScanresults.setText(mScanresults.getText() + "---------" + "\n");
                }
            } else {
                mScanresults.setText("Could not set up the detector!");
            }
        } catch (Exception e){
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, e.toString());
        }

    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
        .openInputStream(uri), null, bmOptions);

    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(mImageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mImageUri != null){
            outState.putString(SAVED_INSTANCE_URI, mImageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, mScanresults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }
}
