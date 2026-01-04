package com.example.prjt;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// ✅ ADD <ViewHolder> TYPE PARAMETER HERE
public class AbsenceAdapter extends RecyclerView.Adapter<AbsenceAdapter.ViewHolder> {

    Context context;
    ArrayList matieres, dates, etats, professeurs, seances;

    public interface OnUploadClickListener {
        void onUploadClick(int position);
    }

    private OnUploadClickListener uploadClickListener;

    public void setOnUploadClickListener(OnUploadClickListener listener) {
        this.uploadClickListener = listener;
    }

    public AbsenceAdapter(Context context, ArrayList matieres, ArrayList dates,
                          ArrayList etats, ArrayList professeurs, ArrayList seances) {
        this.context = context;
        this.matieres = matieres;
        this.dates = dates;
        this.etats = etats;
        this.professeurs = professeurs;
        this.seances = seances;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_absence_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            String matiere = (String) matieres.get(position);
            String date = (String) dates.get(position);
            String seance = (String) seances.get(position);
            String etat = (String) etats.get(position);

            holder.txtMatiere.setText(matiere);
            holder.txtDate.setText(date);
            holder.txtSeance.setText(seance);
            holder.txtEtat.setText(etat);

            // ===== BUTTON LOGIC BY STATE =====
            switch (etat) {
                case "Validée":
                    // ✅ Validated - no button
                    holder.txtEtat.setBackgroundColor(Color.parseColor("#4CAF50"));
                    holder.btnUploadJustification.setVisibility(View.GONE);
                    break;

                case "Refusée":
                    // ❌ Rejected - show blocked button
                    holder.txtEtat.setBackgroundColor(Color.parseColor("#F44336"));
                    holder.btnUploadJustification.setVisibility(View.VISIBLE);
                    holder.btnUploadJustification.setText("⛔ Absence Refusée");
                    holder.btnUploadJustification.setBackgroundColor(Color.parseColor("#F44336"));
                    holder.btnUploadJustification.setEnabled(false);
                    holder.btnUploadJustification.setOnClickListener(v -> {
                        showRejectionDialog();
                    });
                    break;

                case "En attente":
                    // ⏳ Pending - show upload button
                    holder.txtEtat.setBackgroundColor(Color.parseColor("#FF9800"));
                    holder.btnUploadJustification.setVisibility(View.VISIBLE);
                    holder.btnUploadJustification.setText("📤 Joindre justification");
                    holder.btnUploadJustification.setBackgroundColor(Color.parseColor("#2196F3"));
                    holder.btnUploadJustification.setEnabled(true);

                    final int finalPosition = position;
                    holder.btnUploadJustification.setOnClickListener(v -> {
                        if (uploadClickListener != null) {
                            uploadClickListener.onUploadClick(finalPosition);
                        }
                    });
                    break;

                case "En attente de validation":
                    // ✋ File uploaded waiting for professor - no button
                    holder.txtEtat.setBackgroundColor(Color.parseColor("#8B00FF"));
                    holder.txtEtat.setText("📄 En attente de validation");
                    holder.btnUploadJustification.setVisibility(View.GONE);  // ✅ BUTTON HIDDEN
                    break;

                default:
                    holder.txtEtat.setBackgroundColor(Color.parseColor("#9E9E9E"));
                    holder.btnUploadJustification.setVisibility(View.GONE);
            }

            holder.txtEtat.setTextColor(Color.WHITE);
            holder.txtEtat.setPadding(16, 8, 16, 8);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public int getItemCount() {
        return matieres.size();
    }

    // ===== SHOW REJECTION MESSAGE =====
    private void showRejectionDialog() {
        Toast.makeText(context,
                "⚠️ Cette absence a été refusée.\n" +
                        "Veuillez contacter l'administration ou vos parents.",
                Toast.LENGTH_LONG).show();
    }

    // ===== VIEW HOLDER =====
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMatiere, txtDate, txtEtat, txtSeance;
        Button btnUploadJustification;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMatiere = itemView.findViewById(R.id.txtMatiere);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtEtat = itemView.findViewById(R.id.txtEtat);
            txtSeance = itemView.findViewById(R.id.txtSeance);
            btnUploadJustification = itemView.findViewById(R.id.btnUploadJustification);
        }
    }
}
