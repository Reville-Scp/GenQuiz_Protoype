package com.example.genquizlayout;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PdfconverterPageActivity extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseUser user;

    Uri selectedFileUri;
    byte[] convertedPdfBytes;
    String convertedPdfName = "converted-file.pdf";

    TextView selectedFileText;
    TextView conversionStatusText;
    Button convertPdfBtn;
    ImageView statusIcon;
    ProgressBar conversionProgress;

    // Opens the phone file picker and receives the file chosen by the user.
    ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    selectedFileText.setText(getFileName(selectedFileUri));
                    conversionStatusText.setText("Ready to convert.");
                    statusIcon.setImageResource(R.drawable.ic_upload);
                    convertPdfBtn.setEnabled(true);
                }
            }
    );

    // Opens the phone save dialog and receives the location where the PDF will be saved.
    ActivityResultLauncher<Intent> savePdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    savePdfToPhone(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pdfconverter_page);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null){
            Intent intent = new Intent(
                    getApplicationContext(),
                    LoginPageActivity.class
            );

            startActivity(intent);
            finish();
        }

        ImageView leftIcon = findViewById(R.id.leftIcon);

        leftIcon.setImageResource(
                R.drawable.ic_back
        );

        leftIcon.setOnClickListener(v -> {

            finish();

        });

        ImageView menuBtn = findViewById(R.id.menuBtn);

        menuBtn.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(
                    PdfconverterPageActivity.this,
                    menuBtn
            );

            popupMenu.getMenu().add("Dark Mode");
            popupMenu.getMenu().add("Logout");

            popupMenu.setOnMenuItemClickListener(item -> {

                String title = item.getTitle().toString();

                if(title.equals("Dark Mode")) {

                    int currentMode = AppCompatDelegate.getDefaultNightMode();

                    if(currentMode == AppCompatDelegate.MODE_NIGHT_YES){

                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO
                        );

                    }
                    else{

                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES
                        );

                    }

                }

                else if(title.equals("Logout")) {

                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(
                            PdfconverterPageActivity.this,
                            LoginPageActivity.class
                    );

                    startActivity(intent);

                    finish();

                }

                return true;

            });

            popupMenu.show();

        });

        selectedFileText = findViewById(R.id.selectedFileText);
        conversionStatusText = findViewById(R.id.conversionStatusText);
        convertPdfBtn = findViewById(R.id.convertPdfBtn);
        statusIcon = findViewById(R.id.statusIcon);
        conversionProgress = findViewById(R.id.conversionProgress);

        View uploadFileCard = findViewById(R.id.uploadFileCard);

        uploadFileCard.setOnClickListener(v -> openFilePicker());
        convertPdfBtn.setOnClickListener(v -> convertSelectedFile());

        LinearLayout homeNav = findViewById(R.id.homeNav);

        homeNav.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PdfconverterPageActivity.this,
                    MainActivity.class
            );

            startActivity(intent);

        });

        LinearLayout pdfconNav = findViewById(R.id.pdfconNav);

        pdfconNav.setOnClickListener(v -> {

            // Already on the PDF Converter page, so the center nav opens the file picker directly.
            openFilePicker();

        });

        LinearLayout quizzezNav = findViewById(R.id.quizzezNav);

        quizzezNav.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PdfconverterPageActivity.this,
                    QuizPageActivity.class
            );

            startActivity(intent);

        });

    }

    private void openFilePicker() {
        // Let the user choose common document files that Gotenberg can convert.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain"
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void convertSelectedFile() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please choose a file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        setConversionLoading(true);
        conversionStatusText.setText("Converting file to PDF...");

        sendFileToBackend(selectedFileUri);
    }

    private void sendFileToBackend(Uri fileUri) {
        try {
            // Read the chosen document and prepare it for Retrofit upload.
            byte[] fileBytes = readBytesFromUri(fileUri);
            RequestBody requestFile = RequestBody.create(
                    fileBytes,
                    MediaType.parse("application/octet-stream")
            );

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file",
                    getFileName(fileUri),
                    requestFile
            );

            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.convertToPdf(filePart);

            // enqueue runs the request in the background while the UI stays responsive.
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            convertedPdfBytes = response.body().bytes();
                            convertedPdfName = makePdfFileName(getFileName(fileUri));
                            setConversionLoading(false);
                            conversionStatusText.setText("Converted. Choose where to save the PDF.");
                            openSavePdfPicker();
                        } else {
                            conversionStatusText.setText("Conversion failed.");
                            setConversionLoading(false);
                        }
                    } catch (Exception e) {
                        conversionStatusText.setText("Response error.");
                        setConversionLoading(false);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    conversionStatusText.setText("Backend connection failed.");
                    setConversionLoading(false);
                }
            });

        } catch (Exception e) {
            conversionStatusText.setText("File error.");
            setConversionLoading(false);
        }
    }

    private byte[] readBytesFromUri(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        byte[] buffer = new byte[4096];
        int bytesRead;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Read the selected file little by little until the whole file is in memory.
        while (inputStream != null && (bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return outputStream.toByteArray();
    }

    private void openSavePdfPicker() {
        // ACTION_CREATE_DOCUMENT lets the user choose Downloads, Documents, or another folder.
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, convertedPdfName);
        savePdfLauncher.launch(intent);
    }

    private void savePdfToPhone(Uri uri) {
        try {
            // Write the PDF bytes into the location selected by the user.
            OutputStream outputStream = getContentResolver().openOutputStream(uri);

            if (outputStream != null) {
                outputStream.write(convertedPdfBytes);
                outputStream.close();
            }

            statusIcon.setImageResource(R.drawable.ic_check);
            conversionStatusText.setText("Done. Saved as " + convertedPdfName);
            setConversionLoading(false);
            Toast.makeText(this, "PDF saved successfully.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            conversionStatusText.setText("Save error: " + e.getMessage());
            setConversionLoading(false);
        }
    }

    private void setConversionLoading(boolean isLoading) {
        // This shows a spinner while the backend and Gotenberg are converting the selected file.
        convertPdfBtn.setEnabled(!isLoading);
        convertPdfBtn.setText(isLoading ? "Converting..." : "Convert to PDF");
        conversionProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private String getFileName(Uri uri) {
        String fileName = "selected-file";

        // Android gives us a Uri, so this asks Android for the original display name.
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex);
            }

            cursor.close();
        }

        return fileName;
    }

    private String makePdfFileName(String originalName) {
        // Example: "lesson.pptx" becomes "lesson.pdf".
        int dotIndex = originalName.lastIndexOf(".");

        if (dotIndex > 0) {
            return originalName.substring(0, dotIndex) + ".pdf";
        }

        return originalName + ".pdf";
    }
}
