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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
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

            profileInfo.append("Nom: ").append(nom)
                    .append("\n\nLogin: ").append(login)
                    .append("\n\nID: ").append(professeurId);

            cursor.close();
        }

        // Get matières enseignées
        Cursor matieresCursor = db.getMatieresByProfesseur(professeurId);
        if (matieresCursor != null && matieresCursor.getCount() > 0) {
            profileInfo.append("\n\nMatières enseignées:");
            while (matieresCursor.moveToNext()) {
                String matiere = matieresCursor.getString(matieresCursor.getColumnIndexOrThrow("nom_matiere"));
                profileInfo.append("\n• ").append(matiere);
            }
            matieresCursor.close();
        }

        // Get statistics
        Cursor statsCursor = db.getAbsenceStatsByProfessor(professeurId);
        if (statsCursor != null && statsCursor.getCount() > 0) {
            profileInfo.append("\n\nStatistiques des absences:");
            int totalAbsences = 0;
            while (statsCursor.moveToNext()) {
                String matiere = statsCursor.getString(statsCursor.getColumnIndexOrThrow("matiere"));
                int total = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("total"));
                int validees = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("validees"));
                int refusees = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("refusees"));
                int enAttente = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("en_attente"));

                profileInfo.append("\n\n").append(matiere).append(":")
                        .append("\n  • Total: ").append(total)
                        .append("\n  • Validées: ").append(validees)
                        .append("\n  • Refusées: ").append(refusees)
                        .append("\n  • En attente: ").append(enAttente);

                totalAbsences += total;
            }
            profileInfo.append("\n\nTotal général: ").append(totalAbsences).append(" absences");
            statsCursor.close();
        }

        builder.setMessage(profileInfo.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showMatiereFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filtrer par matière");

        // Get matières taught by this professor
        Cursor cursor = db.getMatieresByProfesseur(professeurId);
        final ArrayList<String> matieresList = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String nomMatiere = cursor.getString(cursor.getColumnIndexOrThrow("nom_matiere"));
                matieresList.add(nomMatiere);
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (matieresList.isEmpty()) {
            Toast.makeText(this, "Aucune matière trouvée", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add "Toutes les matières" option
        matieresList.add(0, "Toutes les matières");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, matieresList);

        builder.setAdapter(adapter, (dialog, which) -> {
            if (which == 0) {
                // Show all absences
                isFilteredView = false;
                currentMatiereFilter = "";
                loadAllAbsences();
                getSupportActionBar().setTitle("Toutes les absences");
            } else {
                // Filter by selected matiere
                String selectedMatiere = matieresList.get(which);
                isFilteredView = true;
                currentMatiereFilter = selectedMatiere;
                loadAbsencesByMatiere(selectedMatiere);
                getSupportActionBar().setTitle("Absences - " + selectedMatiere);
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void loadAllAbsences() {
        Cursor cursor = db.getAbsencesByProfesseur(professeurId);
        updateAbsencesListView(cursor, "Aucune absence enregistrée");
    }

    private void loadAbsencesByMatiere(String matiere) {
        Cursor cursor = db.rawQuery(
                "SELECT id as _id, etudiant_nom, matiere, seance, date, justification, etat " +
                        "FROM absence WHERE professeur_id=? AND matiere=? ORDER BY date DESC",
                new String[]{String.valueOf(professeurId), matiere}
        );
        updateAbsencesListView(cursor, "Aucune absence pour " + matiere);
    }

    private void updateAbsencesListView(Cursor cursor, String emptyMessage) {
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, emptyMessage, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] fromColumns = {"etudiant_nom", "matiere", "seance", "date", "etat"};
        int[] toViews = {R.id.txtEtudiant, R.id.txtMatiere, R.id.txtSeance, R.id.txtDate, R.id.txtEtat};

        adapter = new SimpleCursorAdapter(this, R.layout.item_absence, cursor, fromColumns, toViews, 0);

        adapter.setViewBinder((view, cursor1, columnIndex) -> {
            if (view.getId() == R.id.txtEtat) {
                String etat = cursor1.getString(columnIndex);
                TextView textView = (TextView) view;
                textView.setText(etat);

                switch (etat) {
                    case "Validée":
                        textView.setBackgroundColor(Color.parseColor("#4CAF50"));
                        break;
                    case "Refusée":
                        textView.setBackgroundColor(Color.parseColor("#F44336"));
                        break;
                    case "En attente":
                    case "En attente de validation":
                        textView.setBackgroundColor(Color.parseColor("#FF9800"));
                        break;
                    default:
                        textView.setBackgroundColor(Color.parseColor("#9E9E9E"));
                }
                textView.setTextColor(Color.WHITE);
                textView.setPadding(8, 4, 8, 4);
                return true;
            }
            return false;
        });

        absencesListView.setAdapter(adapter);
    }

    private void logout() {
        Intent intent = new Intent(ProfActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- YOUR ORIGINAL METHODS ---



    private void handleAbsenceClick(int absenceId) {
        Cursor cursor = db.getAbsenceDetails(absenceId);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                String etudiantNom = cursor.getString(cursor.getColumnIndexOrThrow("etudiant_nom"));
                String matiere = cursor.getString(cursor.getColumnIndexOrThrow("matiere"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String etat = cursor.getString(cursor.getColumnIndexOrThrow("etat"));
                String fichier = cursor.getString(cursor.getColumnIndexOrThrow("fichier"));
                int etudiantId = cursor.getInt(cursor.getColumnIndexOrThrow("etudiant_id"));


                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(etudiantNom)
                        .setMessage("Matière: " + matiere +
                                "\nDate: " + date +
                                "\nÉtat: " + etat);

                // Option 1: Contacter
                builder.setPositiveButton("Contacter", (dialog, which) -> {
                    contacterEtudiant(etudiantId);
                });

                // Option 2: Gérer justification (si en attente)
                if (etat.equals("En attente de validation") || etat.equals("En attente")) {
                    builder.setNeutralButton("Gérer justification", (dialog, which) -> {
                        showJustificationOptions(absenceId, etudiantNom, matiere, date, fichier);
                    });
                }

                // Option 3: Fermer
                builder.setNegativeButton("Fermer", null);

                builder.show();

            } catch (Exception e) {
                Toast.makeText(this, "Erreur", Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        }
    }



    //separer back-end de email functions, appeler just l'affichage dans le main


    private void showAddAbsenceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_absence, null);
        builder.setView(dialogView);

        Spinner spinnerEtudiant = dialogView.findViewById(R.id.spinnerEtudiant);
        Spinner spinnerMatiere = dialogView.findViewById(R.id.spinnerMatiere);
        EditText editSeance = dialogView.findViewById(R.id.editSeance);
        EditText editDate = dialogView.findViewById(R.id.editDate);

        String dateAujourdhui = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        editDate.setText(dateAujourdhui);

        Cursor cursorEtudiants = db.getAllEtudiants();
        ArrayList<String> etudiantsList = new ArrayList<>();
        ArrayList<Integer> etudiantsIds = new ArrayList<>();

        if (cursorEtudiants != null && cursorEtudiants.moveToFirst()) {
            do {
                int id = cursorEtudiants.getInt(cursorEtudiants.getColumnIndexOrThrow("_id"));
                String nom = cursorEtudiants.getString(cursorEtudiants.getColumnIndexOrThrow("nom"));
                String groupe = cursorEtudiants.getString(cursorEtudiants.getColumnIndexOrThrow("groupe"));

                etudiantsList.add(nom + " (" + groupe + ")");
                etudiantsIds.add(id);
            } while (cursorEtudiants.moveToNext());
            cursorEtudiants.close();
        }

        ArrayAdapter<String> etudiantsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, etudiantsList);
        etudiantsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEtudiant.setAdapter(etudiantsAdapter);

        Cursor cursorMatieres = db.getMatieresByProfesseur(professeurId);
        ArrayList<String> matieresList = new ArrayList<>();

        if (cursorMatieres != null && cursorMatieres.moveToFirst()) {
            do {
                String nomMatiere = cursorMatieres.getString(cursorMatieres.getColumnIndexOrThrow("nom_matiere"));
                matieresList.add(nomMatiere);
            } while (cursorMatieres.moveToNext());
            cursorMatieres.close();
        }

        ArrayAdapter<String> matieresAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, matieresList);
        matieresAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMatiere.setAdapter(matieresAdapter);

        builder.setTitle("Ajouter une absence");
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            if (spinnerEtudiant.getSelectedItemPosition() == -1 ||
                    spinnerMatiere.getSelectedItemPosition() == -1) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            int etudiantId = etudiantsIds.get(spinnerEtudiant.getSelectedItemPosition());
            String etudiantNom = etudiantsList.get(spinnerEtudiant.getSelectedItemPosition());
            String matiere = spinnerMatiere.getSelectedItem().toString();
            String seance = editSeance.getText().toString();
            String date = editDate.getText().toString();

            boolean success = db.ajouterAbsence(
                    etudiantId,
                    etudiantNom.split(" \\(")[0],
                    professeurId,
                    professeurNom,
                    matiere,
                    seance,
                    date,
                    null,
                    null,
                    "En attente"
            );

            if (success) {
                Toast.makeText(this, "Absence ajoutée avec succès", Toast.LENGTH_SHORT).show();
                loadAllAbsences();
            } else {
                Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteConfirmation(final int absenceId) {
        Cursor cursor = db.getAbsencesEtudiant(absenceId);

        if (cursor != null && cursor.moveToFirst()) {
            String etudiantNom = cursor.getString(cursor.getColumnIndexOrThrow("etudiant_nom"));
            String matiere = cursor.getString(cursor.getColumnIndexOrThrow("matiere"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            cursor.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Supprimer l'absence");
            builder.setMessage("Voulez-vous vraiment supprimer cette absence ?\n\n" +
                    "Étudiant: " + etudiantNom + "\n" +
                    "Matière: " + matiere + "\n" +
                    "Date: " + date);

            builder.setPositiveButton("Supprimer", (dialog, which) -> {
                boolean success = db.supprimerAbsence(absenceId);
                if (success) {
                    Toast.makeText(this, "Absence supprimée", Toast.LENGTH_SHORT).show();
                    loadAllAbsences();
                } else {
                    Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }



    // ===== ADD THESE METHODS TO ProfActivity =====

    // View justification file with validation options
    private void showJustificationOptions(int absenceId, String etudiantNom, String matiere,
                                          String date, String fichierPath) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialogue_justification, null);
        builder.setView(dialogView);

        TextView txtDetails = dialogView.findViewById(R.id.txtDetails);
        Button btnVoirJustification = dialogView.findViewById(R.id.btnVoirJustification);
        Button btnValider = dialogView.findViewById(R.id.btnValider);
        Button btnRefuser = dialogView.findViewById(R.id.btnRefuser);

        // Afficher les détails
        txtDetails.setText("Étudiant: " + etudiantNom +
                "\nMatière: " + matiere +
                "\nDate: " + date);

        AlertDialog dialog = builder.create();

        // ===== VIEW FILE BUTTON =====
        if (fichierPath != null && !fichierPath.isEmpty()) {
            btnVoirJustification.setVisibility(View.VISIBLE);
            btnVoirJustification.setText("📄 Voir le fichier");
            btnVoirJustification.setOnClickListener(v -> {
                openPDFFile(fichierPath);
            });

        } else {
            btnVoirJustification.setVisibility(View.GONE);
            txtDetails.setText(txtDetails.getText() +
                    "\n\n⚠️ Aucun fichier de justification fourni");
        }

        // ===== VALIDATE BUTTON =====
        btnValider.setOnClickListener(v -> {
            if (db.updateAbsenceState(absenceId, "Validée")) {
                Toast.makeText(this, "✓ Absence validée", Toast.LENGTH_SHORT).show();
                sendEmailToStudent(absenceId, "validée");
                loadAllAbsences();
                dialog.dismiss();
            }
        });

        // ===== REJECT BUTTON =====
        btnRefuser.setOnClickListener(v -> {
            AlertDialog.Builder rejectBuilder = new AlertDialog.Builder(this);
            rejectBuilder.setTitle("Refuser l'absence");

            EditText edtMotif = new EditText(this);
            edtMotif.setHint("Motif du refus (optionnel)");
            edtMotif.setLines(2);
            rejectBuilder.setView(edtMotif);

            rejectBuilder.setPositiveButton("Confirmer le refus", (dialog2, which) -> {
                if (db.updateAbsenceState(absenceId, "Refusée")) {
                    Toast.makeText(this, "✓ Absence refusée", Toast.LENGTH_SHORT).show();
                    sendEmailToStudent(absenceId, "refusée");
                    loadAllAbsences();
                    dialog.dismiss();
                }
            });

            rejectBuilder.setNegativeButton("Annuler", null);
            rejectBuilder.show();
        });

        dialog.show();
    }

    // ===== OPEN PDF FILE DIRECTLY =====
    private void openPDFFile(String fichierUri) {
        try {
            if (fichierUri == null || fichierUri.isEmpty()) {
                Toast.makeText(this, "Aucun fichier disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri;

            // Handle different URI formats
            if (fichierUri.startsWith("content://")) {
                uri = Uri.parse(fichierUri);
            } else if (fichierUri.startsWith("file://")) {
                uri = Uri.parse(fichierUri);
            } else {
                // Try as file path
                File file = new File(fichierUri);
                if (file.exists()) {
                    uri = Uri.fromFile(file);
                } else {
                    Toast.makeText(this,
                            "❌ Fichier non trouvé: " + fichierUri,
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // ✅ OPEN WITH SYSTEM PDF APP
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // No PDF viewer installed, try generic viewer
                intent.setType("*/*");
                try {
                    startActivity(Intent.createChooser(intent, "Ouvrir le fichier avec"));
                } catch (Exception ex) {
                    Toast.makeText(this,
                            "⚠️ Aucune application pour ouvrir le fichier",
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


    // ===== OPEN FILE VIEWER =====
    // ===== SIMPLE FILE OPENER (NO FileProvider) =====
    private void ouvrirFichierJustification(String fichierPath) {
        try {
            if (fichierPath == null || fichierPath.isEmpty()) {
                Toast.makeText(this, "Aucun fichier disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = Uri.parse(fichierPath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(Intent.createChooser(intent, "Ouvrir le fichier"));

        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    // ===== SEND EMAIL NOTIFICATION =====
    private void sendEmailToStudent(int absenceId, String status) {
        Cursor cursor = db.getAbsenceDetails(absenceId);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                String etudiantEmail = db.getEmailEtudiant(
                        cursor.getInt(cursor.getColumnIndexOrThrow("etudiant_id")));

                if (etudiantEmail != null && !etudiantEmail.isEmpty()) {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{etudiantEmail});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                            "Décision sur votre absence");
                    emailIntent.putExtra(Intent.EXTRA_TEXT,
                            "Votre absence a été " + status + ".");

                    startActivity(Intent.createChooser(emailIntent, "Envoyer email"));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Erreur email: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        }
    }

    // ===== CONTACT STUDENT =====
    private void contacterEtudiant(int etudiantId) {
        String email = db.getEmailEtudiant(etudiantId);
        if (email != null && !email.isEmpty()) {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            startActivity(Intent.createChooser(emailIntent, "Envoyer email"));
        } else {
            Toast.makeText(this, "Email non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== OPEN DOCUMENT/PDF =====
    private void openDocumentFile(String fichierUri) {
        try {
            if (fichierUri == null || fichierUri.isEmpty()) {
                Toast.makeText(this, "Aucun fichier disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri;

            // Check if it's a content URI or file path
            if (fichierUri.startsWith("content://")) {
                // ✅ Already a content URI
                uri = Uri.parse(fichierUri);
            } else if (fichierUri.startsWith("file://")) {
                // ✅ File URI
                uri = Uri.parse(fichierUri);
            } else {
                // It might be a file path
                File file = new File(fichierUri);
                if (file.exists()) {
                    uri = Uri.fromFile(file);
                } else {
                    Toast.makeText(this,
                            "Fichier non trouvé: " + fichierUri,
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // ===== OPEN WITH INTENT =====
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this,
                        "⚠️ Aucune application PDF disponible",
                        Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "❌ Erreur: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    // Helper method to execute raw query
    private Cursor rawQuery(String query, String[] selectionArgs) {
        return db.getReadableDatabase().rawQuery(query, selectionArgs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}