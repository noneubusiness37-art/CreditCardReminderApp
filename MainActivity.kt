package com.example.creditcardreminder

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class Card(val name:String, val due:Int, val amount:Double)

class MainActivity : Activity() {
    private val cards = mutableListOf<Card>()
    private lateinit var list: LinearLayout
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("cards", MODE_PRIVATE)
        load()
        showHome()
        if (android.os.Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 10)
    }

    private fun load() {
        val n = prefs.getInt("count", 0)
        repeat(n) {
            cards.add(Card(
                prefs.getString("name$it","Card")!!,
                prefs.getInt("due$it",1),
                prefs.getString("amount$it","0")!!.toDoubleOrNull() ?: 0.0
            ))
        }
    }
    private fun save() {
        val e = prefs.edit().clear()
        e.putInt("count", cards.size)
        cards.forEachIndexed { i,c ->
            e.putString("name$i",c.name).putInt("due$i",c.due).putString("amount$i",c.amount.toString())
        }
        e.apply()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,32,32,24) }
        val title = TextView(this).apply { text="💳 Credit Card Reminder"; textSize=28f; setTextColor(Color.rgb(20,30,50)); setPadding(0,0,0,18) }
        root.addView(title)
        val add = Button(this).apply { text="+ Add Credit Card"; setOnClickListener { addCardDialog() } }
        root.addView(add, LinearLayout.LayoutParams(-1,60))
        val summary = TextView(this).apply { textSize=17f; setPadding(0,18,0,12) }
        root.addView(summary)
        list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        refresh(summary)
    }

    private fun refresh(summary: TextView) {
        list.removeAllViews()
        if (cards.isEmpty()) {
            summary.text = "No cards yet. Add a card to start tracking payments."
            return
        }
        val total = cards.sumOf { it.amount }
        summary.text = "${cards.size} card(s) • Upcoming payments: $${"%.2f".format(total)}"
        cards.forEachIndexed { index,c ->
            val box = LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL; setPadding(22,18,22,18)
                setBackgroundColor(Color.rgb(245,247,250))
            }
            val days = daysUntil(c.due)
            val status = if (days == 0) "Due today" else if (days < 0) "Due date passed" else "Due in $days day(s)"
            val tv = TextView(this).apply {
                text="${c.name}\nDue day: ${c.due}\nPayment: $${"%.2f".format(c.amount)}\n$status"
                textSize=17f; setTextColor(Color.DKGRAY)
            }
            box.addView(tv)
            val remove=Button(this).apply { text="Delete"; setOnClickListener {
                cards.removeAt(index); save(); showHome()
            }}
            box.addView(remove)
            val lp=LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,0,16)
            list.addView(box,lp)
        }
    }

    private fun daysUntil(day:Int):Int {
        val cal=Calendar.getInstance()
        val today=cal.get(Calendar.DAY_OF_MONTH)
        val target=cal.clone() as Calendar
        target.set(Calendar.DAY_OF_MONTH, minOf(day,target.getActualMaximum(Calendar.DAY_OF_MONTH)))
        if (target.timeInMillis < cal.timeInMillis) target.add(Calendar.MONTH,1)
        val d=(target.timeInMillis-cal.timeInMillis)/(24*60*60*1000)
        return d.toInt()
    }

    private fun addCardDialog() {
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,10,40,0) }
        val name=EditText(this).apply { hint="Card name (e.g. Chase Visa)" }
        val due=EditText(this).apply { hint="Due day (1-31)"; inputType=2 }
        val amount=EditText(this).apply { hint="Payment amount"; inputType=8194 }
        layout.addView(name); layout.addView(due); layout.addView(amount)
        AlertDialog.Builder(this).setTitle("Add Credit Card").setView(layout)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Save") { _,_ ->
                val n=name.text.toString().ifBlank{"Credit Card"}
                val d=(due.text.toString().toIntOrNull() ?: 1).coerceIn(1,31)
                val a=amount.text.toString().toDoubleOrNull() ?: 0.0
                cards.add(Card(n,d,a)); save(); showHome()
            }.show()
    }
}
