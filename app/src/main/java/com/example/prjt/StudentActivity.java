package com.example.prjt;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StudentActivity extends AppCompatActivity
        implements AbsenceAdapter.OnUploadClickListener {

    private static final int FILE_REQUEST_CODE = 100;

    TextView txtWelcome;
    RecyclerView recyclerAbsences;
    Button btnLogout;
    DBHelper db;

    ArrayList matieres, dates, etats, professeurs, seances;
    AbsenceAdapter adapter;

    int etudiantId;
    String etudiantNom;
    String groupe;
    String filiere;

    private int absencePositionEnCours;
    private Uri fichierUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        txtWelcome = findViewById(R.id.txtWelcome);
        recyclerAbsences = findViewById(R.id.recyclerAbsences);
        btnLogout = findViewById(R.id.btnLogout);
        db = new DBHelper(this);

        etudiantId = getIntent().getIntExtra("etudiant_id", -1);
        etudiantNom = getIntent().getStringExtra("etudiant_nom");
        groupe = getIntent().getStringExtra("groupe");
        filiere = getIntent().getStringExtra("filiere");

        if (etudiantId == -1) {
            Toast.makeText(this, "Erreur : étudiant non identifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtWelcome.setText("Bienvenue, " + etudiantNom + " (" + groupe + ")");

        matieres = new ArrayList<>();
        dates = new ArrayList<>();
        etats = new ArrayList<>();
        professeurs = new ArrayList<>();
        seances = new ArrayList<>();

        afficherAbsences();

        adapter = new AbsenceAdapter(this, matieres, dates, etats, professeurs, seances);
        adapter.setOnUploadClickListener(this);
        recyclerAbsences.setLayoutManager(new LinearLayoutManager(this));
        recyclerAbsences.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(StudentActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void afficherAbsences() {
        Cursor c = db.getAbsencesEtudiant(etudiantId);
        matieres.clear();
        dates.clear();
        etats.clear();
        professeurs.clear();
        seances.clear();

        if (c != null) {
            while (c.moveToNext()) {
                matieres.add(c.getString(c.getColumnIndexOrThrow("matiere")));
                dates.add(c.getString(c.getColumnIndexOrThrow("date")));
                etats.add(c.getString(c.getColumnIndexOrThrow("etat")));
                professeurs.add(c.getString(c.getColumnIndexOrThrow("professeur_nom")));
                String seance = c.getString(c.getColumnIndexOrThrow("seance"));
                seances.add(seance != null ? seance : "Non spécifiée");
            }
            c.close();
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // ===== CHOOSE FILE =====
    private void choisirFichier() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();

            if (selectedUri != null) {
                try {
                    // ✅ COPY FILE TO APP'S PERSISTENT STORAGE
                    String fileName = getFileName(selectedUri);
                    File appDir = getApplicationContext().getFilesDir();
                    File savedFile = new File(appDir, "justifications_" + System.currentTimeMillis() + "_" + fileName);

                    // Copy file content
                    InputStream inputStream = getContentResolver().openInputStream(selectedUri);
                    OutputStream outputStream = new FileOutputStream(savedFile);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    inputStream.close();
                    outputStream.close();

                    // ✅ SAVE FILE PATH (NOT URI)
                    fichierUri = Uri.fromFile(savedFile);

                    Toast.makeText(this,
                            "✓ Fichier sélectionné: " + fileName,
                            Toast.LENGTH_SHORT).show();

                    // ✅ SHOW DIALOG TO SEND FILE
                    showSendFileDialog();

                } catch (IOException e) {
                    Toast.makeText(this,
                            "✗ Erreur lecture fichier: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    // ===== GET FILE NAME =====
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(index);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // ===== SHOW DIALOG TO CONFIRM SEND =====
    private void showSendFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Envoyer la justification");
        builder.setMessage("Êtes-vous sûr de vouloir envoyer ce fichier?");
        builder.setPositiveButton("Envoyer", (dialog, which) -> {
            envoyerJustification();
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    // ===== SEND FILE TO DATABASE =====
    private void envoyerJustification() {
        if (fichierUri == null) {
            Toast.makeText(this, "Aucun fichier sélectionné", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String matiere = (String) matieres.get(absencePositionEnCours);
            String date = (String) dates.get(absencePositionEnCours);

            // ✅ SAVE FILE PATH
            String filePath = fichierUri.getPath();

            boolean success = db.modifierAbsenceJustification(
                    etudiantId,
                    matiere,
                    date,
                    "Justification uploadée",
                    filePath, // ✅ Use file path
                    "En attente de validation"
            );

            if (success) {
                Toast.makeText(this,
                        "✓ Fichier envoyé avec succès!",
                        Toast.LENGTH_LONG).show();
                afficherAbsences();
                fichierUri = null;
            } else {
                Toast.makeText(this,
                        "✗ Erreur lors de l'envoi",
                        Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "✗ Erreur: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onUploadClick(int position) {
        absencePositionEnCours = position;
        String etat = (String) etats.get(position);

        // ===== BLOCK IF VALIDATED =====
        if (etat.equals("Validée")) {
            Toast.makeText(this, "✓ Absence déjà validée", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== BLOCK IF REFUSED =====
        if (etat.equals("Refusée")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Absence Refusée");
            builder.setMessage("Cette absence a été refusée par le professeur.\n\n" +
                    "Pour contester cette décision:\n" +
                    "📞 Appelez vos parents\n" +
                    "🏢 Contactez l'administration\n" +
                    "📧 Parlez avec votre professeur");
            builder.setPositiveButton("Fermer", null);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.show();
            return;
        }

        // ===== ALLOW UPLOAD ONLY FOR "EN ATTENTE" =====
        if (etat.equals("En attente") || etat.equals("En attente de validation")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Justifier l'absence");
            builder.setMessage("Sélectionnez un fichier PDF ou document");
            builder.setPositiveButton("Choisir fichier", (dialog, which) -> {
                choisirFichier();
            });
            builder.setNegativeButton("Annuler", null);
            builder.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) db.close();
    }
}