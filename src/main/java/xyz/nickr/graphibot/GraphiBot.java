package xyz.nickr.graphibot;

import java.util.concurrent.TimeUnit;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;

/**
 * @author Nick Robson
 */
public class GraphiBot {

    public static void main(String[] args) {
        String authToken = System.getenv("AUTH_TOKEN");
        if (authToken == null || authToken.isEmpty())
            throw new IllegalStateException("No authentication token in environment variable: AUTH_TOKEN");

        GraphiBot bot = new GraphiBot(authToken);
        bot.start();

        System.out.println("Listening for events!");
    }

    private final TelegramBot bot;
    final ExpiringMap<Long, GraphiSession> sessions;

    public GraphiBot(String authToken) {
        this.bot = TelegramBot.login(authToken);
        if (this.bot == null)
            throw new IllegalStateException("Invalid authentication token in environment variable: AUTH_TOKEN");
        this.bot.getEventsManager().register(new GraphiBotListener(this));
        this.sessions = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .expiration(30, TimeUnit.MINUTES)
                .entryLoader(GraphiSession::new)
                .asyncExpirationListener((id, session) -> {
                    Chat chat = bot.getChat(id);
                    if (chat != null) {
                        chat.sendMessage("Session expired.");
                        if (session.getSize() > 0 && !session.isEmpty()) {
                            String message = session.getExpiryMessage();
                            chat.sendMessage(SendableTextMessage.markdown(message).build());
                        }
                    }
                })
                .build();
    }

    public void start() {
        this.bot.startUpdates(true);
    }

    public GraphiSession getSession(long id) {
        return sessions.get(id);
    }

}
