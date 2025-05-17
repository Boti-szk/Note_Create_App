package com.example.note_create_app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> implements Filterable {
    private ArrayList<Notes> mNotes = new ArrayList<>();
    private ArrayList<Notes> mNotesAll = new ArrayList<>();
    private Context mContext;
    private int lastPosition = -1;

    NotesAdapter(Context context, ArrayList<Notes> notes) {
        this.mNotes = notes;
        this.mNotesAll = notes;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(NotesAdapter.ViewHolder holder, int position) {
        Notes currentNote = mNotes.get(position);
        holder.bindTo(currentNote);

        if (holder.getAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return mNotes.size();
    }

    @Override
    public Filter getFilter() {
        return notesFilter;
    }

    private Filter notesFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<Notes> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if (charSequence == null || charSequence.length() == 0) {
                results.count = mNotesAll.size();
                results.values = mNotesAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();
                for (Notes note : mNotesAll) {
                    if (note.getTitle().toLowerCase().contains(filterPattern)) {
                        filteredList.add(note);
                    }
                }
                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mNotes = (ArrayList<Notes>) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleText;
        private TextView mContentText;
        private TextView mDateText;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitleText = itemView.findViewById(R.id.noteTitle);
            mContentText = itemView.findViewById(R.id.noteContentView);
            mDateText = itemView.findViewById(R.id.noteDate);
        }

        public void bindTo(Notes currentNote) {
            mTitleText.setText(currentNote.getTitle());
            mContentText.setText(currentNote.getContent());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedDate = sdf.format(new Date(currentNote.getDate()));
            mDateText.setText(formattedDate);

            //Jegyzet módosítása
            //itemView.findViewById(R.id.Edit).setOnClickListener(view ->
            //        Log.d("Activity", "Edit gomb megnyomása"));

            itemView.findViewById(R.id.Delete).setOnClickListener(view ->
                    ((Note_Create_MainActivity) mContext).deleteNote(currentNote));
        }
    }
}
