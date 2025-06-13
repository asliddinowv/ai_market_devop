package com.company;

import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MyBot extends TelegramLongPollingBot {

    // Har bir foydalanuvchining vazifalar ro'yxati
    private final Map<Long, List<TodoTask>> userTasks = new HashMap<>();

    // Foydalanuvchi holatini saqlash (vazifa qo'shish jarayonida bo'lsa)
    private final Map<Long, UserState> userStates = new HashMap<>();

    // Vazifa ID generatori
    private int taskIdCounter = 1;

    public MyBot() {
        super("7615437880:AAH5BXF9dQ1UBnmlV-nhXdGnZ6XbBQxr-cA");
    }

    @Override
    public String getBotUsername() {
        return "@javacoursespdp_bot";
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    @SneakyThrows
    private void handleTextMessage(Message message) {
        long chatId = message.getChatId();
        String text = message.getText();

        // Foydalanuvchi holatini tekshirish
        UserState currentState = userStates.get(chatId);

        if (currentState == UserState.WAITING_FOR_TASK) {
            // Yangi vazifa qo'shish
            addNewTask(chatId, text);
            userStates.remove(chatId);
            return;
        }

        switch (text) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;
            case "📝 Vazifa qo'shish":
                startAddingTask(chatId);
                break;
            case "📋 Vazifalar ro'yxati":
                showAllTasks(chatId);
                break;
            case "✅ Bajarilgan vazifalar":
                showCompletedTasks(chatId);
                break;
            case "⏳ Bajarilmagan vazifalar":
                showPendingTasks(chatId);
                break;
            case "📊 Statistika":
                showStatistics(chatId);
                break;
            case "🗑️ Hammasini o'chirish":
                confirmClearAll(chatId);
                break;
            case "ℹ️ Yordam":
                showHelp(chatId);
                break;
            default:
                sendMessage(chatId, "❌ Noto'g'ri buyruq. Menyudan tanlang yoki /start bosing.");
        }
    }

    @SneakyThrows
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        if (data.startsWith("complete_")) {
            int taskId = Integer.parseInt(data.split("_")[1]);
            completeTask(chatId, taskId);
        } else if (data.startsWith("delete_")) {
            int taskId = Integer.parseInt(data.split("_")[1]);
            deleteTask(chatId, taskId);
        } else if (data.startsWith("uncomplete_")) {
            int taskId = Integer.parseInt(data.split("_")[1]);
            uncompleteTask(chatId, taskId);
        } else if (data.equals("clear_all_confirm")) {
            clearAllTasks(chatId);
        } else if (data.equals("clear_all_cancel")) {
            sendMessage(chatId, "❌ Bekor qilindi");
        } else if (data.equals("show_all")) {
            showAllTasks(chatId);
        } else if (data.equals("show_pending")) {
            showPendingTasks(chatId);
        } else if (data.equals("show_completed")) {
            showCompletedTasks(chatId);
        }
    }

    @SneakyThrows
    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "🎯 *ToDo Bot'ga Xush Kelibsiz!* 🎯\n\n" +
                "Bu bot sizga vazifalarni boshqarishda yordam beradi:\n\n" +
                "📝 Yangi vazifa qo'shish\n" +
                "📋 Vazifalar ro'yxatini ko'rish\n" +
                "✅ Vazifalarni bajarilgan deb belgilash\n" +
                "🗑️ Vazifalarni o'chirish\n" +
                "📊 Statistikani ko'rish\n\n" +
                "*Hoziroq boshlang!*";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(welcomeText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(getMainKeyboard());

        execute(message);
    }

    @SneakyThrows
    private void startAddingTask(long chatId) {
        userStates.put(chatId, UserState.WAITING_FOR_TASK);
        sendMessage(chatId, "📝 *Yangi vazifa qo'shish*\n\nVazifa matnini kiriting:");
    }

    @SneakyThrows
    private void addNewTask(long chatId, String taskText) {
        List<TodoTask> tasks = userTasks.computeIfAbsent(chatId, k -> new ArrayList<>());

        TodoTask newTask = new TodoTask(
                taskIdCounter++,
                taskText,
                LocalDateTime.now(),
                false
        );

        tasks.add(newTask);

        sendMessage(chatId, "✅ *Vazifa muvaffaqiyatli qo'shildi!*\n\n" +
                "📝 Vazifa: " + taskText + "\n" +
                "🕒 Vaqt: " + newTask.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
    }

    @SneakyThrows
    private void showAllTasks(long chatId) {
        List<TodoTask> tasks = userTasks.getOrDefault(chatId, new ArrayList<>());

        if (tasks.isEmpty()) {
            sendMessage(chatId, "📋 *Vazifalar ro'yxati bo'sh*\n\nYangi vazifa qo'shish uchun '📝 Vazifa qo'shish' tugmasini bosing.");
            return;
        }

        StringBuilder text = new StringBuilder("📋 *Barcha vazifalar:*\n\n");

        for (TodoTask task : tasks) {
            String status = task.isCompleted() ? "✅" : "⏳";
            text.append(String.format("%s *%d.* %s\n", status, task.getId(), task.getText()));
            text.append(String.format("🕒 %s\n\n",
                    task.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(getTasksKeyboard(tasks));

        execute(message);
    }

    @SneakyThrows
    private void showPendingTasks(long chatId) {
        List<TodoTask> allTasks = userTasks.getOrDefault(chatId, new ArrayList<>());
        List<TodoTask> pendingTasks = new ArrayList<>();

        for (TodoTask task : allTasks) {
            if (!task.isCompleted()) {
                pendingTasks.add(task);
            }
        }

        if (pendingTasks.isEmpty()) {
            sendMessage(chatId, "⏳ *Bajarilmagan vazifalar yo'q*\n\nBarcha vazifalar bajarilgan! 🎉");
            return;
        }

        StringBuilder text = new StringBuilder("⏳ *Bajarilmagan vazifalar:*\n\n");

        for (TodoTask task : pendingTasks) {
            text.append(String.format("⏳ *%d.* %s\n", task.getId(), task.getText()));
            text.append(String.format("🕒 %s\n\n",
                    task.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(getTasksKeyboard(pendingTasks));

        execute(message);
    }

    @SneakyThrows
    private void showCompletedTasks(long chatId) {
        List<TodoTask> allTasks = userTasks.getOrDefault(chatId, new ArrayList<>());
        List<TodoTask> completedTasks = new ArrayList<>();

        for (TodoTask task : allTasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            }
        }

        if (completedTasks.isEmpty()) {
            sendMessage(chatId, "✅ *Bajarilgan vazifalar yo'q*\n\nHali hech qanday vazifa bajarilmagan.");
            return;
        }

        StringBuilder text = new StringBuilder("✅ *Bajarilgan vazifalar:*\n\n");

        for (TodoTask task : completedTasks) {
            text.append(String.format("✅ *%d.* %s\n", task.getId(), task.getText()));
            text.append(String.format("🕒 %s\n",
                    task.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
            if (task.getCompletedAt() != null) {
                text.append(String.format("✅ Bajarildi: %s\n",
                        task.getCompletedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
            }
            text.append("\n");
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(getCompletedTasksKeyboard(completedTasks));

        execute(message);
    }

    @SneakyThrows
    private void showStatistics(long chatId) {
        List<TodoTask> tasks = userTasks.getOrDefault(chatId, new ArrayList<>());

        if (tasks.isEmpty()) {
            sendMessage(chatId, "📊 *Statistika*\n\nHali vazifalar yo'q.");
            return;
        }

        int totalTasks = tasks.size();
        int completedTasks = 0;
        int pendingTasks = 0;

        for (TodoTask task : tasks) {
            if (task.isCompleted()) {
                completedTasks++;
            } else {
                pendingTasks++;
            }
        }

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;

        String statisticsText = String.format(
                "📊 *Statistika*\n\n" +
                        "📝 Jami vazifalar: *%d*\n" +
                        "✅ Bajarilgan: *%d*\n" +
                        "⏳ Bajarilmagan: *%d*\n" +
                        "📈 Bajarilish foizi: *%.1f%%*\n\n" +
                        "🎯 Davom eting!",
                totalTasks, completedTasks, pendingTasks, completionRate
        );

        sendMessage(chatId, statisticsText);
    }

    @SneakyThrows
    private void confirmClearAll(long chatId) {
        List<TodoTask> tasks = userTasks.getOrDefault(chatId, new ArrayList<>());

        if (tasks.isEmpty()) {
            sendMessage(chatId, "🗑️ O'chiradigan vazifalar yo'q.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚠️ *Diqqat!*\n\nBarcha vazifalarni o'chirishni xohlaysizmi?\n\nBu amalni qaytarib bo'lmaydi!");
        message.setParseMode("Markdown");
        message.setReplyMarkup(getClearAllKeyboard());

        execute(message);
    }

    @SneakyThrows
    private void clearAllTasks(long chatId) {
        userTasks.remove(chatId);
        sendMessage(chatId, "🗑️ *Barcha vazifalar o'chirildi*\n\nYangi vazifa qo'shishni boshlashingiz mumkin!");
    }

    @SneakyThrows
    private void showHelp(long chatId) {
        String helpText = "ℹ️ *ToDo Bot Yordam*\n\n" +
                "*Asosiy funksiyalar:*\n\n" +
                "📝 *Vazifa qo'shish* - Yangi vazifa yaratish\n" +
                "📋 *Vazifalar ro'yxati* - Barcha vazifalarni ko'rish\n" +
                "✅ *Bajarilgan vazifalar* - Tugallangan vazifalar\n" +
                "⏳ *Bajarilmagan vazifalar* - Kutilayotgan vazifalar\n" +
                "📊 *Statistika* - Umumiy ma'lumotlar\n" +
                "🗑️ *Hammasini o'chirish* - Barcha vazifalarni tozalash\n\n" +
                "*Tugmalar:*\n" +
                "✅ - Vazifani bajarilgan deb belgilash\n" +
                "❌ - Vazifani o'chirish\n" +
                "↩️ - Vazifani qayta bajarilmagan qilish\n\n" +
                "*Maslahat:* Vazifalarni qisqa va aniq yozing!";

        sendMessage(chatId, helpText);
    }

    private void completeTask(long chatId, int taskId) {
        List<TodoTask> tasks = userTasks.get(chatId);
        if (tasks != null) {
            for (TodoTask task : tasks) {
                if (task.getId() == taskId) {
                    task.setCompleted(true);
                    task.setCompletedAt(LocalDateTime.now());
                    sendMessage(chatId, "✅ Vazifa bajarilgan deb belgilandi:\n*" + task.getText() + "*");
                    return;
                }
            }
        }
        sendMessage(chatId, "❌ Vazifa topilmadi");
    }

    private void uncompleteTask(long chatId, int taskId) {
        List<TodoTask> tasks = userTasks.get(chatId);
        if (tasks != null) {
            for (TodoTask task : tasks) {
                if (task.getId() == taskId) {
                    task.setCompleted(false);
                    task.setCompletedAt(null);
                    sendMessage(chatId, "⏳ Vazifa qayta bajarilmagan qilindi:\n*" + task.getText() + "*");
                    return;
                }
            }
        }
        sendMessage(chatId, "❌ Vazifa topilmadi");
    }

    private void deleteTask(long chatId, int taskId) {
        List<TodoTask> tasks = userTasks.get(chatId);
        if (tasks != null) {
            TodoTask taskToRemove = null;
            for (TodoTask task : tasks) {
                if (task.getId() == taskId) {
                    taskToRemove = task;
                    break;
                }
            }
            if (taskToRemove != null) {
                tasks.remove(taskToRemove);
                sendMessage(chatId, "🗑️ Vazifa o'chirildi:\n*" + taskToRemove.getText() + "*");
                return;
            }
        }
        sendMessage(chatId, "❌ Vazifa topilmadi");
    }

    @SneakyThrows
    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        execute(message);
    }

    private ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📝 Vazifa qo'shish"));
        row1.add(new KeyboardButton("📋 Vazifalar ro'yxati"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("✅ Bajarilgan vazifalar"));
        row2.add(new KeyboardButton("⏳ Bajarilmagan vazifalar"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📊 Statistika"));
        row3.add(new KeyboardButton("🗑️ Hammasini o'chirish"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("ℹ️ Yordam"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardMarkup getTasksKeyboard(List<TodoTask> tasks) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (TodoTask task : tasks) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            if (!task.isCompleted()) {
                InlineKeyboardButton completeBtn = new InlineKeyboardButton();
                completeBtn.setText("✅");
                completeBtn.setCallbackData("complete_" + task.getId());
                row.add(completeBtn);
            }

            InlineKeyboardButton deleteBtn = new InlineKeyboardButton();
            deleteBtn.setText("🗑️");
            deleteBtn.setCallbackData("delete_" + task.getId());
            row.add(deleteBtn);

            rows.add(row);
        }

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardMarkup getCompletedTasksKeyboard(List<TodoTask> tasks) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (TodoTask task : tasks) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton uncompleteBtn = new InlineKeyboardButton();
            uncompleteBtn.setText("↩️");
            uncompleteBtn.setCallbackData("uncomplete_" + task.getId());
            row.add(uncompleteBtn);

            InlineKeyboardButton deleteBtn = new InlineKeyboardButton();
            deleteBtn.setText("🗑️");
            deleteBtn.setCallbackData("delete_" + task.getId());
            row.add(deleteBtn);

            rows.add(row);
        }

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardMarkup getClearAllKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
        confirmBtn.setText("✅ Ha, o'chirish");
        confirmBtn.setCallbackData("clear_all_confirm");
        row.add(confirmBtn);

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Bekor qilish");
        cancelBtn.setCallbackData("clear_all_cancel");
        row.add(cancelBtn);

        rows.add(row);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    // TodoTask sinfi
    public static class TodoTask {
        private int id;
        private String text;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private boolean completed;

        public TodoTask(int id, String text, LocalDateTime createdAt, boolean completed) {
            this.id = id;
            this.text = text;
            this.createdAt = createdAt;
            this.completed = completed;
        }

        // Getters and Setters
        public int getId() { return id; }
        public String getText() { return text; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public boolean isCompleted() { return completed; }

        public void setCompleted(boolean completed) { this.completed = completed; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }

    // UserState enum
    public enum UserState {
        WAITING_FOR_TASK
    }
}