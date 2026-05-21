package com.example.genquizlayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class GenquizPageActivity extends AppCompatActivity {

    FirebaseFirestore db;
    FirebaseAuth auth;
    String quizText;
    String fileName;
    boolean reviewMode;
    int reviewScore;

    TextView fileTitle;
    TextView questionCount;
    TextView questionText;
    TextView scoreResultText;
    View scoreOverlay;
    ProgressBar progressBar;

    MaterialButton answerA;
    MaterialButton answerB;
    MaterialButton answerC;
    MaterialButton answerD;
    MaterialButton prevBtn;
    MaterialButton nextBtn;

    ArrayList<QuizQuestion> questions = new ArrayList<>();
    ArrayList<String> selectedAnswers = new ArrayList<>();

    int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.genquiz_page);

        fileTitle = findViewById(R.id.fileTitle);
        questionCount = findViewById(R.id.questionCount);
        questionText = findViewById(R.id.questionText);
        scoreResultText = findViewById(R.id.scoreResultText);
        scoreOverlay = findViewById(R.id.scoreOverlay);
        progressBar = findViewById(R.id.progressBar);

        answerA = findViewById(R.id.answerA);
        answerB = findViewById(R.id.answerB);
        answerC = findViewById(R.id.answerC);
        answerD = findViewById(R.id.answerD);
        prevBtn = findViewById(R.id.prevBtn);
        nextBtn = findViewById(R.id.nextBtn);

        ImageButton closeBtn = findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(v -> finish());
        scoreOverlay.setOnClickListener(v -> openSavedQuizzesPage());

        quizText = getIntent().getStringExtra("quizText");
        fileName = getIntent().getStringExtra("fileName");
        reviewMode = getIntent().getBooleanExtra("reviewMode", false);
        reviewScore = getIntent().getIntExtra("score", 0);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (fileName != null) {
            fileTitle.setText(fileName);
        }

        questions = parseQuizText(quizText);

        if (questions.size() == 0) {
            Toast.makeText(this, "No quiz questions found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // In normal mode, start with blank answers. In review mode, load saved answers from Firestore.
        ArrayList<String> savedAnswers = getIntent().getStringArrayListExtra("selectedAnswers");

        for (int i = 0; i < questions.size(); i++) {
            if (reviewMode && savedAnswers != null && i < savedAnswers.size()) {
                selectedAnswers.add(savedAnswers.get(i));
            } else {
                selectedAnswers.add("");
            }
        }

        if (reviewMode) {
            Toast.makeText(this, "Review mode. Score: " + reviewScore + "/" + questions.size(), Toast.LENGTH_LONG).show();
        }

        answerA.setOnClickListener(v -> chooseAnswer("A"));
        answerB.setOnClickListener(v -> chooseAnswer("B"));
        answerC.setOnClickListener(v -> chooseAnswer("C"));
        answerD.setOnClickListener(v -> chooseAnswer("D"));

        prevBtn.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showCurrentQuestion();
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (currentIndex < questions.size() - 1) {
                currentIndex++;
                showCurrentQuestion();
            } else {
                showFinalScore();
            }
        });

        showCurrentQuestion();
    }

    private void chooseAnswer(String answer) {
        if (reviewMode) {
            return;
        }

        selectedAnswers.set(currentIndex, answer);
        showCurrentQuestion();
    }

    private void showCurrentQuestion() {
        QuizQuestion quizQuestion = questions.get(currentIndex);

        questionText.setText((currentIndex + 1) + ". " + quizQuestion.question);
        answerA.setText("A. " + quizQuestion.optionA);
        answerB.setText("B. " + quizQuestion.optionB);
        answerC.setText("C. " + quizQuestion.optionC);
        answerD.setText("D. " + quizQuestion.optionD);

        questionCount.setText("QUESTION " + (currentIndex + 1) + " OF " + questions.size());
        progressBar.setProgress(((currentIndex + 1) * 100) / questions.size());

        prevBtn.setEnabled(currentIndex > 0);

        if (currentIndex == questions.size() - 1) {
            if (reviewMode) {
                nextBtn.setText("Done");
            } else {
                nextBtn.setText("Finish");
            }
        } else {
            nextBtn.setText("Next");
        }

        updateAnswerColors();
    }

    private void updateAnswerColors() {
        resetButtonColor(answerA);
        resetButtonColor(answerB);
        resetButtonColor(answerC);
        resetButtonColor(answerD);

        String selected = selectedAnswers.get(currentIndex);

        if (reviewMode) {
            showReviewColors();
            return;
        }

        if (selected.equals("A")) {
            setSelectedButtonColor(answerA);
        } else if (selected.equals("B")) {
            setSelectedButtonColor(answerB);
        } else if (selected.equals("C")) {
            setSelectedButtonColor(answerC);
        } else if (selected.equals("D")) {
            setSelectedButtonColor(answerD);
        }
    }

    private void showReviewColors() {
        QuizQuestion quizQuestion = questions.get(currentIndex);
        String selected = selectedAnswers.get(currentIndex);

        // In review mode, green means correct answer and red means user's wrong answer.
        if (quizQuestion.answer.equals("A")) {
            setCorrectButtonColor(answerA);
        } else if (quizQuestion.answer.equals("B")) {
            setCorrectButtonColor(answerB);
        } else if (quizQuestion.answer.equals("C")) {
            setCorrectButtonColor(answerC);
        } else if (quizQuestion.answer.equals("D")) {
            setCorrectButtonColor(answerD);
        }

        if (!selected.equals("") && !selected.equalsIgnoreCase(quizQuestion.answer)) {
            if (selected.equals("A")) {
                setWrongButtonColor(answerA);
            } else if (selected.equals("B")) {
                setWrongButtonColor(answerB);
            } else if (selected.equals("C")) {
                setWrongButtonColor(answerC);
            } else if (selected.equals("D")) {
                setWrongButtonColor(answerD);
            }
        }
    }

    private void resetButtonColor(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.answerDefaultBg)));
        button.setTextColor(ContextCompat.getColor(this, R.color.answerDefaultText));
    }

    private void setSelectedButtonColor(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryBlue)));
        button.setTextColor(Color.WHITE);
    }

    private void setCorrectButtonColor(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.successGreen)));
        button.setTextColor(Color.WHITE);
    }

    private void setWrongButtonColor(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.wrongRed)));
        button.setTextColor(Color.WHITE);
    }

    private void showFinalScore() {
        if (reviewMode) {
            // Review mode is only for viewing saved answers, so pressing Done should not create a new quiz attempt.
            Intent intent = new Intent(GenquizPageActivity.this, QuizPageActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        int score = 0;

        for (int i = 0; i < questions.size(); i++) {
            if (selectedAnswers.get(i).equalsIgnoreCase(questions.get(i).answer)) {
                score++;
            }

        }
        saveQuizResult(score, questions.size());

        showScoreOverlay(score, questions.size());
    }

    private void showScoreOverlay(int score, int total) {
        // This replaces the old Toast with a card that stays visible until the user taps.
        scoreResultText.setText("Score: " + score + "/" + total);
        scoreOverlay.setVisibility(View.VISIBLE);
    }

    private void openSavedQuizzesPage() {
        Intent intent = new Intent(GenquizPageActivity.this, QuizPageActivity.class);
        startActivity(intent);
        finish();
    }

    private void saveQuizResult(int score, int total) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("fileName", fileName);
        quizData.put("quizText", quizText);
        quizData.put("score", score);
        quizData.put("total", total);
        quizData.put("createdAt", System.currentTimeMillis());
        quizData.put("selectedAnswers", new ArrayList<>(selectedAnswers));

        db.collection("users")
                .document(userId)
                .collection("quizzes")
                .add(quizData);
    }

    private ArrayList<QuizQuestion> parseQuizText(String quizText) {
        ArrayList<QuizQuestion> parsedQuestions = new ArrayList<>();

        if (quizText == null) {
            return parsedQuestions;
        }

        String[] lines = quizText.split("\\r?\\n");
        QuizQuestion currentQuestion = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty()) {
                continue;
            }

            // Gemini usually writes questions like "1. What is...?"
            if (line.matches("^\\d+[\\.)].*")) {
                if (currentQuestion != null) {
                    parsedQuestions.add(currentQuestion);
                }

                currentQuestion = new QuizQuestion();
                currentQuestion.question = line.replaceFirst("^\\d+[\\.)]\\s*", "");
            }

            else if (currentQuestion != null && line.matches("^[Aa][\\.)].*")) {
                currentQuestion.optionA = line.replaceFirst("^[Aa][\\.)]\\s*", "");
            }

            else if (currentQuestion != null && line.matches("^[Bb][\\.)].*")) {
                currentQuestion.optionB = line.replaceFirst("^[Bb][\\.)]\\s*", "");
            }

            else if (currentQuestion != null && line.matches("^[Cc][\\.)].*")) {
                currentQuestion.optionC = line.replaceFirst("^[Cc][\\.)]\\s*", "");
            }

            else if (currentQuestion != null && line.matches("^[Dd][\\.)].*")) {
                currentQuestion.optionD = line.replaceFirst("^[Dd][\\.)]\\s*", "");
            }

            else if (currentQuestion != null && line.toLowerCase().startsWith("answer:")) {
                String answerText = line.substring(line.indexOf(":") + 1).trim();

                if (!answerText.isEmpty()) {
                    currentQuestion.answer = answerText.substring(0, 1).toUpperCase();
                }
            }
        }

        if (currentQuestion != null) {
            parsedQuestions.add(currentQuestion);
        }

        return parsedQuestions;
    }

    static class QuizQuestion {
        String question = "";
        String optionA = "";
        String optionB = "";
        String optionC = "";
        String optionD = "";
        String answer = "";
    }
}
