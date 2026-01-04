package com.example.prjt;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ProfActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DBHelper db;
    ListView absencesListView;
    SimpleCursorAdapter adapter;
    TextView txtProfName;

    // Navigation drawer
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    int professeurId;
    String professeurNom;

    // Current view state
    private boolean isFilteredView = false;
    private String currentMatiereFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prof_new);

        db = new DBHelper(this);
        absencesListView = findViewById(R.id.absences_list_view);
        txtProfName = findViewById(R.id.txtProfName);

        // Setup toolbar with hamburger menu
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Tableau de bord");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Récupérer les infos du professeur
        professeurId = getIntent().getIntExtra("professeur_id", -1);
        professeurNom = getIntent().getStringExtra("professeur_nom");

        if (professeurId == -1) {
            Toast.makeText(this, "Erreur: Professeur non identifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update navigation header with professor info
        View headerView = navigationView.getHeaderView(0);
        TextView navName = headerView.findViewById(R.id.nav_prof_name);
        navName.setText("Prof. " + professeurNom);

        // Set the welcome text
        txtProfName.setText("Prof. " + professeurNom);

        // Charger les absences par défaut (toutes)
        loadAllAbsences();

        // Clic long pour supprimer
        absencesListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteConfirmation((int) id);
            return true;
        });

        // Clic simple pour gérer justifications/contact
        absencesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleAbsenceClick((int) id);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            showProfileDialog();
        } else if (id == R.id.nav_add_absence) {
            showAddAbsenceDialog();
        } else if (id == R.id.nav_view_absences) {
            showMatiereFilterDialog();
        } else if (id == R.id.nav_all_absences) {
            // Load all absences
            isFilteredView = false;
            currentMatiereFilter = "";
            loadAllAbsences();
            getSupportActionBar().setTitle("Toutes les absences");
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profil Professeur");

        // Get professor details
        Cursor cursor = db.getProfesseurById(professeurId);
        StringBuilder profileInfo = new StringBuilder();

        if (cursor != null && cursor.moveToFirst()) {
            String nom = cursor.getString(cursor.getColumnIndexOrThrow("nom"));
            String login = cursor.getString(cursor.getColumnIndexOrThrow("login"));
            String specialite = cursor.getString(cursor.getColumnIndexOrThrow("specialite"));

            profileInfo.append("Nom: ").append(nom).append("\n");
            profileInfo.append("Login: ").append(login).append("\n");
            profileInfo.append("Spécialité: ").append(specialite);

            cursor.close();
        }

        builder.setMessage(profileInfo.toString());
        builder.setPositiveButton("Fermer", null);
        builder.show();
    }

    private void showAddAbsenceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter une absence");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_absence, null);
        EditText etudiantInput = view.findViewById(R.id.et_student_name);
        EditText matiereInput = view.findViewById(R.id.et_subject);
        EditText dateInput = view.findViewById(R.id.et_date);

        builder.setView(view);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            String etudiant = etudiantInput.getText().toString();
            String matiere = matiereInput.getText().toString();
            String date = dateInput.getText().toString();

            if (!etudiant.isEmpty() && !matiere.isEmpty() && !date.isEmpty()) {
                // Add absence logic here
                Toast.makeText(this, "Absence ajoutée", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void showMatiereFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filtrer par matière");

        ArrayList<String> matieres = new ArrayList<>();
        Cursor c = db.getAllMatieres();
        if (c != null) {
            while (c.moveToNext()) {
                matieres.add(c.getString(c.getColumnIndexOrThrow("matiere")));
            }
            c.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, matieres);
        builder.setAdapter(adapter, (dialog, which) -> {
            currentMatiereFilter = matieres.get(which);
            loadFilteredAbsences(currentMatiereFilter);
            isFilteredView = true;
            getSupportActionBar().setTitle("Absences: " + currentMatiereFilter);
        });
        builder.show();
    }

    private void loadAllAbsences() {
        Cursor cursor = db.getAllAbsences();

        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.item_absence,
                    cursor,
                    new String[]{"etudiant_nom", "matiere", "date", "etat"},
                    new int[]{R.id.txt_student_name, R.id.txt_subject, R.id.txt_date, R.id.txt_status},
                    0
            );
            absencesListView.setAdapter(adapter);
        } else {
            adapter.changeCursor(cursor);
        }
    }

    private void loadFilteredAbsences(String matiere) {
        Cursor cursor = db.getAbsencesByMatiere(matiere);

        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.item_absence,
                    cursor,
                    new String[]{"etudiant_nom", "matiere", "date", "etat"},
                    new int[]{R.id.txt_student_name, R.id.txt_subject, R.id.txt_date, R.id.txt_status},
                    0
            );
            absencesListView.setAdapter(adapter);
        } else {
            adapter.changeCursor(cursor);
        }
    }

    private void handleAbsenceClick(int absenceId) {
        Cursor cursor = db.getAbsenceById(absenceId);

        if (cursor != null && cursor.moveToFirst()) {
            String etudiantNom = cursor.getString(cursor.getColumnIndexOrThrow("etudiant_nom"));
            String matiere = cursor.getString(cursor.getColumnIndexOrThrow("matiere"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            String etat = cursor.getString(cursor.getColumnIndexOrThrow("etat"));
            String fichier = cursor.getString(cursor.getColumnIndexOrThrow("fichier"));
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));

            cursor.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Gestion de l'absence");

            StringBuilder message = new StringBuilder();
            message.append("Étudiant: ").append(etudiantNom).append("\n");
            message.append("Matière: ").append(matiere).append("\n");
            message.append("Date: ").append(date).append("\n");
            message.append("État: ").append(etat).append("\n");

            builder.setMessage(message.toString());

            // Add buttons based on state
            if (etat.equals("En attente de validation") && fichier != null && !fichier.isEmpty()) {
                builder.setPositiveButton("Voir le fichier", (dialog, which) -> {
                    openPDFFile(fichier);
                });
                builder.setNegativeButton("Valider", (dialog, which) -> {
                    validateAbsence(absenceId, etudiantNom, email);
                });
                builder.setNeutralButton("Refuser", (dialog, which) -> {
                    refuseAbsence(absenceId, etudiantNom, email);
                });
            } else if (etat.equals("En attente") && (email != null && !email.isEmpty())) {
                builder.setPositiveButton("Contacter", (dialog, which) -> {
                    sendEmailContact(email);
                });
                builder.setNegativeButton("Fermer", null);
            } else {
                builder.setPositiveButton("Fermer", null);
            }

            builder.show();
        }
    }

    // ===== OPEN PDF FILE DIRECTLY =====
    private void openPDFFile(String fichierPath) {
        try {
            if (fichierPath == null || fichierPath.isEmpty()) {
                Toast.makeText(this, "Aucun fichier disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ CREATE URI FROM FILE PATH
            File file = new File(fichierPath);

            if (!file.exists()) {
                Toast.makeText(this,
                        "❌ Fichier non trouvé: " + fichierPath,
                        Toast.LENGTH_LONG).show();
                return;
            }

            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    "com.example.prjt.fileprovider",
                    file
            );

            // ✅ OPEN FILE
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                intent.setType("*/*");
                try {
                    startActivity(Intent.createChooser(intent, "Ouvrir le fichier avec"));
                } catch (Exception ex) {
                    Toast.makeText(this,
                            "⚠️ Aucune application disponible",
                            Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "❌ Erreur: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void validateAbsence(int absenceId, String etudiantNom, String email) {
        boolean success = db.updateAbsenceEtat(absenceId, "Validée");
        if (success) {
            Toast.makeText(this, "✓ Absence validée", Toast.LENGTH_SHORT).show();
            loadAllAbsences();
        }
    }

    private void refuseAbsence(int absenceId, String etudiantNom, String email) {
        boolean success = db.updateAbsenceEtat(absenceId, "Refusée");
        if (success) {
            Toast.makeText(this, "✓ Absence refusée", Toast.LENGTH_SHORT).show();
            loadAllAbsences();
        }
    }

    private void sendEmailContact(String email) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Absence à justifier");
        intent.putExtra(Intent.EXTRA_TEXT, "Veuillez justifier votre absence...");

        try {
            startActivity(Intent.createChooser(intent, "Envoyer un email"));
        } catch (Exception e) {
            Toast.makeText(this, "Pas d'application email disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(int absenceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Supprimer");
        builder.setMessage("Êtes-vous sûr?");
        builder.setPositiveButton("Supprimer", (dialog, which) -> {
            db.deleteAbsence(absenceId);
            loadAllAbsences();
            Toast.makeText(this, "Supprimé", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Déconnexion");
        builder.setMessage("Êtes-vous sûr?");
        builder.setPositiveButton("Déconnexion", (dialog, which) -> {
            Intent intent = new Intent(ProfActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }
}