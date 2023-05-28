package com.slowlii.pocketchatgpt

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.GestureDetector
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.chaquo.python.*
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(val chatId: String, val role: String, var content: String, val messageId: Int)

class MainActivity : AppCompatActivity() {

    private lateinit var messageContainer: LinearLayout // Контейнер для сообщений
    private lateinit var messageEditText: EditText // Поле ввода сообщения
    private lateinit var sendButton: ImageButton // Кнопка отправки сообщения
    private lateinit var messageScrollView: ScrollView
    private lateinit var sentMessage: String
    private lateinit var dbHelper: DatabaseHelper

    private var hasError: Boolean = false

    private var chatHistory: MutableList<MutableMap<String, String>> = mutableListOf()

    // Идентификатор чата
    private var chatId: String = "Идентификатор чата"

    private var messageId: Int = 0 // рудимент, надо будет с ним разобраться

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация Chaquopy
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Инициализация элементов пользовательского интерфейса
        messageContainer = findViewById(R.id.messageContainer)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        messageScrollView = findViewById(R.id.messageScrollView)

        // Инициализируем dbHelper в методе OnCreate
        dbHelper = DatabaseHelper(this)

        // Инициализируем chatId получая последний идентификатор чата
        chatId = dbHelper.getLastId()

        // Загрузить чат
        displayChatMessages(chatId)
        chatHistory = dbHelper.getChatHistory(chatId)

        // Определение DrawerLayout
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)

        // Определение бокового меню
        val navigationView: NavigationView = findViewById(R.id.navigationView)

        // Добавляем в navigationView меню из 'layout/drawer_menu'
        val headerView = layoutInflater.inflate(R.layout.drawer_menu, navigationView, false)
        navigationView.addHeaderView(headerView)

        // Прослушиваем кнопку настроек ключа API
        val apiKeySettingsButton: Button = headerView.findViewById(R.id.ApiKeyButton)
        apiKeySettingsButton.setOnClickListener {
            val intent = Intent(this, ChatGPTKeyActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

//        // Прослушиваем кнопку настроек
//        val chatSettingsButton: Button = headerView.findViewById(R.id.settingsButton)
//        chatSettingsButton.setOnClickListener {
//            val intent = Intent(this, ChatSettingsActivity::class.java)
//            startActivity(intent)
//            drawerLayout.closeDrawer(GravityCompat.START)
//        }

        // Настройки свайпа бокового окна
        val gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x.minus(e1.x)
                val diffY = e2.y.minus(e1.y)

                if (diffX > SWIPE_THRESHOLD && diffY < SWIPE_THRESHOLD) {
                    drawerLayout.openDrawer(GravityCompat.START)
                    return true
                }
                return false
            }
        })

        val rootView = findViewById<View>(android.R.id.content)

        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        // При нажатии на кнопку меню открывается меню
        val openMenuButton: ImageButton = findViewById(R.id.openMenuButton)
        openMenuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // При нажатии на кнопку удаления чата удаляется чат
        val chatResetButton: ImageButton = findViewById(R.id.chatResetButton)
        chatResetButton.setOnClickListener {
            messageContainer.removeAllViews() // удаляем окошки сообщений
            chatHistory = mutableListOf() // удаляем историю чата
            dbHelper.deleteMessagesByChatId(chatId) // удаляем чат из таблицы чата
        }

        // Отображение диалогового окна при нажатии на кнопку "Выбрать чат"
        val selectChatButton: ImageButton = findViewById(R.id.selectChatButton)
        selectChatButton.setOnClickListener {
            showChatSelectionDialog()
        }

//        // Делаем чат интеллигентным помощником
//        chatHistory.add(mutableMapOf("role" to "system", "content" to "You are a intelligent assistant."))

        // Устанавливаем слушатель событий клавиатуры
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            if (heightDiff > dpToPx(this, 200)) {
                // Клавиатура открыта
                scrollToBottom()
            }
        }

        // Прослушиваем кнопку "Отправить"
        sendButton.setOnClickListener {
            sentMessage = messageEditText.text.toString() // Получение текста сообщения из поля ввода

            if (sentMessage.isNotBlank()) {
                // Проверяем наличие ошибки в предыдущем сообщении собеседника
                if (!hasError) {
                    displaySentMessage(sentMessage) // Отображение отправленного сообщения

                    // При получении сообщения от собеседника
                    dbHelper.addChatMessage(chatId, "user", sentMessage)
                    messageId += 1

                    messageEditText.text.clear() // Очистка поля ввода

                    // Формируем подсказку, объединяя предыдущие сообщения и новое сообщение пользователя
                    chatHistory.add(mutableMapOf("role" to "user", "content" to sentMessage))

                    // Вызов функции askGpt из Python с использованием Chaquopy
                    askGpt(sentMessage)
                } else {
                    // Не удалось отправить сообщение. Предыдущее сообщение содержит ошибку
                    // Скрываем клавиатуру
                    hideKeyboard()
                }
            } else {
                Log.d("TAG", "Отправлено пустое сообщение")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Закройте базу данных при завершении приложения
        dbHelper.close()
    }

    // Отображение диалогового окна с чатами
    private fun showChatSelectionDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_select_chat, null)

        builder.setView(dialogView)

        val dialog = builder.create()
        val cancelButton: Button = dialogView.findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        val chatListRecyclerView: RecyclerView = dialogView.findViewById(R.id.chatList)
        val layoutManager = LinearLayoutManager(this)
        chatListRecyclerView.layoutManager = layoutManager

        val databaseHelper = DatabaseHelper(this)
        var chatList = databaseHelper.getChatList()

        val chatListAdapter = ChatListAdapter(chatList)
        chatListAdapter.setChatId(chatId) // передаем в ChatListAdapter наш chatId
        chatListAdapter.setOnItemClickListener(object : ChatListAdapter.OnItemClickListener {
            override fun onItemClick(chatId: String) {
                dialog.dismiss()
                displayChatMessages(chatId)
            }

            override fun onDeleteClick(chatId: String) {
                // Вызываем метод удаления чата из DatabaseHelper
                databaseHelper.deleteChat(chatId)
                // Обновление списка чатов после удаления
                chatList = databaseHelper.getChatList()
                chatListAdapter.updateChatList(chatList)

                // Если все чаты удалены
                if (chatList.isEmpty()) {
                    addNewChat()
                    dialog.dismiss()
                }
            }
        })
        chatListRecyclerView.adapter = chatListAdapter

        val addChatButton: Button = dialogView.findViewById(R.id.addChatButton)
        addChatButton.setOnClickListener {
            addNewChat()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addNewChat() {
        val newChatId = dbHelper.generateNewChatId()
        this.chatId = newChatId // получаем новый идентификатор чата
        displayChatMessages(chatId)
    }

    private fun displayChatMessages(chatId: String) {
        // Загружаем предыдущий чат

        messageContainer.removeAllViews() // удаляем сообщения предыдущего чата с экрана

        // Выполните операции с базой данных, например, вставку, выборку и т. д.
        // Используйте методы, такие как db.insert(), db.query() и т. д.
        val chatMessages = dbHelper.getChatMessages(chatId)

        for (message in chatMessages) {
            if (message.role == "user") {
                displaySentMessage(message.content)
            } else {
                displayReceivedMessage(message.content)
            }
        }
    }

    // Спрятать клавиатуру
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(messageEditText.windowToken, 0)
    }

    // На сколько выдвинуть меню, чтобы при отпускании оно полностью показалось
    companion object {
        private const val SWIPE_THRESHOLD = 100
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    // Функция для опускания чата в самый конец
    private fun scrollToBottom() {
        messageScrollView.post {
            messageScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    // Функция возвращает chatHistory как json в строке
    private fun getChatHistoryJson(): String {
        val jsonArray = JSONArray(chatHistory)
        return jsonArray.toString()
    }

    // Функция для вызова функции askGpt из Python с использованием Chaquopy
    @OptIn(DelicateCoroutinesApi::class)
    private fun askGpt(content: String) {
        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        val openAiApiKey = sharedPreferences.getString("OpenAiApiKey", "")

        // Вызов функции askGpt в фоновом потоке с использованием Coroutine
        GlobalScope.launch(Dispatchers.IO) {
            val responseText = withContext(Dispatchers.Default) {
                val py = Python.getInstance()
                val module = py.getModule("chat_gpt")
                module.callAttr("askGPT", getChatHistoryJson(), content, openAiApiKey).toString()
            }

            // Обновление интерфейса в главном потоке
            withContext(Dispatchers.Main) {
                if (responseText.startsWith("Ошибка:")) {
                    hasError = true
                    // Выполняем код, если строка начинается с "Ошибка: "
                    displayReceivedMessage(responseText)
                } else {
                    hasError = false
                    // Выполняем код, если строка не начинается с "Ошибка: "
                    chatHistory.add(mutableMapOf("role" to "assistant", "content" to responseText))
                    displayReceivedMessage(responseText)

                    // При получении сообщения от собеседника
                    dbHelper.addChatMessage(chatId, "assistant", responseText)
                    messageId += 1
                }
            }
        }
    }


    // Функция отображения сообщения отправителя
    private fun displaySentMessage(message: String) {
        // Создание нового TextView для отображения отправленного сообщения
        val messageTextView = TextView(this)
        messageTextView.text = message
        messageTextView.setBackgroundResource(R.drawable.sent_message_bg)
        messageTextView.setTextColor(resources.getColor(R.color.sent_message_text_color))
        messageTextView.setTextAppearance(R.style.ChatMessageTextSender) // Стиль текста
        messageTextView.setPadding(16, 8, 16, 8)

        // Установка параметров макета для выравнивания сообщения вправо
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.END

        messageTextView.layoutParams = layoutParams

        // Добавление длительного нажатия для вызова контекстного меню
        messageTextView.setOnLongClickListener { view ->
            showContextMenuForMessage(view, message)
            true
        }

        // Добавление TextView в контейнер сообщений и регистрация для контекстного меню
        messageContainer.addView(messageTextView)
        registerForContextMenu(messageTextView)

        // После добавления сообщения прокрутите чат вниз
        scrollToBottom()
    }

    // Функция отображения сообщения chatGPT
    private fun displayReceivedMessage(message: String) {
        // Создание нового TextView для отображения полученного сообщения
        val messageTextView = TextView(this)
        messageTextView.text = message
        messageTextView.setBackgroundResource(R.drawable.received_message_bg)

        if (hasError) {
            // Устанавливаем красный цвет текста для сообщений с ошибкой
            messageTextView.setTextAppearance(R.style.ChatMessageErrorTextReceiver) // Стиль текста
        } else {
            messageTextView.setTextAppearance(R.style.ChatMessageTextReceiver) // Стиль текста
        }

        messageTextView.setPadding(16, 8, 16, 8)

        // Установка параметров макета для выравнивания сообщения влево
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.START

        messageTextView.layoutParams = layoutParams

        // Добавление длительного нажатия для вызова контекстного меню
        messageTextView.setOnLongClickListener { view ->
            showContextMenuForMessage(view, message)
            true
        }

        // Добавление TextView в контейнер сообщений и регистрация для контекстного меню
        messageContainer.addView(messageTextView)
        registerForContextMenu(messageTextView)

        // Если сообщение является ошибкой, добавьте кнопку "Отправить сообщение заново"
        if (hasError) {
            val retryButton = Button(this)
            retryButton.text = "Отправить сообщение заново"

            // Настройка фона кнопки
            retryButton.setBackgroundResource(R.drawable.received_message_bg) // Замените R.drawable.retry_button_bg на ваш фоновый ресурс

            // Настройка цвета текста кнопки
            retryButton.setTextColor(resources.getColor(R.color.received_message_text_color)) // Замените R.color.retry_button_text_color на ваш цвет текста

            // Настройка оформления кнопки
            retryButton.setTextAppearance(R.style.ChatMessageTextReceiver) // Замените R.style.RetryButtonTextStyle на ваш стиль текста кнопки

            // Установка размеров кнопки и отступов
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT // ширина кнопки в ширину экрана
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL // Размещение по горизонтали посередине
            layoutParams.topMargin = 8 // Отступ сверху
            layoutParams.bottomMargin = 8 // Отступ снизу
            retryButton.layoutParams = layoutParams

            retryButton.setOnClickListener {
                // При нажатии на кнопку вызовите функцию отправки сообщения заново
                resendMessage(sentMessage)
            }
            messageContainer.addView(retryButton)
        }else {
            // После добавления сообщения прокрутите чат вниз
            scrollToBottom()
        }
    }

    // Функция повторной отправки сообщения
    private fun resendMessage(message: String) {
        // Вызов функции askGpt из Python с использованием Chaquopy
        askGpt(message)

        // Удаление сообщения с ошибкой и кнопки "Отправить сообщение заново"
        val childCount = messageContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val childView = messageContainer.getChildAt(i)
            if (childView is TextView && childView.text.toString().startsWith("Ошибка: ")) {
                messageContainer.removeView(childView)
            } else if (childView is Button && childView.text == "Отправить сообщение заново") {
                messageContainer.removeView(childView)
            }
        }
    }

    // Функция показывает контекстное меню сообщения
    private fun showContextMenuForMessage(view: View, message: String) {
        val menu = PopupMenu(this, view) // Создание контекстного меню с привязкой к определенному View
        menu.menuInflater.inflate(R.menu.message_context_menu, menu.menu) // Заполнение меню из ресурса menu/message_context_menu.xml
        menu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_copy_message -> { // Если выбран пункт "Копировать сообщение"
                    copyMessageToClipboard(message) // Вызов метода для копирования сообщения в буфер обмена
                    true
                }
                else -> false
            }
        }
        menu.show() // Показать контекстное меню
    }

    // Функция копирует сообщение в буфер обмена
    private fun copyMessageToClipboard(message: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager // Получение сервиса буфера обмена
        val clip = ClipData.newPlainText("message", message) // Создание ClipData с текстом сообщения
        clipboard.setPrimaryClip(clip) // Установка ClipData в буфер обмена
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v is TextView) {
            menu.setHeaderTitle("Действия с сообщением") // Заголовок контекстного меню
            menuInflater.inflate(R.menu.message_context_menu, menu) // Заполнение меню из ресурса menu/message_context_menu.xml
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val messageView = messageContainer.getChildAt(menuInfo.position) as TextView
        val message = messageView.text.toString()

        return when (item.itemId) {
            R.id.action_copy_message -> {
                copyMessageToClipboard(message)
                true
            }
            // Добавьте другие обработчики элементов меню, если необходимо
            else -> super.onContextItemSelected(item)
        }
    }
}

