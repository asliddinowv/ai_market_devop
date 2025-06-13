package com.company;

import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;

public class MyBot extends TelegramLongPollingBot {

    // Mahsulotlar ro'yxati
    private final List<Product> products = Arrays.asList(
            new Product(1, "Erkaklar klassik ko'ylagi", 150000, "https://images.unsplash.com/photo-1621072156002-e2fccdc0b176", "Yuqori sifatli erkaklar ko'ylagi"),
            new Product(2, "Ayollar bluzasi", 120000, "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1", "Zamonaviy dizayndagi ayollar bluzasi"),
            new Product(3, "Jin shim", 200000, "https://images.unsplash.com/photo-1541840031508-326b77c9a17e", "Klassik jin shim"),
            new Product(4, "Ayollar ko'ylagi", 180000, "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446", "Chiroyli ayollar ko'ylagi"),
            new Product(5, "Erkaklar futbolkasi", 80000, "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab", "Sport va kundalik uchun futbolka"),
            new Product(6, "Ayollar jinsi", 160000, "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1", "Moda jinsi"),
            new Product(7, "Erkaklar kostyumi", 450000, "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d", "Rasmiy marosimlar uchun kostyum"),
            new Product(8, "Ayollar yubkasi", 100000, "https://images.unsplash.com/photo-1583496661160-fb5886a13d14", "Zamonaviy yubka"),
            new Product(9, "Erkaklar sportivka", 250000, "https://images.unsplash.com/photo-1556821840-3a63f95609a7", "Sport kiyimi to'plami"),
            new Product(10, "Ayollar kurtka", 280000, "https://images.unsplash.com/photo-1551028719-00167b16eac5", "Qish uchun issiq kurtka")
    );

    // Savat (har bir foydalanuvchi uchun)
    private final Map<Long, List<CartItem>> userCarts = new HashMap<>();

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

        switch (text) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;
            case "🛍️ Mahsulotlar":
                showProducts(chatId);
                break;
            case "🛒 Savat":
                showCart(chatId);
                break;
            case "📞 Aloqa":
                showContact(chatId);
                break;
            case "ℹ️ Ma'lumot":
                showInfo(chatId);
                break;
            default:
                sendMessage(chatId, "Iltimos, menyudan tanlang 👇");
        }
    }

    @SneakyThrows
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        if (data.startsWith("product_")) {
            int productId = Integer.parseInt(data.split("_")[1]);
            showProductDetail(chatId, productId);
        } else if (data.startsWith("add_")) {
            int productId = Integer.parseInt(data.split("_")[1]);
            addToCart(chatId, productId);
        } else if (data.startsWith("remove_")) {
            int productId = Integer.parseInt(data.split("_")[1]);
            removeFromCart(chatId, productId);
        } else if (data.equals("clear_cart")) {
            clearCart(chatId);
        } else if (data.equals("checkout")) {
            checkout(chatId);
        } else if (data.equals("back_to_products")) {
            showProducts(chatId);
        }
    }

    @SneakyThrows
    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "🎉 *Kiyim-Kechak Do'konimizga Xush Kelibsiz!* 🎉\n\n" +
                "Bizda eng so'nggi moda va yuqori sifatli kiyimlar mavjud!\n\n" +
                "🛍️ Mahsulotlarimizni ko'rish\n" +
                "🛒 Savatchangizni tekshirish\n" +
                "📞 Biz bilan bog'lanish\n\n" +
                "*Hoziroq xarid qilishni boshlang!*";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(welcomeText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(getMainKeyboard());

        execute(message);
    }

    @SneakyThrows
    private void showProducts(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🛍️ *Bizning mahsulotlarimiz:*");
        message.setParseMode("Markdown");
        message.setReplyMarkup(getProductsKeyboard());

        execute(message);
    }

    @SneakyThrows
    private void showProductDetail(long chatId, int productId) {
        Product product = products.stream()
                .filter(p -> p.getId() == productId)
                .findFirst()
                .orElse(null);

        if (product == null) return;

        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        photo.setPhoto(new InputFile(product.getImageUrl()));

        String caption = String.format(
                "*%s*\n\n" +
                        "💰 Narxi: *%,d so'm*\n\n" +
                        "📝 Ta'rif: %s\n\n" +
                        "Qo'shish uchun tugmani bosing 👇",
                product.getName(),
                product.getPrice(),
                product.getDescription()
        );

        photo.setCaption(caption);
        photo.setParseMode("Markdown");
        photo.setReplyMarkup(getProductDetailKeyboard(productId));

        execute(photo);
    }

    @SneakyThrows
    private void showCart(long chatId) {
        List<CartItem> cart = userCarts.getOrDefault(chatId, new ArrayList<>());

        if (cart.isEmpty()) {
            sendMessage(chatId, "🛒 Savatingiz bo'sh\n\nMahsulot qo'shish uchun 'Mahsulotlar' bo'limiga o'ting");
            return;
        }

        StringBuilder text = new StringBuilder("🛒 *Savatingiz:*\n\n");
        int total = 0;

        for (CartItem item : cart) {
            Product product = getProductById(item.getProductId());
            if (product != null) {
                int itemTotal = product.getPrice() * item.getQuantity();
                text.append(String.format("• %s\n  Miqdor: %d × %,d = *%,d so'm*\n\n",
                        product.getName(), item.getQuantity(), product.getPrice(), itemTotal));
                total += itemTotal;
            }
        }

        text.append(String.format("💰 *Jami: %,d so'm*", total));

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(getCartKeyboard());

        execute(message);
    }

    @SneakyThrows
    private void showContact(long chatId) {
        String contactText = "📞 *Biz bilan bog'laning:*\n\n" +
                "📱 Telefon: +998 90 123 45 67\n" +
                "📱 Telegram: @kiyim_dokani\n" +
                "📧 Email: info@kiyimdokani.uz\n" +
                "🏢 Manzil: Toshkent sh., Amir Temur ko'chasi 15\n\n" +
                "🕒 Ish vaqti: 9:00 - 21:00 (har kuni)";

        sendMessage(chatId, contactText);
    }

    @SneakyThrows
    private void showInfo(long chatId) {
        String infoText = "ℹ️ *Do'kon haqida ma'lumot:*\n\n" +
                "🎯 Bizning maqsadimiz - sizga eng yaxshi kiyimlarni taqdim etish!\n\n" +
                "✅ Yuqori sifat\n" +
                "✅ Arzon narxlar\n" +
                "✅ Tez yetkazib berish\n" +
                "✅ Kafolat beriladigan xizmat\n\n" +
                "🚚 *Yetkazib berish:*\n" +
                "• Toshkent bo'ylab - 20,000 so'm\n" +
                "• Viloyatlarga - 35,000 so'm\n" +
                "• 500,000 so'mdan yuqori xaridlarda - BEPUL!\n\n" +
                "💳 *To'lov usullari:*\n" +
                "• Naqd pul\n" +
                "• Plastik karta\n" +
                "• Bank o'tkazmasi";

        sendMessage(chatId, infoText);
    }

    private void addToCart(long chatId, int productId) {
        List<CartItem> cart = userCarts.computeIfAbsent(chatId, k -> new ArrayList<>());

        CartItem existingItem = cart.stream()
                .filter(item -> item.getProductId() == productId)
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + 1);
        } else {
            cart.add(new CartItem(productId, 1));
        }

        Product product = getProductById(productId);
        sendMessage(chatId, "✅ " + product.getName() + " savatga qo'shildi!");
    }

    private void removeFromCart(long chatId, int productId) {
        List<CartItem> cart = userCarts.get(chatId);
        if (cart != null) {
            cart.removeIf(item -> item.getProductId() == productId);
            sendMessage(chatId, "🗑️ Mahsulot savatdan olib tashlandi");
        }
    }

    private void clearCart(long chatId) {
        userCarts.remove(chatId);
        sendMessage(chatId, "🗑️ Savat tozalandi");
    }

    @SneakyThrows
    private void checkout(long chatId) {
        List<CartItem> cart = userCarts.get(chatId);
        if (cart == null || cart.isEmpty()) {
            sendMessage(chatId, "Savatingiz bo'sh!");
            return;
        }

        int total = cart.stream()
                .mapToInt(item -> {
                    Product product = getProductById(item.getProductId());
                    return product != null ? product.getPrice() * item.getQuantity() : 0;
                })
                .sum();

        String orderText = "✅ *Buyurtmangiz qabul qilindi!*\n\n" +
                String.format("💰 Jami summa: *%,d so'm*\n\n", total) +
                "📞 Tez orada operatorlarimiz siz bilan bog'lanishadi.\n\n" +
                "🚚 Yetkazib berish 1-2 kun ichida amalga oshiriladi.\n\n" +
                "*Xarid uchun rahmat!* 🙏";

        clearCart(chatId);
        sendMessage(chatId, orderText);
    }

    private Product getProductById(int id) {
        return products.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
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
        row1.add(new KeyboardButton("🛍️ Mahsulotlar"));
        row1.add(new KeyboardButton("🛒 Savat"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📞 Aloqa"));
        row2.add(new KeyboardButton("ℹ️ Ma'lumot"));

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardMarkup getProductsKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Product product : products) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getName() + " - " + String.format("%,d so'm", product.getPrice()));
            button.setCallbackData("product_" + product.getId());
            row.add(button);
            rows.add(row);
        }

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardMarkup getProductDetailKeyboard(int productId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("🛒 Savatga qo'shish");
        addButton.setCallbackData("add_" + productId);
        row1.add(addButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Orqaga");
        backButton.setCallbackData("back_to_products");
        row2.add(backButton);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardMarkup getCartKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton checkoutButton = new InlineKeyboardButton();
        checkoutButton.setText("✅ Buyurtma berish");
        checkoutButton.setCallbackData("checkout");
        row1.add(checkoutButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton clearButton = new InlineKeyboardButton();
        clearButton.setText("🗑️ Savatni tozalash");
        clearButton.setCallbackData("clear_cart");
        row2.add(clearButton);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    // Product sinfi
    public static class Product {
        private int id;
        private String name;
        private int price;
        private String imageUrl;
        private String description;

        public Product(int id, String name, int price, String imageUrl, String description) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
            this.description = description;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public int getPrice() { return price; }
        public String getImageUrl() { return imageUrl; }
        public String getDescription() { return description; }
    }

    // CartItem sinfi
    public static class CartItem {
        private int productId;
        private int quantity;

        public CartItem(int productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        // Getters and Setters
        public int getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}