package xyz.nickr.graphibot;

import com.jtelegram.api.TelegramBot;
import com.jtelegram.api.TelegramBotRegistry;
import com.jtelegram.api.requests.message.framework.ParseMode;
import com.jtelegram.api.requests.message.send.SendText;
import com.jtelegram.api.update.PollingUpdateProvider;
import java.util.concurrent.TimeUnit;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * @author Nick Robson
 */
public class GraphiBot {

    public static void main(String[] args) {
        String authToken = System.getenv("AUTH_TOKEN");
        if (authToken == null || authToken.isEmpty())
            throw new IllegalStateException("No authentication token in environment variable: AUTH_TOKEN");

        new GraphiBot(authToken);

        System.out.println("Listening for events!");
    }

    private TelegramBot bot;
    ExpiringMap<Long, GraphiSession> sessions;

    private GraphiBot(String authToken) {
        TelegramBotRegistry registry = TelegramBotRegistry.builder()
                .updateProvider(new PollingUpdateProvider())
                .build();
        registry.registerBot(authToken, (bot, err) -> {
            if (err != null) {
                throw new IllegalStateException("Invalid authentication token in environment variable: AUTH_TOKEN", err);
            }
            this.bot = bot;
            this.bot.getCommandRegistry().registerCommand(new GraphiBotListener(this));

            this.sessions = ExpiringMap.builder()
                    .expirationPolicy(ExpirationPolicy.ACCESSED)
                    .expiration(30, TimeUnit.MINUTES)
                    .entryLoader(GraphiSession::new)
                    .asyncExpirationListener((id, session) -> {
                        String message = "Session expired.";
                        if (session.getSize() > 0 && !session.isEmpty()) {
                            String expiryMessage = session.getExpiryMessage();
                            message += "\n\n" + expiryMessage;
                        }
                        SendText sendText = SendText.builder()
                                .text(message)
                                .parseMode(ParseMode.MARKDOWN)
                                .build();
                        bot.perform(sendText);
                    })
                    .build();
        });
    }

    public TelegramBot getTelegramBot() {
        return bot;
    }

    public GraphiSession getSession(long id) {
        return sessions.get(id);
    }

}
