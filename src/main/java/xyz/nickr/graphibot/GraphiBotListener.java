package xyz.nickr.graphibot;

import java.awt.Color;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import pro.zackpollard.telegrambot.api.chat.message.send.InputFile;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableDocumentMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendablePhotoMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import xyz.nickr.graphibot.actions.ColorAction;
import xyz.nickr.graphibot.actions.GraphiAction;
import xyz.nickr.graphibot.exceptions.GraphiException;
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
@AllArgsConstructor
public class GraphiBotListener implements Listener {

    private final GraphiBot bot;

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {
        GraphiSession session = bot.getSession(event.getMessage().getSender().getId());

        try {
            String command = event.getCommand();
            String argsString = event.getArgsString().trim();

            System.out.println(event.getMessage().getSender().getUsername() + ": " + event.getContent().getContent());

            if ("start".equals(command)) {
                reply(event, "Hello! I'm able to help you realise your creative talents.\nTo see what commands are available, use /help.");
                return;
            } else if ("delete".equals(command)) {
                if (session.getSize() < 0) {
                    reply(event, "You don't have an active session!");
                    return;
                }
                if (bot.sessions.remove(event.getMessage().getSender().getId(), session)) {
                    reply(event, "Deleted your session.");
                    reply(event, session.getExpiryMessage(), ParseMode.MARKDOWN);
                    return;
                }
            } else if ("help".equals(command)) {
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
                return;
            } else if ("size".equals(command)) {
                if (argsString.isEmpty()) {
                    reply(event, "*Usage*: `/size [size]`\nMin: " + GraphiSession.MIN_SIZE + "\nMax: " + GraphiSession.MAX_SIZE, ParseMode.MARKDOWN);
                    return;
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
                        return;
                    }
                    session.setSize(size);
                    reply(event, "Size set to " + size + "x" + size);
                }
            } else if ("background".equals(command)) {
                if (argsString.isEmpty()) {
                    reply(event, "*Usage*: `/background [color]`", ParseMode.MARKDOWN);
                    return;
                } else {
                    Color c = ColorAction.getColor(argsString, true);
                    if (c != null) {
                        session.setBackground(c);
                        session.setBackgroundString(argsString);
                        reply(event, "Set background to " + argsString);
                        if (session.isEmpty()) {
                            return;
                        }
                    } else {
                        reply(event, "Not a valid color: '" + argsString + "'");
                        return;
                    }
                }
            } else if ("draw".equals(command)) {
                if (argsString.isEmpty()) {
                    String message = "*Usage:* `/draw [command] [arguments]`";
                    message += "\n_Available drawing actions:_";
                    for (Supplier<GraphiAction> supplier : GraphiAction.ACTIONS_SET) {
                        GraphiAction action = supplier.get();
                        message += "\n  - `" + String.join("|", (CharSequence[]) action.getNames()) + " " + action.getDescription() + "`";
                    }
                    reply(event, message, ParseMode.MARKDOWN);
                    return;
                } else {
                    String[] lines = argsString.split("\\n");
                    for (String line : lines)
                        session.accept(line);
                }
            } else if ("undo".equals(command)) {
                GraphiAction action = session.undo();
                reply(event, "Undone: " + action);
            } else if ("redo".equals(command)) {
                GraphiAction action = session.redo();
                reply(event, "Redone: " + action);
            } else if ("remove".equals(command)) {
                if (argsString.isEmpty()) {
                    reply(event, "*Usage*: `/remove [n]`", ParseMode.MARKDOWN);
                    return;
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
                }
            } else if ("clear".equals(command)) {
                if (session.isEmpty()) {
                    reply(event, "You already have a fresh canvas.");
                    return;
                }
                session.clear();
                reply(event, "Cleared actions - fresh canvas!");
            } else if ("actions".equals(command)) {
                String message = toString(session.getActions());
                reply(event, message, ParseMode.MARKDOWN);
                return;
            } else if ("file".equals(command)) {
                InputStream inputStream = session.toInputStream();
                SendableDocumentMessage message = SendableDocumentMessage.builder()
                        .document(new InputFile(inputStream, "graphibot_session.png"))
                        .build();
                reply(event, "Here's your image!");
                event.getChat().sendMessage(message);
                return;
            } else {
                return;
            }

            if (session.isEmpty()) {
                reply(event, "You have no actions performed! Use /draw.");
                return;
            }

            InputStream inputStream = session.toInputStream();
            if (inputStream == null)
                return;

            SendablePhotoMessage message = SendablePhotoMessage.builder()
                    .photo(new InputFile(inputStream, "graphibot_session.png"))
                    .build();

            event.getChat().sendMessage(message);
        } catch (NoSizeSet e) {
            reply(event, "You haven't set a size yet.");
        } catch (NoSuchAction e) {
            reply(event, "There is no draw action called '" + e.getAction() + "'.");
        } catch (InvalidActionString e) {
            if (e.getAction() == null)
                reply(event, "That is an invalid string");
            else {
                String s = "Cannot parse '" + e.getActionString() + "' for a " + e.getAction().getName();
                String[] ms = e.getMessages();
                if (ms.length > 0) {
                    s += ":";
                    for (String m : ms) {
                        s += "\n  " + m;
                    }
                }
                reply(event, s);
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

    public void reply(CommandMessageReceivedEvent event, String message) {
        reply(event, message, ParseMode.NONE);
    }

    public void reply(CommandMessageReceivedEvent event, String message, ParseMode parseMode) {
        SendableTextMessage m = SendableTextMessage.builder()
                .message(message)
                .replyTo(event.getMessage())
                .parseMode(parseMode)
                .build();

        event.getChat().sendMessage(m);
    }

    public static String toString(List<GraphiAction> actions) {
        String message = "*Actions performed:*";
        for (GraphiAction action : actions) {
            message += "\n  " + action.toString();
        }
        return message;
    }
}
