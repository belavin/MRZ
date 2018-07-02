package flynn.pro.mrz;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


final class OcrInitAsyncTask extends AsyncTask<String, String, Boolean> {
  private static final String TAG = OcrInitAsyncTask.class.getSimpleName();

  private CaptureActivity activity;
  private Context context;
  private TessBaseAPI baseApi;
  private ProgressDialog dialog;
  private ProgressDialog indeterminateDialog;
  private final String languageCode;
  private String languageName;
  private int ocrEngineMode;

  public static final String DEFAULT_STORAGE_LOCATION = Environment.getExternalStorageDirectory().getPath();
  public static final String MRZ_FOLDER_RELATIVE = "/MRZ/";
  public static final String MRZ_STORAGE_LOCATION = DEFAULT_STORAGE_LOCATION + MRZ_FOLDER_RELATIVE;


  OcrInitAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, ProgressDialog dialog,
                   ProgressDialog indeterminateDialog, String languageCode, String languageName,
                   int ocrEngineMode) {
    this.activity = activity;
    this.context = activity.getBaseContext();
    this.baseApi = baseApi;
    this.dialog = dialog;
    this.indeterminateDialog = indeterminateDialog;
    this.languageCode = languageCode;
    this.languageName = languageName;
    this.ocrEngineMode = ocrEngineMode;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    dialog.setTitle("Please wait");
    dialog.setMessage("Checking for data installation...");
    dialog.setIndeterminate(false);
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    dialog.setCancelable(false);
    dialog.show();
    activity.setButtonVisibility(false);
  }


  protected Boolean doInBackground(String... params) {

      File f = new File(this.activity.getCacheDir()+"/MRZ_STORAGE_LOCATION/eng.traineddata");
      File folder = new File(this.activity.getCacheDir()+"/tessdata");
      if (!f.exists()) try {

          if (!f.exists() && !folder.mkdirs()) {
              Log.e(TAG, "Couldn't make directory " + folder);
              return false;
          }

          InputStream is = this.activity.getAssets().open("MRZ_STORAGE_LOCATION/eng.traineddata");
          FileOutputStream fos = new FileOutputStream(f);
          copyFile(is,fos);
          fos.close();
          is.close();
      } catch (Exception e) { throw new RuntimeException(e); }

    File f2 = new File(this.activity.getCacheDir()+"/MRZ_STORAGE_LOCATION/eng.user-patterns");
    File folder2 = new File(this.activity.getCacheDir()+"/tessdata");
    if (!f2.exists()) try {

      InputStream is = this.activity.getAssets().open("MRZ_STORAGE_LOCATION/eng.user-patterns");
      FileOutputStream fos = new FileOutputStream(f2);
      copyFile(is,fos);
      fos.close();
      is.close();
    } catch (Exception e) { throw new RuntimeException(e); }


    if (baseApi.init(this.activity.getCacheDir() + File.separator, languageCode, ocrEngineMode)) {
      try {
        dialog.dismiss();
      } catch (IllegalArgumentException e) {
      }
      return true;

    }
    return false;
  }


  private boolean copyFile(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int read;
    while((read = in.read(buffer)) != -1){
      out.write(buffer, 0, read);
    }
    return true;
  }



  @Override
  protected void onProgressUpdate(String... message) {
    super.onProgressUpdate(message);
    int percentComplete = 0;

    percentComplete = Integer.parseInt(message[1]);
    dialog.setMessage(message[0]);
    dialog.setProgress(percentComplete);
    dialog.show();
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);
    
    try {
      indeterminateDialog.dismiss();
    } catch (IllegalArgumentException e) {
    e.printStackTrace();
    }

    if (result) {
      activity.resumeOCR();
      activity.showLanguageName();
    } else {
      activity.showErrorMessage("Error", "Network is unreachable - cannot download language data. "
          + "Please enable network access and restart this app.");
    }
  }



}