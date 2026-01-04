package com.example.prjt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    EditText edtLogin, edtPassword;
    RadioGroup roleGroup;
    RadioButton rbEtudiant, rbProf;
    Button btnLogin;

    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Liaison XML
        edtLogin = findViewById(R.id.edtLogin);
        edtPassword = findViewById(R.id.edtPassword);
        roleGroup = findViewById(R.id.roleGroup);
        rbEtudiant = findViewById(R.id.rbEtudiant);
        rbProf = findViewById(R.id.rbProf);
        btnLogin = findViewById(R.id.btnLogin);

        // instancier DBHelper
        db = new DBHelper(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String login = edtLogin.getText().toString().trim();
                String pass = edtPassword.getText().toString().trim();

                // Vérification des champs vides
                if (login.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    return;
                }


                // Si étudiant
                if (rbEtudiant.isChecked()) {
                    Cursor c = db.getEtudiant(login, pass);

                    if (c != null && c.moveToFirst()) {
                        // Récupérer l'ID et le nom de l'étudiant
                        int etudiantId = c.getInt(c.getColumnIndexOrThrow("id"));
                        String etudiantNom = c.getString(c.getColumnIndexOrThrow("nom"));
                        String groupe = c.getString(c.getColumnIndexOrThrow("groupe"));
                        String filiere = c.getString(c.getColumnIndexOrThrow("filiere"));

                        c.close();

                        Toast.makeText(LoginActivity.this, "Bienvenue " + etudiantNom, Toast.LENGTH_SHORT).show();

                        Intent i = new Intent(LoginActivity.this, StudentActivity.class);
                        i.putExtra("etudiant_id", etudiantId);
                        i.putExtra("etudiant_nom", etudiantNom);
                        i.putExtra("login", login);
                        i.putExtra("groupe", groupe);
                        i.putExtra("filiere", filiere);
                        startActivity(i);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
                        if (c != null) c.close();
                    }
                }

                //  Si professeur
                else if (rbProf.isChecked()) {
                    Cursor c = db.getProfesseur(login, pass);

                    if (c != null && c.moveToFirst()) {
                        //  Récupérer l'ID et le nom du professeur
                        int professeurId = c.getInt(c.getColumnIndexOrThrow("id"));
                        String professeurNom = c.getString(c.getColumnIndexOrThrow("nom"));

                        c.close();

                        Toast.makeText(LoginActivity.this, "Bienvenue " + professeurNom, Toast.LENGTH_SHORT).show();

                        Intent i = new Intent(LoginActivity.this, ProfActivity.class);
                        i.putExtra("professeur_id", professeurId);
                        i.putExtra("professeur_nom", professeurNom);
                        i.putExtra("login", login);
                        startActivity(i);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
                        if (c != null) c.close();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}