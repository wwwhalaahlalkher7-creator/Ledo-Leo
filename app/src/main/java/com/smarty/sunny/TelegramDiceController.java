package com.smarty.sunny;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ⚡ Ultra-Fast Real-time Telegram Bot Controller for Ludo King Dice Rolls.
 * Supports Single & Multi-Roll Sequence Overrides (e.g. b6,b4 or r6,r6,r4).
 *
 * Supported Commands:
 * - b6,b4 or b6,4 or b 6 4  -> Next Blue rolls 6, then 4!
 * - r6,r4 or r6,4           -> Next Red rolls 6, then 4!
 * - g6,g4 or g6,4           -> Next Green rolls 6, then 4!
 * - y6,y4 or y6,4           -> Next Yellow rolls 6, then 4!
 * - 6,4 or 6,6,5            -> Next any-player rolls sequence [6, 4]
 * - all 6,4                 -> All players roll sequence [6, 4]
 * - b6, r6, g6, y6, 6       -> Single roll overrides
 * - clear, reset            -> Clear all queues (Normal Random)
 * - status                  -> Check active queued rolls
 */
public class TelegramDiceController {
    private static final String TAG = "TelegramDiceCtrl";
    private static TelegramDiceController instance;

    // 🔑 Verified Active Telegram Bot Token
    public static String DEFAULT_BOT_TOKEN = "8763631865:AAFeclEG8MIMUhL6IJi3PAJQtzq-r5PhaAA";

    // 🔒 Admin Chat ID: 0 = Allow all, or set your specific Telegram chat ID to lock down access
    private static final long ADMIN_CHAT_ID = 7823358687L;

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> colorQueues = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Integer> anyColorQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ExecutorService executor;
    private long lastUpdateId = 0;
    private String currentToken = "";
    private Context context;
    private SharedPreferences telegramPrefs;

    private TelegramDiceController() {}

    public static synchronized TelegramDiceController getInstance() {
        if (instance == null) {
            instance = new TelegramDiceController();
        }
        return instance;
    }

    public void init(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.telegramPrefs = context.getSharedPreferences("TelegramDicePrefs", Context.MODE_PRIVATE);
        this.lastUpdateId = telegramPrefs.getLong("last_update_id", 0);

        SharedPreferences sp = context.getSharedPreferences("LudoTelegramPrefs", Context.MODE_PRIVATE);
        String savedToken = sp.getString("telegram_bot_token", "");
        String tokenToUse = (savedToken != null && !savedToken.trim().isEmpty()) ? savedToken.trim() : DEFAULT_BOT_TOKEN.trim();
        if (!tokenToUse.isEmpty()) {
            startListening(tokenToUse);
        }
    }

    public void setBotToken(String token) {
        if (token == null) token = "";
        token = token.trim();
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences("LudoTelegramPrefs", Context.MODE_PRIVATE);
            sp.edit().putString("telegram_bot_token", token).apply();
        }
        if (!token.isEmpty()) {
            startListening(token);
        } else {
            stopListening();
        }
    }

    public synchronized void startListening(String token) {
        if (token == null || token.trim().isEmpty()) return;
        this.currentToken = token.trim();

        // Idempotent: If already running, do not spawn duplicate threads
        if (executor != null && !executor.isShutdown()) {
            return;
        }

        isRunning.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.execute(this::pollTelegramUpdates);
        Log.i(TAG, "⚡ Telegram Bot Controller started listening with zero-latency long-polling...");
    }

    public synchronized void stopListening() {
        isRunning.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        Log.i(TAG, "Telegram Bot Controller stopped.");
    }

    /**
     * Clears all queued dice overrides on new game start.
     */
    public void clearQueues() {
        colorQueues.clear();
        anyColorQueue.clear();
        Log.i(TAG, "🧹 Cleared all Telegram dice queues.");
    }

    /**
     * Consumes and returns the override dice value (1..6) for the given color from the queue.
     * Returns 0 if no override is set (normal random dice roll).
     */
    public int consumeDiceValue(String color) {
        if (color == null) color = "";
        color = color.toLowerCase(Locale.ROOT).trim();

        // 1. Check color-specific sequence queue
        ConcurrentLinkedQueue<Integer> queue = colorQueues.get(color);
        if (queue != null) {
            Integer val = queue.poll();
            if (val != null && val >= 1 && val <= 6) {
                Log.i(TAG, "🎲 [BOT OVERRIDE] " + color + " rolled forced value: " + val + " (Remaining in queue: " + queue.size() + ")");
                return val;
            }
        }

        // 2. Check general (any color) sequence queue
        Integer anyVal = anyColorQueue.poll();
        if (anyVal != null && anyVal >= 1 && anyVal <= 6) {
            Log.i(TAG, "🎲 [BOT OVERRIDE] Any-player rolled forced value: " + anyVal + " (Remaining in queue: " + anyColorQueue.size() + ")");
            return anyVal;
        }

        return 0; // 0 means normal random roll
    }

    /**
     * Ultra-fast long polling loop for Telegram getUpdates API.
     */
    private void pollTelegramUpdates() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            HttpURLConnection conn = null;
            try {
                String urlStr = "https://api.telegram.org/bot" + currentToken +
                        "/getUpdates?offset=" + (lastUpdateId + 1) +
                        "&timeout=25&allowed_updates=[\"message\"]";

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("User-Agent", "LudoKingController/1.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(35000);
                conn.setUseCaches(false);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    parseTelegramResponse(response.toString());
                } else {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                Log.e(TAG, "Poll error: " + e.getMessage());
                try {
                    Thread.sleep(250); // fast retry on network hiccup
                } catch (InterruptedException ignored) {
                    break;
                }
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void parseTelegramResponse(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            if (!root.optBoolean("ok", false)) return;

            JSONArray result = root.optJSONArray("result");
            if (result == null || result.length() == 0) return;

            for (int i = 0; i < result.length(); i++) {
                JSONObject update = result.getJSONObject(i);
                long updateId = update.getLong("update_id");
                if (updateId > lastUpdateId) {
                    lastUpdateId = updateId;
                    if (telegramPrefs != null) {
                        telegramPrefs.edit().putLong("last_update_id", lastUpdateId).apply();
                    }
                }

                if (update.has("message")) {
                    JSONObject msg = update.getJSONObject("message");
                    long chatId = msg.getJSONObject("chat").getLong("id");
                    String text = msg.optString("text", "").trim();

                    if (!text.isEmpty()) {
                        handleCommand(chatId, text);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing update: " + e.getMessage());
        }
    }

    private long getAuthorizedAdminChatId() {
        if (telegramPrefs != null) {
            long savedAdmin = telegramPrefs.getLong("admin_chat_id", 0L);
            if (savedAdmin != 0L) return savedAdmin;
        }
        return ADMIN_CHAT_ID;
    }

    private void handleCommand(long chatId, String rawText) {
        String text = rawText.trim().toLowerCase(Locale.ROOT);
        // Remove leading / or . or ! if user typed like /b6,b4
        if (text.startsWith("/") || text.startsWith(".") || text.startsWith("!")) {
            text = text.substring(1).trim();
        }

        // Allow checking chat ID
        if (text.equals("myid") || text.equals("id")) {
            sendMessage(chatId, "🆔 *Your Telegram Chat ID:* `" + chatId + "`\n" +
                    (getAuthorizedAdminChatId() != 0L ? (chatId == getAuthorizedAdminChatId() ? "✅ You are authorized Admin." : "❌ Not authorized.") : "ℹ️ Admin lock is currently open (0L)."));
            return;
        }

        // Allow setting/claiming admin with command: setadmin or auth
        if (text.equals("setadmin") || text.equals("auth")) {
            if (telegramPrefs != null) {
                telegramPrefs.edit().putLong("admin_chat_id", chatId).apply();
            }
            sendMessage(chatId, "🔐 *Authorized!* This Chat ID (`" + chatId + "`) is now set as the active controller Admin.");
            return;
        }

        long authAdmin = getAuthorizedAdminChatId();
        if (authAdmin != 0L && chatId != authAdmin) {
            Log.w(TAG, "Unauthorized access attempt from chatId: " + chatId);
            sendMessage(chatId, "⛔ *Access Denied:* Unauthorized Telegram account. Send `myid` to view your ID.");
            return;
        }

        Log.d(TAG, "⚡ Received Telegram command: " + text);

        // Command: start or help
        if (text.equals("start") || text.equals("help")) {
            String helpMsg = "👑 *Ludo King Sequence Dice Controller*\n\n" +
                    "⚡ *Multi-Roll Sequence Examples:*\n" +
                    "• `b6,b4` or `b6,4` ➔ Blue rolls 6, then 4 next!\n" +
                    "• `r6,r6,r4` ➔ Red rolls 6, 6, then 4!\n" +
                    "• `g6,g5` ➔ Green rolls 6, then 5!\n" +
                    "• `y6,y2` ➔ Yellow rolls 6, then 2!\n" +
                    "• `6,4` ➔ Next any-player rolls 6, then 4!\n" +
                    "• `all 6,4` ➔ All players roll 6, then 4!\n\n" +
                    "🎯 *Single Roll Examples:*\n" +
                    "• `b6`, `r6`, `g6`, `y6`, `6`\n\n" +
                    "⚙️ *Controls:*\n" +
                    "• `clear` ➔ Reset to Normal Random\n" +
                    "• `status` ➔ Check active queued rolls\n\n" +
                    "_Note: Rolls sequence in exact order, then auto-reverts to normal random!_";
            sendMessage(chatId, helpMsg);
            return;
        }

        // Command: status
        if (text.equals("status")) {
            StringBuilder sb = new StringBuilder("📊 *Current Queued Rolls:*\n");
            sb.append("🔴 Red: ").append(formatQueue("red")).append("\n");
            sb.append("🟢 Green: ").append(formatQueue("green")).append("\n");
            sb.append("🔵 Blue: ").append(formatQueue("blue")).append("\n");
            sb.append("🟡 Yellow: ").append(formatQueue("yellow")).append("\n");
            sb.append("🎲 Next Immediate: ").append(anyColorQueue.isEmpty() ? "None" : anyColorQueue.toString()).append("\n\n");
            sb.append("_(0/None means normal random play)_");
            sendMessage(chatId, sb.toString());
            return;
        }

        // Command: clear / reset
        if (text.equals("clear") || text.equals("reset")) {
            clearQueues();
            sendMessage(chatId, "🧹 *Cleared all dice queues!* (Normal Random Active)");
            return;
        }

        // Command: all <sequence> or *<sequence>
        if (text.startsWith("all") || text.startsWith("*")) {
            String seqPart = text.replaceFirst("^(all|\\*)\\s*", "");
            List<Integer> numbers = extractAllNumbers(seqPart);
            if (!numbers.isEmpty()) {
                setQueueForColor("red", numbers);
                setQueueForColor("green", numbers);
                setQueueForColor("blue", numbers);
                setQueueForColor("yellow", numbers);
                anyColorQueue.clear(); // Clear anyColorQueue to prevent duplicate overrides
                sendMessage(chatId, "⚡ *ALL Players queued sequence: " + numbers + "* 🎲 (Instant)");
                return;
            }
        }

        // Parse color-based or general multi-roll sequence (e.g. b6,b4 or b6,4 or r6,r4 or 6,4)
        String[] parts = text.split("[,\\s]+");
        String activeColor = null;
        Map<String, List<Integer>> colorMap = new HashMap<>();
        List<Integer> generalList = new ArrayList<>();

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String partColor = detectColor(part);
            if (partColor != null) {
                activeColor = partColor;
            }

            int num = extractNumber(part);
            if (num >= 1 && num <= 6) {
                if (activeColor != null) {
                    colorMap.computeIfAbsent(activeColor, k -> new ArrayList<>()).add(num);
                } else {
                    generalList.add(num);
                }
            }
        }

        // Reject ambiguous mixed commands
        if (!generalList.isEmpty() && !colorMap.isEmpty()) {
            sendMessage(chatId, "❌ Mixed command. Use either color sequence (e.g. `b6,b4`) or general sequence (e.g. `6,4`).");
            return;
        }

        if (!colorMap.isEmpty()) {
            StringBuilder resp = new StringBuilder("⚡ *Dice Sequence Queued:*\n");
            for (Map.Entry<String, List<Integer>> entry : colorMap.entrySet()) {
                String c = entry.getKey();
                List<Integer> nums = entry.getValue();
                setQueueForColor(c, nums);
                String emoji = c.equals("red") ? "🔴" : c.equals("blue") ? "🔵" : c.equals("green") ? "🟢" : "🟡";
                resp.append(emoji).append(" *").append(capitalize(c)).append(":* ").append(nums).append("\n");
            }
            resp.append("\n_(Auto-reverts to normal random after rolls complete)_");
            sendMessage(chatId, resp.toString().trim());
            return;
        }

        if (!generalList.isEmpty()) {
            anyColorQueue.clear();
            anyColorQueue.addAll(generalList);
            sendMessage(chatId, "🎲 *Next Roll Sequence (Any Color):* " + generalList + " ⚡");
            return;
        }

        sendMessage(chatId, "❓ Unknown command: `" + rawText + "`\nExample: `b6,b4` or `r6,4` or `/help`");
    }

    private String detectColor(String part) {
        if (part == null) return null;
        if (part.startsWith("red") || part.matches("^r\\d*")) return "red";
        if (part.startsWith("blue") || part.matches("^b\\d*")) return "blue";
        if (part.startsWith("green") || part.matches("^g\\d*")) return "green";
        if (part.startsWith("yellow") || part.matches("^y\\d*")) return "yellow";
        return null;
    }

    private void setQueueForColor(String color, List<Integer> nums) {
        ConcurrentLinkedQueue<Integer> queue = colorQueues.computeIfAbsent(color, k -> new ConcurrentLinkedQueue<>());
        queue.clear();
        queue.addAll(nums);
    }

    private String formatQueue(String color) {
        ConcurrentLinkedQueue<Integer> q = colorQueues.get(color);
        if (q == null || q.isEmpty()) return "None";
        return q.toString();
    }

    private List<Integer> extractAllNumbers(String text) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '1' && c <= '6') {
                list.add(c - '0');
            }
        }
        return list;
    }

    private int extractNumber(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                return c - '0';
            }
        }
        return -1;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }

    private void sendMessage(long chatId, String message) {
        if (currentToken == null || currentToken.isEmpty()) return;
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String postUrl = "https://api.telegram.org/bot" + currentToken + "/sendMessage";
                String postData = "chat_id=" + chatId +
                        "&text=" + URLEncoder.encode(message, "UTF-8") +
                        "&parse_mode=Markdown";

                URL url = new URL(postUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                conn.getResponseCode();
            } catch (Exception e) {
                Log.e(TAG, "Send message error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }
}
