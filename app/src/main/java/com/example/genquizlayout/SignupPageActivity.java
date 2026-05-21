package com.example.genquizlayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupPageActivity extends AppCompatActivity {

    private static final String TAG = "SignupPageActivity";
    EditText signupEmail, signupPassword;
    Button signupBtn;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.signup_page);


        mAuth = FirebaseAuth.getInstance();
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        signupBtn = findViewById(R.id.signupBtn);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.loginNow);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginPageActivity.class);
                startActivity(intent);
                finish();
            }
        });



        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password;

                email = String.valueOf(signupEmail.getText()).trim();
                password = String.valueOf(signupPassword.getText()).trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(SignupPageActivity.this, "Enter Email",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(SignupPageActivity.this, "Enter Password",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {

                                    Toast.makeText(SignupPageActivity.this, "Account Created",
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), LoginPageActivity.class);
                                    startActivity(intent);
                                    finish();

                                } else {
                                    Toast.makeText(SignupPageActivity.this, "Account Creation failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            }
        });
    }
}
