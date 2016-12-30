package xyz.nickr.graphibot.actions;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import xyz.nickr.graphibot.GraphiSession;
import xyz.nickr.graphibot.exceptions.InvalidActionString;
import xyz.nickr.graphibot.exceptions.NotInCanvas;

/**
 * @author Nick Robson
 */
public class RectAction extends GraphiAction {

    private int x, y, width, height;
    private boolean outline;

    public RectAction() {
        super("x y width height outline/fill", "rect");
    }

    @Override
    public void interpret(GraphiSession session, String args) {
        String[] split = args.split(" ");
        if (split.length > 5)
            throw new InvalidActionString(this, args, "Too many arguments - should be 5.");
        if (split.length < 5)
            throw new InvalidActionString(this, args, "Not enough arguments - should be 5.");
        if (!Arrays.asList("outline", "fill").contains(split[4].toLowerCase()))
            throw new InvalidActionString(this, args, "Last argument must be either outline or fill.");
        int[] ints = new int[4];
        int i = 0;
        try {
            for (i = 0; i < 4; i++) {
                ints[i] = Integer.parseInt(split[i]);
            }
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(this, args, "Not an integer: '" + split[i] + "'");
        }
        x = ints[0];
        y = ints[1];
        width = ints[2];
        height = ints[3];
        if (x < 0 || x + width >= session.getSize())
            throw new NotInCanvas();
        if (y < 0 || y + height >= session.getSize())
            throw new NotInCanvas();
        outline = split[4].equalsIgnoreCase("outline");
    }

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {
        if (outline) {
            g.drawRect(x, y, width, height);
        } else {
            g.fillRect(x, y, width, height);
        }
    }

    @Override
    public String asString() {
        return String.format("%d %d %d %d %s", x, y, width, height, outline ? "outline" : "fill");
    }

}
