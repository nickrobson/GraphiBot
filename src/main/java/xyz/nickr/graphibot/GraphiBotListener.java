package xyz.nickr.graphibot;

import com.jtelegram.api.commands.Command;
import com.jtelegram.api.commands.filters.AbstractCommandFilter;
import com.jtelegram.api.commands.filters.MentionFilter;
import com.jtelegram.api.commands.filters.TextFilter;
import com.jtelegram.api.events.message.TextMessageEvent;
import com.jtelegram.api.message.input.file.LocalInputFile;
import com.jtelegram.api.requests.message.framework.ParseMode;
import com.jtelegram.api.requests.message.send.SendDocument;
import com.jtelegram.api.requests.message.send.SendPhoto;
import com.jtelegram.api.requests.message.send.SendText;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import xyz.nickr.graphibot.actions.ColorAction;
import xyz.nickr.graphibot.actions.GraphiAction;
import xyz.nickr.graphibot.exceptions.InvalidActionNumber;
import xyz.nickr.graphibot.exceptions.InvalidActionString;
import xyz.nickr.graphibot.exceptions.NoSizeSet;
import xyz.nickr.graphibot.exceptions.NoSuchAction;
import xyz.nickr.graphibot.exceptions.NotInCanvas;
import xyz.nickr.graphibot.exceptions.NothingToRedo;
import xyz.nickr.graphibot.exceptions.NothingToUndo;

/**
 * @author Nick Robson
 */
public class GraphiBotListener extends AbstractCommandFilter {

    private static final Set<String> commands = new HashSet<>(
            Arrays.asList("start", "delete", "help", "size", "background", "draw", "undo", "redo", "remove", "clear", "actions", "file")
    );

    private static GraphiBot bot;

    public GraphiBotListener(GraphiBot bot) {
        super(
                new MentionFilter(
                        new TextFilter("start", false, start()),
                        new TextFilter("delete", false, delete()),
                        new TextFilter("help", false, help()),
                        new TextFilter("size", false, size()),
                        new TextFilter("background", false, background()),
                        new TextFilter("draw", false, draw()),
                        new TextFilter("undo", false, undo()),
                        new TextFilter("redo", false, redo()),
                        new TextFilter("remove", false, remove()),
                        new TextFilter("clear", false, clear()),
                        new TextFilter("actions", false, actions()),
                        new TextFilter("file", false, file())
                )
        );
        GraphiBotListener.bot = bot;
    }

    @Override
    protected boolean preTest(TextMessageEvent textMessageEvent, Command command) {
        if (commands.contains(command.getBaseCommand().toLowerCase())) {
            System.out.println(command);
            return true;
        }
        return false;
    }

    private static CommandHandler start() {
        return (event, command, session) -> {
            reply(event, "Hello! I'm able to help you realise your creative talents.\nTo see what commands are available, use /help.");
        };
    }

    private static CommandHandler delete() {
        return (event, command, session) -> {
            if (session.getSize() < 0) {
                reply(event, "You don't have an active session!");
            } else if (bot.sessions.remove(event.getMessage().getSender().getId(), session)) {
                reply(event, "Deleted your session.");
                reply(event, session.getExpiryMessage(), ParseMode.MARKDOWN);
            }
        };
    }

    private static CommandHandler help() {
        return (event, command, session) -> {
            Map<String, String> commands = new LinkedHashMap<String, String>() {
                {
                    put("help", "see this help menu");
                    put("actions", "see the actions performed");
                    put("size", "set the size of the canvas");
                    put("draw", "draw stuff");
                    put("background", "set the background color");
                    put("undo", "undo the last action");
                    put("redo", "redo an undone action");
                    put("remove", "remove the nth action");
                    put("clear", "reset to a fresh canvas");
                    put("delete", "delete your drawing session");
                    put("file", "get your image as a file (uncompressed!)");
                }
            };
            StringBuilder message = new StringBuilder("*Commands:*");
            commands.forEach((k, v) -> message.append("\n  /").append(k).append(" - ").append(v));
            reply(event, message.toString(), ParseMode.MARKDOWN);
        };
    }

    private static CommandHandler size() {
        return (event, command, session) -> {
            String argsString = command.getArgsAsText();
            if (argsString.isEmpty()) {
                reply(event, "*Usage*: `/size [size]`\nMin: " + GraphiSession.MIN_SIZE + "\nMax: " + GraphiSession.MAX_SIZE, ParseMode.MARKDOWN);
            } else {
                int size;
                try {
                    size = Integer.parseInt(argsString);
                } catch (NumberFormatException ex) {
                    reply(event, "Invalid number.");
                    return;
                }
                if (size < GraphiSession.MIN_SIZE || size > GraphiSession.MAX_SIZE) {
                    reply(event, "Size must be between " + GraphiSession.MIN_SIZE + " and " + GraphiSession.MAX_SIZE);
                } else {
                    session.setSize(size);
                    reply(event, "Size set to " + size + "x" + size);
                    if (!session.isEmpty()) {
                        show(event, session);
                    }
                }
            }
        };
    }

    private static CommandHandler background() {
        return (event, command, session) -> {
            String argsString = command.getArgsAsText();
            if (argsString.isEmpty()) {
                reply(event, "*Usage*: `/background [color]`", ParseMode.MARKDOWN);
            } else {
                Color c = ColorAction.getColor(argsString, true);
                if (c != null) {
                    session.setBackground(c);
                    session.setBackgroundString(argsString);
                    reply(event, "Set background to " + argsString);
                    if (!session.isEmpty()) {
                        show(event, session);
                    }
                } else {
                    reply(event, "Not a valid color: '" + argsString + "'");
                }
            }
        };
    }

    private static CommandHandler draw() {
        return (event, command, session) -> {
            String argsString = command.getArgsAsText();
            if (argsString.isEmpty()) {
                String message = "*Usage:* `/draw [command] [arguments]`";
                message += "\n_Available drawing actions:_";
                for (Supplier<GraphiAction> supplier : GraphiAction.ACTIONS_SET) {
                    GraphiAction action = supplier.get();
                    message += "\n  - `" + String.join("|", (CharSequence[]) action.getNames()) + " " + action.getDescription() + "`";
                }
                reply(event, message, ParseMode.MARKDOWN);
            } else {
                String[] lines = argsString.split("\\n");
                for (String line : lines)
                    session.accept(line);
                show(event, session);
            }
        };
    }

    private static CommandHandler remove() {
        return (event, command, session) -> {
            String argsString = command.getArgsAsText();
            if (argsString.isEmpty()) {
                reply(event, "*Usage*: `/remove [n]`", ParseMode.MARKDOWN);
            } else {
                int n;
                try {
                    n = Integer.parseInt(argsString);
                } catch (NumberFormatException ex) {
                    reply(event, "Invalid number.");
                    return;
                }
                GraphiAction action = session.remove(n - 1);
                reply(event, "Removed #" + n + ": " + action);
                show(event, session);
            }
        };
    }

    private static CommandHandler undo() {
        return (event, command, session) -> {
            GraphiAction action = session.undo();
            reply(event, "Undone: " + action);
            show(event, session);
        };
    }

    private static CommandHandler redo() {
        return (event, command, session) -> {
            GraphiAction action = session.redo();
            reply(event, "Redone: " + action);
            show(event, session);
        };
    }

    private static CommandHandler clear() {
        return (event, command, session) -> {
            if (session.isEmpty()) {
                reply(event, "You already have a fresh canvas.");
            } else {
                session.clear();
                reply(event, "Cleared actions - fresh canvas!");
                show(event, session);
            }
        };
    }

    private static CommandHandler actions() {
        return (event, command, session) -> {
            String message = toString(session.getActions());
            reply(event, message, ParseMode.MARKDOWN);
        };
    }

    private static CommandHandler file() {
        return (event, command, session) -> {
//            InputStream inputStream = session.toInputStream();
//            SendDocument message = SendDocument.builder()
//                    .document(new InputStreamInputFile(inputStream, "graphibot_session.png"))
//                    .build();
            try {
                File f = File.createTempFile("graphibot_session", ".png");
                ImageIO.write(session.build(), "png", f);
                SendDocument sendPhoto = SendDocument.builder()
                        .chatId(event.getMessage().getChat().getChatId())
                        .document(new LocalInputFile(f))
                        .build();
                bot.getTelegramBot().perform(sendPhoto);
            } catch (IOException e) {
                e.printStackTrace();
            }
            reply(event, "Here's your image!");
        };
    }

    private static void show(TextMessageEvent event, GraphiSession session) {
        if (session.isEmpty()) {
            reply(event, "You have no actions performed! Use /draw.");
            return;
        }

//        InputStream inputStream = session.toInputStream();
//        SendPhoto message = SendPhoto.builder()
//                .photo(new InputStreamInputFile(inputStream, "graphibot_session.png"))
//                .build();

        try {
            File f = File.createTempFile("graphibot_session", ".png");
            ImageIO.write(session.build(), "png", f);
            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(event.getMessage().getChat().getChatId())
                    .photo(new LocalInputFile(f))
                    .build();
            bot.getTelegramBot().perform(sendPhoto);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private interface CommandHandler extends com.jtelegram.api.commands.CommandHandler {
        @Override
        default void onCommand(TextMessageEvent event, Command command) {
            GraphiSession session = bot.getSession(event.getMessage().getSender().getId());
            try {
                handle(event, command, session);
            } catch (NoSizeSet e) {
                reply(event, "You haven't set a size yet.");
            } catch (NoSuchAction e) {
                reply(event, "There is no draw action called '" + e.getAction() + "'.");
            } catch (InvalidActionString e) {
                if (e.getAction() == null)
                    reply(event, "That is an invalid string");
                else {
                    StringBuilder s = new StringBuilder("Cannot parse '" + e.getActionString() + "' for a " + e.getAction().getName());
                    String[] ms = e.getMessages();
                    if (ms.length > 0) {
                        s.append(":");
                        for (String m : ms) {
                            s.append("\n  ").append(m);
                        }
                    }
                    reply(event, s.toString());
                }
            } catch (NothingToUndo e) {
                reply(event, "You haven't got anything to undo!");
            } catch (NothingToRedo e) {
                reply(event, "You haven't got anything to redo!");
            } catch (NotInCanvas e) {
                reply(event, "It looks like the command you just sent would be drawn outside of the canvas!");
            } catch (InvalidActionNumber e) {
                reply(event, "You have " + session.getActions().size() + " actions. " + (e.getNumber() + 1) + " is not valid.");
            } catch (Exception e) {
                reply(event, e.toString());
            }
        }

        void handle(TextMessageEvent event, Command command, GraphiSession session);
    }

    private static void reply(TextMessageEvent event, String text) {
        reply(event, text, ParseMode.NONE);
    }

    private static void reply(TextMessageEvent event, String text, ParseMode parseMode) {
        SendText m = SendText.builder()
                .text(text)
                .chatId(event.getMessage().getChat().getChatId())
                .replyToMessageID(event.getMessage().getMessageId())
                .parseMode(parseMode)
                .build();

        bot.getTelegramBot().perform(m);
    }

    public static String toString(List<GraphiAction> actions) {
        StringBuilder message = new StringBuilder("*Actions performed:*");
        for (GraphiAction action : actions) {
            message.append("\n  ").append(action.toString());
        }
        return message.toString();
    }
}
