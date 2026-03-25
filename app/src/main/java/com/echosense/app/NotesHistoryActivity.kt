package com.echosense.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotesHistoryActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes_history)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerNotes)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = NotesAdapter()
        recycler.adapter = adapter

        val db = EchoSenseDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.conversationNoteDao().getAllNotes().collectLatest { notes ->
                adapter.submitList(notes)
            }
        }
    }

    inner class NotesAdapter : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {
        private var notes: List<ConversationNote> = emptyList()

        fun submitList(newNotes: List<ConversationNote>) {
            notes = newNotes
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation_note, parent, false)
            return NoteViewHolder(view)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val note = notes[position]
            holder.bind(note)
        }

        override fun getItemCount(): Int = notes.size

        inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvSpeaker: TextView = view.findViewById(R.id.tvSpeaker)
            private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
            private val tvText: TextView = view.findViewById(R.id.tvText)
            private val tvSummary: TextView = view.findViewById(R.id.tvSummary)

            fun bind(note: ConversationNote) {
                tvSpeaker.text = note.speakerLabel
                tvTimestamp.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(note.timestamp))
                tvText.text = note.text
                
                if (note.summary != null) {
                    tvSummary.visibility = View.VISIBLE
                    tvSummary.text = note.summary
                } else {
                    tvSummary.visibility = View.GONE
                }

                itemView.setOnLongClickListener {
                    // Quick delete on long press for the prototype
                    lifecycleScope.launch {
                        EchoSenseDatabase.getDatabase(this@NotesHistoryActivity).conversationNoteDao().deleteNote(note)
                    }
                    true
                }
            }
        }
    }
}
