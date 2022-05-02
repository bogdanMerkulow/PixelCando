package pixel.cando.utils.mlkit;

import android.graphics.Bitmap;
import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.MlKitException;

import java.nio.ByteBuffer;

public interface VisionImageProcessor {

  void processBitmap(@NonNull Bitmap bitmap, @NonNull GraphicOverlay graphicOverlay);

  void stop();

}
