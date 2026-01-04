package com.example.prjt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "absences.db";
    private static final int DATABASE_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DBNAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Table Etudiant (Student)
        db.execSQL("CREATE TABLE etudiant (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "login TEXT UNIQUE NOT NULL, " +
                "motdepasse TEXT NOT NULL, " +
                "groupe TEXT, " +
                "filiere TEXT)");

        // Table Professeur (Professor)
        db.execSQL("CREATE TABLE professeur (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom TEXT NOT NULL, " +
                "login TEXT UNIQUE NOT NULL, " +
                "motdepasse TEXT NOT NULL)");

        // Table Matiere (Subject taught by professor)
        db.execSQL("CREATE TABLE matiere (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom_matiere TEXT NOT NULL, " +
                "professeur_id INTEGER, " +
                "FOREIGN KEY(professeur_id) REFERENCES professeur(id))");

        // Table Groupe (Groups taught by professor)
        db.execSQL("CREATE TABLE groupe (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom_groupe TEXT NOT NULL, " +
                "professeur_id INTEGER, " +
                "matiere_id INTEGER, " +
                "FOREIGN KEY(professeur_id) REFERENCES professeur(id), " +
                "FOREIGN KEY(matiere_id) REFERENCES matiere(id))");

        // Table Absence (Enhanced with professor and subject info)
        db.execSQL("CREATE TABLE absence (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "etudiant_id INTEGER NOT NULL, " +
                "etudiant_nom TEXT, " +
                "professeur_id INTEGER NOT NULL, " +
                "professeur_nom TEXT, " +
                "matiere TEXT NOT NULL, " +
                "seance TEXT, " +
                "date TEXT NOT NULL, " +
                "justification TEXT, " +
                "fichier TEXT, " +
                "etat TEXT DEFAULT 'En attente', " +
                "FOREIGN KEY(etudiant_id) REFERENCES etudiant(id), " +
                "FOREIGN KEY(professeur_id) REFERENCES professeur(id))");

        // Insert test data
        insertTestData(db);
    }

    private void insertTestData(SQLiteDatabase db) {
        // Insert test students
        db.execSQL("INSERT INTO etudiant (nom,email, login, motdepasse, groupe, filiere) " +
                "VALUES ('Ahmed Alami','najimlap@gmail.com', 'ahmed.alami', '1234', 'G1', 'Informatique')");
        db.execSQL("INSERT INTO etudiant (nom, login, motdepasse, groupe, filiere) " +
                "VALUES ('Fatima Zahra', 'fatima.zahra', '1234', 'G1', 'Informatique')");
        db.execSQL("INSERT INTO etudiant (nom, login, motdepasse, groupe, filiere) " +
                "VALUES ('Mohammed Bennani', 'mohammed.bennani', '1234', 'G2', 'Informatique')");
        db.execSQL("INSERT INTO etudiant (nom, login, motdepasse, groupe, filiere) " +
                "VALUES ('Sara Idrissi', 'sara.idrissi', '1234', 'G2', 'Gestion')");

        // Insert test professors
        db.execSQL("INSERT INTO professeur (nom, login, motdepasse) " +
                "VALUES ('Prof. Hassan Tazi', 'hassan.tazi', '1234')");
        db.execSQL("INSERT INTO professeur (nom, login, motdepasse) " +
                "VALUES ('Prof. Amina Bennis', 'amina.bennis', '1234')");
        db.execSQL("INSERT INTO professeur (nom, login, motdepasse) " +
                "VALUES ('Prof. Karim Moussaoui', 'karim.moussaoui', '1234')");

        // Insert subjects (matieres)
        db.execSQL("INSERT INTO matiere (nom_matiere, professeur_id) VALUES ('Java Programming', 1)");
        db.execSQL("INSERT INTO matiere (nom_matiere, professeur_id) VALUES ('Base de données', 1)");
        db.execSQL("INSERT INTO matiere (nom_matiere, professeur_id) VALUES ('Mathématiques', 2)");
        db.execSQL("INSERT INTO matiere (nom_matiere, professeur_id) VALUES ('Réseaux', 3)");

        // Insert groups taught by professors
        db.execSQL("INSERT INTO groupe (nom_groupe, professeur_id, matiere_id) VALUES ('G1', 1, 1)");
        db.execSQL("INSERT INTO groupe (nom_groupe, professeur_id, matiere_id) VALUES ('G2', 1, 1)");
        db.execSQL("INSERT INTO groupe (nom_groupe, professeur_id, matiere_id) VALUES ('G1', 1, 2)");
        db.execSQL("INSERT INTO groupe (nom_groupe, professeur_id, matiere_id) VALUES ('G1', 2, 3)");
        db.execSQL("INSERT INTO groupe (nom_groupe, professeur_id, matiere_id) VALUES ('G2', 3, 4)");

        // Insert test absences
        db.execSQL("INSERT INTO absence (etudiant_id, etudiant_nom, professeur_id, professeur_nom, matiere, seance, date, etat) " +
                "VALUES (1, 'Ahmed Alami', 1, 'Prof. Hassan Tazi', 'Java Programming', 'Séance 1', '2024-12-10', 'En attente')");
        db.execSQL("INSERT INTO absence (etudiant_id, etudiant_nom, professeur_id, professeur_nom, matiere, seance, date, justification, etat) " +
                "VALUES (2, 'Fatima Zahra', 2, 'Prof. Amina Bennis', 'Mathématiques', 'Séance 2', '2024-12-11', 'Maladie', 'Validée')");
    }

    public String getEmailEtudiant(int etudiantId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT email FROM etudiant WHERE id=?",
                new String[]{String.valueOf(etudiantId)}
        );

        String email = null;
        if (cursor.moveToFirst()) {
            email = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return email;
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS absence");
        db.execSQL("DROP TABLE IF EXISTS groupe");
        db.execSQL("DROP TABLE IF EXISTS matiere");
        db.execSQL("DROP TABLE IF EXISTS etudiant");
        db.execSQL("DROP TABLE IF EXISTS professeur");
        onCreate(db);
    }

    // Méthode pour modifier une absence avec justification
    public boolean modifierAbsenceJustification(int etudiantId, String matiere, String date,
                                                String justification, String fichier, String etat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("justification", justification);
        cv.put("fichier", fichier);
        cv.put("etat", etat);

        int result = db.update("absence", cv,
                "etudiant_id=? AND matiere=? AND date=?",
                new String[]{String.valueOf(etudiantId), matiere, date});

        db.close();
        return result > 0;
    }

    public Cursor getEtudiant(String login, String pass) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM etudiant WHERE login=? AND motdepasse=?",
                new String[]{login, pass});
    }

    public Cursor getProfesseur(String login, String pass) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM professeur WHERE login=? AND motdepasse=?",
                new String[]{login, pass});
    }

    public Cursor getAllEtudiants() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id as _id, nom, login, groupe, filiere FROM etudiant ORDER BY nom", null);
    }

    public Cursor getProfesseurById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM professeur WHERE id=?", new String[]{String.valueOf(id)});
    }

    public Cursor getMatieresByProfesseur(int professeurId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id as _id, nom_matiere FROM matiere WHERE professeur_id=? ORDER BY nom_matiere",
                new String[]{String.valueOf(professeurId)});
    }

    public boolean ajouterAbsence(int etudiantId, String etudiantNom, int professeurId,
                                  String professeurNom, String matiere, String seance,
                                  String date, String justification, String fichier, String etat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("etudiant_id", etudiantId);
        cv.put("etudiant_nom", etudiantNom);
        cv.put("professeur_id", professeurId);
        cv.put("professeur_nom", professeurNom);
        cv.put("matiere", matiere);
        cv.put("seance", seance);
        cv.put("date", date);
        cv.put("justification", justification);
        cv.put("fichier", fichier);
        cv.put("etat", etat);

        long result = db.insert("absence", null, cv);
        return result != -1;
    }

    public Cursor getAbsencesEtudiant(int etudiantId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id as _id, professeur_nom, matiere, seance, date, justification, etat " +
                        "FROM absence WHERE etudiant_id=? ORDER BY date DESC",
                new String[]{String.valueOf(etudiantId)});
    }

    public Cursor getAbsencesByProfesseur(int professeurId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id as _id, etudiant_nom, matiere, seance, date, justification, etat " +
                        "FROM absence WHERE professeur_id=? ORDER BY date DESC",
                new String[]{String.valueOf(professeurId)});
    }


    public Cursor getAbsenceDetails(int absenceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM absence WHERE id=?",
                new String[]{String.valueOf(absenceId)});
    }

    public boolean supprimerAbsence(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete("absence", "id=?", new String[]{String.valueOf(id)});
        return result != -1;
    }

    public boolean updateAbsenceState(int id, String newState) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("etat", newState);
        int result = db.update("absence", cv, "id=?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    public Cursor getAbsenceStatsByProfessor(int professeurId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT matiere, COUNT(*) as total, " +
                        "SUM(CASE WHEN etat='Validée' THEN 1 ELSE 0 END) as validees, " +
                        "SUM(CASE WHEN etat='Refusée' THEN 1 ELSE 0 END) as refusees, " +
                        "SUM(CASE WHEN etat='En attente' THEN 1 ELSE 0 END) as en_attente " +
                        "FROM absence WHERE professeur_id=? GROUP BY matiere",
                new String[]{String.valueOf(professeurId)});
    }

    public Cursor rawQuery(String query, String[] selectionArgs) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(query, selectionArgs);
    }



















/*    public Cursor getEtudiantById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT nom, login, groupe, filiere FROM etudiant WHERE id=?",
                new String[]{String.valueOf(id)}
        );
    }

    // Méthode pour obtenir l'ID d'une absence spécifique
    public int getAbsenceId(int etudiantId, String matiere, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM absence WHERE etudiant_id=? AND matiere=? AND date=?",
                new String[]{String.valueOf(etudiantId), matiere, date});

        int absenceId = -1;
        if (cursor.moveToFirst()) {
            absenceId = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return absenceId;
    }

    public boolean ajouterEtudiant(String nom, String login, String motdepasse, String groupe, String filiere) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nom", nom);
        cv.put("login", login);
        cv.put("motdepasse", motdepasse);
        cv.put("groupe", groupe);
        cv.put("filiere", filiere);
        long result = db.insert("etudiant", null, cv);
        return result != -1;
    }

    public void ajouterJustificationAbsence(
            int absenceId,
            String justification,
            Uri fichierUri
    ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("justification", justification);
        values.put("fichier", fichierUri != null ? fichierUri.toString() : null);
        values.put("etat", "Justifiée");

        db.update(
                "absence",
                values,
                "id = ?",
                new String[]{String.valueOf(absenceId)}
        );

        db.close();
    }

    public boolean ajouterProfesseur(String nom, String login, String motdepasse) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nom", nom);
        cv.put("login", login);
        cv.put("motdepasse", motdepasse);
        long result = db.insert("professeur", null, cv);
        return result != -1;
    }

    public Cursor getAllProfesseurs() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id as _id, nom, login FROM professeur ORDER BY nom", null);
    }

    public boolean ajouterMatiere(String nomMatiere, int professeurId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nom_matiere", nomMatiere);
        cv.put("professeur_id", professeurId);
        long result = db.insert("matiere", null, cv);
        return result != -1;
    }

    public Cursor getAllMatieres() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT m.id as _id, m.nom_matiere, p.nom as professeur_nom " +
                "FROM matiere m " +
                "LEFT JOIN professeur p ON m.professeur_id = p.id " +
                "ORDER BY m.nom_matiere", null);
    }

    public boolean ajouterGroupe(String nomGroupe, int professeurId, int matiereId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nom_groupe", nomGroupe);
        cv.put("professeur_id", professeurId);
        cv.put("matiere_id", matiereId);
        long result = db.insert("groupe", null, cv);
        return result != -1;
    }

    public Cursor getGroupesByProfesseur(int professeurId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT g.id as _id, g.nom_groupe, m.nom_matiere " +
                        "FROM groupe g " +
                        "LEFT JOIN matiere m ON g.matiere_id = m.id " +
                        "WHERE g.professeur_id=? " +
                        "ORDER BY g.nom_groupe",
                new String[]{String.valueOf(professeurId)});
    }

    public Cursor getAllAbsences() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id as _id, etudiant_nom, professeur_nom, matiere, date, etat " +
                "FROM absence ORDER BY date DESC", null);
    }

    public boolean modifierAbsence(int id, String justification, String fichier, String etat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("justification", justification);
        cv.put("fichier", fichier);
        cv.put("etat", etat);

        long result = db.update("absence", cv, "id=?", new String[]{String.valueOf(id)});
        return result != -1;
    }

    public Cursor getAbsenceStatsByStudent(int etudiantId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT matiere, COUNT(*) as total, " +
                        "SUM(CASE WHEN etat='Validée' THEN 1 ELSE 0 END) as validees, " +
                        "SUM(CASE WHEN etat='Refusée' THEN 1 ELSE 0 END) as refusees, " +
                        "SUM(CASE WHEN etat='En attente' THEN 1 ELSE 0 END) as en_attente " +
                        "FROM absence WHERE etudiant_id=? GROUP BY matiere",
                new String[]{String.valueOf(etudiantId)});
    }

    public Cursor getAbsencesByMatiere(int professeurId, String matiere) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT id as _id, etudiant_nom, matiere, seance, date, justification, etat " +
                        "FROM absence WHERE professeur_id=? AND matiere=? ORDER BY date DESC",
                new String[]{String.valueOf(professeurId), matiere}
        );
    }*/

}