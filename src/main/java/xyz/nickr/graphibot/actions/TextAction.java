package xyz.nickr.graphibot.actions;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import xyz.nickr.graphibot.GraphiSession;
import xyz.nickr.graphibot.exceptions.InvalidActionString;
import xyz.nickr.graphibot.exceptions.NotInCanvas;

/**
 * @author Nick Robson
 */
public class TextAction extends GraphiAction {

    private int x, y, align;
    private String text;

    public TextAction() {
        super("x y left/center/right text...", "text");
    }

    @Override
    public void interpret(GraphiSession session, String args) {
        String[] split = args.split(" ", 4);
        if (split.length < 4)
            throw new InvalidActionString(this, args, "Not enough arguments - should be 4 or more.");
        try {
            x = Integer.parseInt(split[0]);
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(this, args, "Not an integer: '" + split[0] + "'");
        }
        try {
            y = Integer.parseInt(split[1]);
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(this, args, "Not an integer: '" + split[1] + "'");
        }
        if (x < 0 || x >= session.getWidth())
            throw new NotInCanvas();
        if (y < 0 || y >= session.getHeight())
            throw new NotInCanvas();
        List<String> alignOptions = Arrays.asList("left", "center", "right");
        align = alignOptions.indexOf(split[2]);
        if (align == -1)
            throw new InvalidActionString(this, args, "Alignment must be either left, center, or right.");
        text = split[3];
    }

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {
        if (align == 0) {
            g.drawString(text, x, y);
            return;
        }
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(text);
        if (align == 1) {
            g.drawString(text, x - width / 2, y);
        } else if (align == 2) {
            g.drawString(text, x - width, y);
        }
    }

    @Override
    public String asString() {
        String alignString = (align == 0 ? "left" : (align == 1 ? "center" : "right"));
        return String.format("%d %d %s %s", x, y, alignString, text);
    }

}
