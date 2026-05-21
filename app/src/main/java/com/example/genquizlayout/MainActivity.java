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

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;

    Uri selectedPdfUri;
    String selectedFileName = "selected.pdf";

    TextView fileText;
    TextView generateStatusText;
    Button generateBtn;
    ProgressBar generateProgress;

    // Opens the Android file picker and receives the PDF chosen by the user.
    ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedPdfUri = result.getData().getData();
                    selectedFileName = getFileName(selectedPdfUri);
                    fileText.setText(selectedFileName);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.home_page);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null){
            Intent intent = new Intent(getApplicationContext(), LoginPageActivity.class);
            startActivity(intent);
            finish();
        }

        fileText = findViewById(R.id.fileText);
        generateStatusText = findViewById(R.id.generateStatusText);
        generateBtn = findViewById(R.id.generateBtn);
        generateProgress = findViewById(R.id.generateProgress);

        Button uploadBtn = findViewById(R.id.uploadBtn);

        uploadBtn.setOnClickListener(v -> openPdfPicker());
        generateBtn.setOnClickListener(v -> generateQuizFromPdf());

        ImageView menuBtn = findViewById(R.id.menuBtn);

        menuBtn.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(
                    MainActivity.this,
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
                            MainActivity.this,
                            LoginPageActivity.class
                    );

                    startActivity(intent);

                    finish();

                }

                return true;

            });

            popupMenu.show();

        });

        LinearLayout pdfconvNav = findViewById(R.id.pdfconNav);

        pdfconvNav.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    PdfconverterPageActivity.class
            );

            startActivity(intent);

        });

        LinearLayout scoreNav = findViewById(R.id.quizzezNav);

        scoreNav.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    QuizPageActivity.class
            );

            startActivity(intent);

        });

    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(intent);
    }

    private void generateQuizFromPdf() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, "Please upload a PDF first.", Toast.LENGTH_SHORT).show();
            return;
        }

        setQuizLoading(true);

        sendPdfToBackend(selectedPdfUri);
    }

    private void sendPdfToBackend(Uri pdfUri) {
        try {
            // Read the chosen PDF from the phone and prepare it for Retrofit upload.
            byte[] pdfBytes = readBytesFromUri(pdfUri);
            RequestBody requestFile = RequestBody.create(
                    pdfBytes,
                    MediaType.parse("application/pdf")
            );

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file",
                    getFileName(pdfUri),
                    requestFile
            );

            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.generateQuiz(filePart);

            // enqueue runs the request in the background, then returns here when done.
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    setQuizLoading(false);

                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String quizText = response.body().string();

                            openGeneratedQuiz(quizText);
                        } else {
                            Toast.makeText(MainActivity.this, "Quiz generation failed.", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Response error.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    setQuizLoading(false);
                    // Show the real connection error so it is easier to debug emulator/backend issues.
                    Toast.makeText(MainActivity.this, "Backend connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            setQuizLoading(false);
            Toast.makeText(this, "File error.", Toast.LENGTH_LONG).show();
        }
    }

    private void openGeneratedQuiz(String quizText) {
        setQuizLoading(false);

        Intent intent = new Intent(MainActivity.this, GenquizPageActivity.class);
        intent.putExtra("quizText", quizText);
        intent.putExtra("fileName", selectedFileName);
        startActivity(intent);
    }

    private void setQuizLoading(boolean isLoading) {
        // This keeps the user informed while the backend is reading the PDF and asking Gemini.
        generateBtn.setEnabled(!isLoading);
        generateBtn.setText(isLoading ? "Generating..." : "Generate Quiz");
        generateProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        generateStatusText.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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

    private String getFileName(Uri uri) {
        String fileName = "selected.pdf";

        // Android gives a Uri, so this asks Android for the original file name.
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
}
