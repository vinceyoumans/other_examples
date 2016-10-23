package com.techlatin.hm004;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Detector.ImageListener{

    public static final String LOG_TAG = "Affectiva";
    public static final int PICK_IMAGE = 100;

    ImageView imageView;
    TextView[] metricScoreTextViews;

    LinearLayout metricsContainer;  //android


    PhotoDetector detector;     //you
    Bitmap bitmap = null;       //android
    Frame.BitmapFrame frame;    //you




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        Log.e(LOG_TAG, "onCreate");

        try {
            loadInitialImage();
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "loading initial image", ioe);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(LOG_TAG, "onResume");

    }

    void loadInitialImage() throws IOException {
        if (bitmap == null) {
            bitmap = getBitmapFromAsset(this, "images/default.jpg");
        }
        setAndProcessBitmap(Frame.ROTATE.NO_ROTATION, false);
    }

    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(LOG_TAG, "onDestroy");
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.e(LOG_TAG, "onPause");

    }


    void stopDetector() {
        if (detector.isRunning()) {
            detector.stop();
        }
    }


    void initUI() {
        metricsContainer = (LinearLayout) findViewById(R.id.metrics_container);
        metricScoreTextViews = MetricsPanelCreator.createScoresTextViews();
        MetricsPanelCreator.populateMetricsContainer(metricsContainer,metricScoreTextViews,this);

        imageView = (ImageView) findViewById(R.id.image_view);
    }


    public Bitmap getBitmapFromAsset(Context context, String filePath) throws IOException {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap;
        istr = assetManager.open(filePath);
        bitmap = BitmapFactory.decodeStream(istr);

        return bitmap;
    }


    public Bitmap getBitmapFromUri(Uri uri) throws FileNotFoundException {
        InputStream istr;
        Bitmap bitmap;
        istr = getContentResolver().openInputStream(uri);
        bitmap = BitmapFactory.decodeStream(istr);

        return bitmap;
    }


    public void select_new_image(View view) {
        Intent gallery =
                new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    void setAndProcessBitmap(Frame.ROTATE rotation, boolean isExpectingFaceDetection) {
        if (bitmap == null) {
            return;
        }

        switch (rotation) {
            case BY_90_CCW:
                bitmap = Frame.rotateImage(bitmap,-90);
                break;
            case BY_90_CW:
                bitmap = Frame.rotateImage(bitmap,90);
                break;
            case BY_180:
                bitmap = Frame.rotateImage(bitmap,180);
                break;
            default:
                //keep bitmap as it is
        }

        frame = new Frame.BitmapFrame(bitmap, Frame.COLOR_FORMAT.UNKNOWN_TYPE);

        detector = new PhotoDetector(this,1, Detector.FaceDetectorMode.LARGE_FACES );
        detector.setDetectAllEmotions(true);
        detector.setDetectAllExpressions(true);
        detector.setDetectAllAppearances(true);
        detector.setImageListener(this);

        startDetector();
        detector.process(frame);
        stopDetector();

    }


    @SuppressWarnings("SuspiciousNameCombination")
    Bitmap drawCanvas(int width, int height, PointF[] points, Frame frame, Paint circlePaint) {
        if (width <= 0 || height <= 0) {
            return null;
        }

        Bitmap blackBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        blackBitmap.eraseColor(Color.BLACK);
        Canvas c = new Canvas(blackBitmap);

        Frame.ROTATE frameRot = frame.getTargetRotation();
        Bitmap bitmap;

        int frameWidth = frame.getWidth();
        int frameHeight = frame.getHeight();
        int canvasWidth = c.getWidth();
        int canvasHeight = c.getHeight();
        int scaledWidth;
        int scaledHeight;
        int topOffset = 0;
        int leftOffset= 0;
        float radius = (float)canvasWidth/100f;

        if (frame instanceof Frame.BitmapFrame) {
            bitmap = ((Frame.BitmapFrame)frame).getBitmap();
        } else { //frame is ByteArrayFrame
            byte[] pixels = ((Frame.ByteArrayFrame)frame).getByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(pixels);
            bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
        }

        if (frameRot == Frame.ROTATE.BY_90_CCW || frameRot == Frame.ROTATE.BY_90_CW) {
            int temp = frameWidth;
            frameWidth = frameHeight;
            frameHeight = temp;
        }

        float frameAspectRatio = (float)frameWidth/(float)frameHeight;
        float canvasAspectRatio = (float) canvasWidth/(float) canvasHeight;
        if (frameAspectRatio > canvasAspectRatio) { //width should be the same
            scaledWidth = canvasWidth;
            scaledHeight = (int)((float)canvasWidth / frameAspectRatio);
            topOffset = (canvasHeight - scaledHeight)/2;
        } else { //height should be the same
            scaledHeight = canvasHeight;
            scaledWidth = (int) ((float)canvasHeight*frameAspectRatio);
            leftOffset = (canvasWidth - scaledWidth)/2;
        }

        float scaling = (float)scaledWidth/(float)frame.getOriginalBitmapFrame().getWidth();

        Matrix matrix = new Matrix();
        matrix.postRotate((float)frameRot.toDouble());
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,frameWidth,frameHeight,matrix,false);
        c.drawBitmap(rotatedBitmap,null,new Rect(leftOffset,topOffset,leftOffset+scaledWidth,topOffset+scaledHeight),null);


        if (points != null) {
            //Save our own reference to the list of points, in case the previous reference is overwritten by the main thread.

            for (PointF point : points) {

                //transform from the camera coordinates to our screen coordinates
                //The camera preview is displayed as a mirror, so X pts have to be mirrored back.
                float x = (point.x * scaling) + leftOffset;
                float y = (point.y * scaling) + topOffset;

                c.drawCircle(x, y, radius, circlePaint);
            }
        }

        return blackBitmap;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(LOG_TAG, "onActivityForResult");
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {

            Uri imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);

            } catch (IOException e) {
                Toast.makeText(this,"Unable to open image.",Toast.LENGTH_LONG).show();
            }

            setAndProcessBitmap(Frame.ROTATE.NO_ROTATION, true);

        } else {
            Toast.makeText(this,"No image selected.",Toast.LENGTH_LONG).show();
        }
    }





    public void rotate_left(View view) {
        setAndProcessBitmap(Frame.ROTATE.BY_90_CCW, true);
    }

    public void rotate_right(View view) {
        setAndProcessBitmap(Frame.ROTATE.BY_90_CW,true);
    }

    @Override
    public void onImageResults(List<Face> faces, Frame image, float timestamp) {

        PointF[] points = null;

        if (faces != null && faces.size() > 0) {
            Face face = faces.get(0);
            setMetricTextViewText(face);
            points = face.getFacePoints();
        } else {
            for (int n = 0; n < MetricsManager.getTotalNumMetrics(); n++) {
                metricScoreTextViews[n].setText("---");
            }
        }

        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.RED);
        Bitmap imageBitmap = drawCanvas(imageView.getWidth(),imageView.getHeight(),points,image,circlePaint);
        if (imageBitmap != null)
            imageView.setImageBitmap(imageBitmap);
    }


    private void setMetricTextViewText(Face face) {
        // set the text for all the numeric metrics (scored or measured)
        for (int n = 0; n < MetricsManager.getTotalNumNumericMetrics(); n++) {
            metricScoreTextViews[n].setText(String.format("%.3f", getScore(n, face)));
        }

        // set the text for the appearance metrics
        String textValue="";
        switch (face.appearance.getGender()) {
            case UNKNOWN:
                textValue = "unknown";
                break;
            case FEMALE:
                textValue = "female";
                break;
            case MALE:
                textValue = "male";
                break;
        }
        metricScoreTextViews[MetricsManager.GENDER].setText(textValue);

        switch (face.appearance.getAge()) {
            case AGE_UNKNOWN:
                textValue = "unknown";
                break;
            case AGE_UNDER_18:
                textValue = "under 18";
                break;
            case AGE_18_24:
                textValue = "18-24";
                break;
            case AGE_25_34:
                textValue = "25-34";
                break;
            case AGE_35_44:
                textValue = "35-44";
                break;
            case AGE_45_54:
                textValue = "45-54";
                break;
            case AGE_55_64:
                textValue = "55-64";
                break;
            case AGE_65_PLUS:
                textValue = "65+";
                break;
        }
        metricScoreTextViews[MetricsManager.AGE].setText(textValue);

        switch (face.appearance.getEthnicity()) {
            case UNKNOWN:
                textValue = "unknown";
                break;
            case CAUCASIAN:
                textValue = "caucasian";
                break;
            case BLACK_AFRICAN:
                textValue = "black african";
                break;
            case EAST_ASIAN:
                textValue = "east asian";
                break;
            case SOUTH_ASIAN:
                textValue = "south asian";
                break;
            case HISPANIC:
                textValue = "hispanic";
                break;
        }
        metricScoreTextViews[MetricsManager.ETHNICITY].setText(textValue);
    }






    @Override
    public void onImageResults(List<Face> faces, Frame image, float timestamp) {

        PointF[] points = null;

        if (faces != null && faces.size() > 0) {
            Face face = faces.get(0);
            setMetricTextViewText(face);
            points = face.getFacePoints();
        } else {
            for (int n = 0; n < MetricsManager.getTotalNumMetrics(); n++) {
                metricScoreTextViews[n].setText("---");
            }
        }

        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.RED);
        Bitmap imageBitmap = drawCanvas(imageView.getWidth(),imageView.getHeight(),points,image,circlePaint);
        if (imageBitmap != null)
            imageView.setImageBitmap(imageBitmap);
    }


    private void setMetricTextViewText(Face face) {
        // set the text for all the numeric metrics (scored or measured)
        for (int n = 0; n < MetricsManager.getTotalNumNumericMetrics(); n++) {
            metricScoreTextViews[n].setText(String.format("%.3f", getScore(n, face)));
        }

        // set the text for the appearance metrics
        String textValue="";
        switch (face.appearance.getGender()) {
            case UNKNOWN:
                textValue = "unknown";
                break;
            case FEMALE:
                textValue = "female";
                break;
            case MALE:
                textValue = "male";
                break;
        }
        metricScoreTextViews[MetricsManager.GENDER].setText(textValue);

        switch (face.appearance.getAge()) {
            case AGE_UNKNOWN:
                textValue = "unknown";
                break;
            case AGE_UNDER_18:
                textValue = "under 18";
                break;
            case AGE_18_24:
                textValue = "18-24";
                break;
            case AGE_25_34:
                textValue = "25-34";
                break;
            case AGE_35_44:
                textValue = "35-44";
                break;
            case AGE_45_54:
                textValue = "45-54";
                break;
            case AGE_55_64:
                textValue = "55-64";
                break;
            case AGE_65_PLUS:
                textValue = "65+";
                break;
        }
        metricScoreTextViews[MetricsManager.AGE].setText(textValue);

        switch (face.appearance.getEthnicity()) {
            case UNKNOWN:
                textValue = "unknown";
                break;
            case CAUCASIAN:
                textValue = "caucasian";
                break;
            case BLACK_AFRICAN:
                textValue = "black african";
                break;
            case EAST_ASIAN:
                textValue = "east asian";
                break;
            case SOUTH_ASIAN:
                textValue = "south asian";
                break;
            case HISPANIC:
                textValue = "hispanic";
                break;
        }
        metricScoreTextViews[MetricsManager.ETHNICITY].setText(textValue);
    }




    float getScore(int metricCode, Face face) {

        float score;

        switch (metricCode) {
            case MetricsManager.ANGER:
                score = face.emotions.getAnger();
                break;
            case MetricsManager.CONTEMPT:
                score = face.emotions.getContempt();
                break;
            case MetricsManager.DISGUST:
                score = face.emotions.getDisgust();
                break;
            case MetricsManager.FEAR:
                score = face.emotions.getFear();
                break;
            case MetricsManager.JOY:
                score = face.emotions.getJoy();
                break;
            case MetricsManager.SADNESS:
                score = face.emotions.getSadness();
                break;
            case MetricsManager.SURPRISE:
                score = face.emotions.getSurprise();
                break;
            case MetricsManager.ATTENTION:
                score = face.expressions.getAttention();
                break;
            case MetricsManager.BROW_FURROW:
                score = face.expressions.getBrowFurrow();
                break;
            case MetricsManager.BROW_RAISE:
                score = face.expressions.getBrowRaise();
                break;
            case MetricsManager.CHEEK_RAISE:
                score = face.expressions.getCheekRaise();
                break;
            case MetricsManager.CHIN_RAISE:
                score = face.expressions.getChinRaise();
                break;
            case MetricsManager.DIMPLER:
                score = face.expressions.getDimpler();
                break;
            case MetricsManager.ENGAGEMENT:
                score = face.emotions.getEngagement();
                break;
            case MetricsManager.EYE_CLOSURE:
                score = face.expressions.getEyeClosure();
                break;
            case MetricsManager.EYE_WIDEN:
                score = face.expressions.getEyeWiden();
                break;
            case MetricsManager.INNER_BROW_RAISE:
                score = face.expressions.getInnerBrowRaise();
                break;
            case MetricsManager.JAW_DROP:
                score = face.expressions.getJawDrop();
                break;
            case MetricsManager.LID_TIGHTEN:
                score = face.expressions.getLidTighten();
                break;
            case MetricsManager.LIP_DEPRESSOR:
                score = face.expressions.getLipCornerDepressor();
                break;
            case MetricsManager.LIP_PRESS:
                score = face.expressions.getLipPress();
                break;
            case MetricsManager.LIP_PUCKER:
                score = face.expressions.getLipPucker();
                break;
            case MetricsManager.LIP_STRETCH:
                score = face.expressions.getLipStretch();
                break;
            case MetricsManager.LIP_SUCK:
                score = face.expressions.getLipSuck();
                break;
            case MetricsManager.MOUTH_OPEN:
                score = face.expressions.getMouthOpen();
                break;
            case MetricsManager.NOSE_WRINKLE:
                score = face.expressions.getNoseWrinkle();
                break;
            case MetricsManager.SMILE:
                score = face.expressions.getSmile();
                break;
            case MetricsManager.SMIRK:
                score = face.expressions.getSmirk();
                break;
            case MetricsManager.UPPER_LIP_RAISE:
                score = face.expressions.getUpperLipRaise();
                break;
            case MetricsManager.VALENCE:
                score = face.emotions.getValence();
                break;
            case MetricsManager.YAW:
                score = face.measurements.orientation.getYaw();
                break;
            case MetricsManager.ROLL:
                score = face.measurements.orientation.getRoll();
                break;
            case MetricsManager.PITCH:
                score = face.measurements.orientation.getPitch();
                break;
            case MetricsManager.INTER_OCULAR_DISTANCE:
                score = face.measurements.getInterocularDistance();
                break;
            default:
                score = Float.NaN;
                break;
        }
        return score;
    }

}
