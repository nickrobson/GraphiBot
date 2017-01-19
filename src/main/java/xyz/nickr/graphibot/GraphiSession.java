package xyz.nickr.graphibot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import xyz.nickr.graphibot.actions.CompositeAction;
import xyz.nickr.graphibot.actions.GraphiAction;
import xyz.nickr.graphibot.exceptions.InvalidActionNumber;
import xyz.nickr.graphibot.exceptions.InvalidActionString;
import xyz.nickr.graphibot.exceptions.NoSizeSet;
import xyz.nickr.graphibot.exceptions.NoSuchAction;
import xyz.nickr.graphibot.exceptions.NothingToRedo;
import xyz.nickr.graphibot.exceptions.NothingToUndo;

/**
 * @author Nick Robson
 */
@RequiredArgsConstructor
public class GraphiSession {

    public static final int MIN_SIZE = 32, MAX_SIZE = 2048;

    private final long userId;
    private final List<GraphiAction> actions = new ArrayList<>();
    private int actionCount = 0, preClearCount;
    private boolean wasCleared = false;

    private Integer size;

    @Getter
    private Color background = Color.BLACK;
    @Getter @Setter
    private String backgroundString = "black";

    public void accept(String string) {
        if (size == null)
            throw new NoSizeSet();
        String[] split = string.trim().split(" ", 2);
        if (split.length == 1) {
            GraphiAction action = GraphiAction.create(split[0]);
            if (action == null)
                throw new NoSuchAction(split[0]);
            else
                throw new InvalidActionString(action, string, "Missing arguments!");
        }
        String actionType = split[0];
        String actionArguments = split[1];
        GraphiAction action = GraphiAction.create(actionType);
        if (action == null)
            throw new NoSuchAction(actionType);
        action.interpret(this, actionArguments);
        if (actions.size() > actionCount)
            actions.subList(actionCount, actions.size()).clear();
        actions.add(action);
        actionCount++;
        wasCleared = false;
    }

    public GraphiAction undo() {
        if (size == null)
            throw new NoSizeSet();
        if (wasCleared) {
            actionCount = preClearCount;
            wasCleared = false;
            return new CompositeAction(actions.subList(0, actionCount));
        }
        if (actionCount == 0)
            throw new NothingToUndo();
        return actions.get(--actionCount);
    }

    public GraphiAction redo() {
        if (size == null)
            throw new NoSizeSet();
        wasCleared = false;
        if (actionCount == actions.size())
            throw new NothingToRedo();
        return actions.get(actionCount++);
    }

    public GraphiAction remove(int n) {
        if (size == null)
            throw new NoSizeSet();
        try {
            GraphiAction action = actions.remove(n);
            wasCleared = false;
            --actionCount;
            return action;
        } catch (Exception ex) {
            throw new InvalidActionNumber(n);
        }
    }

    public void clear() {
        preClearCount = actionCount;
        actionCount = 0;
        wasCleared = true;
    }

    public boolean isEmpty() {
        return actionCount == 0;
    }

    public int setSize(int size) {
        return this.size = Math.max(MIN_SIZE, Math.min(size, MAX_SIZE));
    }

    public void setBackground(Color c) {
        if (size == null)
            throw new NoSizeSet();
        background = c;
    }

    public BufferedImage build() {
        if (size == null)
            throw new NoSizeSet();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (background != null) {
            g.setColor(background);
            g.fillRect(0, 0, size, size);
        }
        g.setColor(Color.WHITE);
        for (int i = 0; i < actionCount; i++) {
            GraphiAction action = actions.get(i);
            action.draw(this, image, g);
        }
        return image;
    }

    public InputStream toInputStream() {
        try {
            BufferedImage image = build();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public List<GraphiAction> getActions() {
        if (size == null)
            throw new NoSizeSet();
        return Collections.unmodifiableList(actions.subList(0, actionCount));
    }

    public int getSize() {
        return size != null ? size : -1;
    }

    public String getExpiryMessage() {
        if (size == null)
            throw new NoSizeSet();
        String message = "";
        message += "Size: " + size;
        message += "\nBackground: " + backgroundString;
        if (actionCount > 0)
            message += "\n" + GraphiBotListener.toString(getActions());
        return message;
    }
}
