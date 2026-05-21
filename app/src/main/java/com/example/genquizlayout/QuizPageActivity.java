package com.example.genquizlayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class QuizPageActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;
    FirebaseFirestore db;
    LinearLayout quizListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.quiz_page);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        quizListContainer = findViewById(R.id.quizListContainer);

        if (user == null){
            Intent intent = new Intent(
                    getApplicationContext(),
                    LoginPageActivity.class
            );

            startActivity(intent);
            finish();
            return;
        }

        setupTopBar();
        setupBottomNavigation();
        loadSavedQuizzes();
    }

    private void setupTopBar() {
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
                    QuizPageActivity.this,
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
                            QuizPageActivity.this,
                            LoginPageActivity.class
                    );

                    startActivity(intent);

                    finish();

                }

                return true;

            });

            popupMenu.show();

        });
    }

    private void setupBottomNavigation() {
        LinearLayout homeNav = findViewById(R.id.homeNav);

        homeNav.setOnClickListener(v -> {

            Intent intent = new Intent(
                    QuizPageActivity.this,
                    MainActivity.class
            );

            startActivity(intent);

        });

        LinearLayout pdfconNav = findViewById(R.id.pdfconNav);

        pdfconNav.setOnClickListener(v -> {

            Intent intent = new Intent(
                    QuizPageActivity.this,
                    PdfconverterPageActivity.class
            );

            startActivity(intent);

        });
    }

    private void loadSavedQuizzes() {
        quizListContainer.removeAllViews();

        // Each user has their own quizzes under users/{userId}/quizzes.
        db.collection("users")
                .document(user.getUid())
                .collection("quizzes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyMessage();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String fileName = document.getString("fileName");
                        String quizText = document.getString("quizText");
                        Long score = document.getLong("score");
                        Long total = document.getLong("total");
                        ArrayList<String> selectedAnswers = (ArrayList<String>) document.get("selectedAnswers");

                        addQuizCard(fileName, quizText, selectedAnswers, score, total);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load quizzes.", Toast.LENGTH_LONG).show()
                );
    }

    private void addQuizCard(String fileName, String quizText, ArrayList<String> selectedAnswers, Long score, Long total) {
        if (fileName == null) {
            fileName = "Untitled quiz";
        }

        if (score == null) {
            score = 0L;
        }

        if (total == null) {
            total = 0L;
        }

        // Inflate the XML card design so each Firestore quiz uses the same UI style.
        View card = LayoutInflater.from(this).inflate(R.layout.quiz_item_card, quizListContainer, false);

        TextView titleText = card.findViewById(R.id.quizFileName);
        TextView scoreText = card.findViewById(R.id.quizScore);
        MaterialButton reviewBtn = card.findViewById(R.id.viewPdfBtn);
        MaterialButton retakeBtn = card.findViewById(R.id.retakeBtn);

        titleText.setText(fileName);
        scoreText.setText("Score: " + score + "/" + total);

        reviewBtn.setText("Review");

        String finalFileName = fileName;
        String finalQuizText = quizText;
        ArrayList<String> finalSelectedAnswers = selectedAnswers;
        int finalScore = score.intValue();

        // Review opens the saved quiz and highlights correct/wrong answers.
        reviewBtn.setOnClickListener(v -> {
            if (finalQuizText == null || finalQuizText.isEmpty()) {
                Toast.makeText(this, "Quiz data is missing.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(QuizPageActivity.this, GenquizPageActivity.class);
            intent.putExtra("quizText", finalQuizText);
            intent.putExtra("fileName", finalFileName);
            intent.putExtra("reviewMode", true);
            intent.putExtra("score", finalScore);

            if (finalSelectedAnswers != null) {
                intent.putStringArrayListExtra("selectedAnswers", finalSelectedAnswers);
            }

            startActivity(intent);
        });

        // Retake opens the same generated quiz again by passing quizText back to GenquizPageActivity.
        retakeBtn.setOnClickListener(v -> {
            if (finalQuizText == null || finalQuizText.isEmpty()) {
                Toast.makeText(this, "Quiz data is missing.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(QuizPageActivity.this, GenquizPageActivity.class);
            intent.putExtra("quizText", finalQuizText);
            intent.putExtra("fileName", finalFileName);
            startActivity(intent);
        });

        quizListContainer.addView(card);
    }

    private void showEmptyMessage() {
        TextView emptyText = new TextView(this);
        emptyText.setText("No saved quizzes yet.");
        emptyText.setTextSize(16);
        emptyText.setTextColor(0xFF777777);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(0, dpToPx(40), 0, 0);

        quizListContainer.addView(emptyText);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
