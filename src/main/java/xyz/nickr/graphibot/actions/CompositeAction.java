package xyz.nickr.graphibot.actions;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import xyz.nickr.graphibot.GraphiSession;

/**
 * Intentionally does no rendering; only for displaying all actions redone after a clear
 *
 * @author Nick Robson
 */
public class CompositeAction extends GraphiAction {

    private List<GraphiAction> actions;

    public CompositeAction(List<GraphiAction> actions) {
        super("", "");
        this.actions = actions;
    }

    @Override
    public void interpret(GraphiSession session, String args) {}

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {}

    @Override
    public String asString() {
        String message = "";
        for (GraphiAction a : actions) {
            message += "\n" + a;
        }
        return message.trim();
    }

}
