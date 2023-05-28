package com.slowlii.pocketchatgpt

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatSettingsActivity : AppCompatActivity() {

    private lateinit var countTokensButton: Button
    private lateinit var textSizeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_settings)



//        // Инициализация Chaquopy
//        if (!Python.isStarted()) {
//            Python.start(AndroidPlatform(this))
//        }

        textSizeButton = findViewById(R.id.textSizeButton)
        textSizeButton.setOnClickListener {
            resizeMessageText()
        }
    }

    fun resizeMessageText() {
//        val text: String = textInput.text.toString()
        val textSize = 16

        val textSizeTextView: TextView = findViewById(R.id.textSizeTextView)

        textSizeTextView.text = "Размер шрифта: $textSize"
    }

//    @SuppressLint("SetTextI18n")
//    @OptIn(DelicateCoroutinesApi::class)
//    fun calculateTokenCount() {
//        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
//        val openAiApiKey = sharedPreferences.getString("OpenAiApiKey", "")
//
//        val textInput: EditText = findViewById(R.id.textInput)
//        val text: String = textInput.text.toString()
//
//        val tokenCountTextView: TextView = findViewById(R.id.tokenCountTextView)
//
//        // Вызов функции askGpt в фоновом потоке с использованием Coroutine
//        GlobalScope.launch(Dispatchers.IO) {
//            val tokenCount = withContext(Dispatchers.Default) {
//                val py = Python.getInstance()
//                val module = py.getModule("chat_gpt")
//                module.callAttr("token_count", text, openAiApiKey).toString()
//            }
//
//            tokenCountTextView.text = "Количество токенов: $tokenCount"
//        }
//    }
}
