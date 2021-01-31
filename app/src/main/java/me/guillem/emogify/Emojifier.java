package me.guillem.emogify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;


/**
 * * Created by Guillem on 29/01/21.
 */
public class Emojifier {
    private static FirebaseVisionImage image;
    private static FirebaseVisionFaceDetector detector;
    private static final float EMOJI_SCALE_FACTOR = .9f;
    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;


    public static Bitmap detectFace(Context context, Bitmap bitmap) {
        FirebaseVisionFaceDetectorOptions options=
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .setTrackingEnabled(true)
                        .build();
        final Bitmap[] resultBitmap = {bitmap};
        try {
            image =FirebaseVisionImage.fromBitmap(bitmap);
            detector= FirebaseVision.getInstance()
                    .getVisionFaceDetector(options);
        } catch (Exception e) {
            e.printStackTrace();
        }



        detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {


                String resultText = "";
                int i = 1;
                if (firebaseVisionFaces.size() == 0) {
                    Toast.makeText(context, "No Faces", Toast.LENGTH_SHORT).show();

                } else {
                    for (FirebaseVisionFace face : firebaseVisionFaces) {


                        Bitmap emojiBitmap;
                        switch (whichEmoji(face)) {
                            case SMILE:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.smile);
                                break;
                            case FROWN:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.frown);
                                break;
                            case LEFT_WINK:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.leftwink);
                                break;
                            case RIGHT_WINK:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.rightwink);
                                break;
                            case LEFT_WINK_FROWN:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.leftwinkfrown);
                                break;
                            case RIGHT_WINK_FROWN:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.rightwinkfrown);
                                break;
                            case CLOSED_EYE_SMILE:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.closed_smile);
                                break;
                            case CLOSED_EYE_FROWN:
                                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                        R.drawable.closed_frown);
                                break;
                            default:
                                emojiBitmap = null;
                                Toast.makeText(context, R.string.no_emoji, Toast.LENGTH_SHORT).show();
                        }

                         resultBitmap[0] = addBitmapToFace(resultBitmap[0], emojiBitmap, face);

/*                        resultText = resultText.concat("\n" + i + ".")
                                .concat("\nSmile" + face.getSmilingProbability() * 100 + "%"
                                        + "  --  " + face.getLeftEyeOpenProbability());
                        i++;*/
                    }
                }


/*                    Bundle bundle = new Bundle();
                    bundle.putString(FaceDetection.RESULT_TEXT,resultText);
                    DialogFragment resultDialog = new ResultDialog();
                    resultDialog.setArguments(bundle);
                    resultDialog.setCancelable(false);
                    resultDialog.show(getSupportFragmentManager(), FaceDetection.RESULT_DIALOG);*/
            }
        });
        return resultBitmap[resultBitmap.length-1];
    }
    private static Emoji whichEmoji(FirebaseVisionFace face) {

        boolean smiling = face.getSmilingProbability() > SMILING_PROB_THRESHOLD;

        boolean leftEyeClosed = face.getLeftEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;
        boolean rightEyeClosed = face.getRightEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;

        // Determine and log the appropriate emoji
        Emoji emoji;
        if(smiling) {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK;
            }  else if(rightEyeClosed && !leftEyeClosed){
                emoji = Emoji.RIGHT_WINK;
            } else if (leftEyeClosed){
                emoji = Emoji.CLOSED_EYE_SMILE;
            } else {
                emoji = Emoji.SMILE;
            }
        } else {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK_FROWN;
            }  else if(rightEyeClosed && !leftEyeClosed){
                emoji = Emoji.RIGHT_WINK_FROWN;
            } else if (leftEyeClosed){
                emoji = Emoji.CLOSED_EYE_FROWN;
            } else {
                emoji = Emoji.FROWN;
            }
        }
        return emoji;
    }

    // Enum for all possible Emojis
    private enum Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }

    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, FirebaseVisionFace face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getBoundingBox().width() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getBoundingBox().centerX() + face.getBoundingBox().width() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getBoundingBox().centerY() + face.getBoundingBox().height() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }

}
