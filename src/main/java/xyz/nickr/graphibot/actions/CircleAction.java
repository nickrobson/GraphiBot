package xyz.nickr.graphibot.actions;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import xyz.nickr.graphibot.GraphiSession;
import xyz.nickr.graphibot.exceptions.InvalidActionString;
import xyz.nickr.graphibot.exceptions.NotInCanvas;

/**
 * @author Nick Robson
 */
public class CircleAction extends GraphiAction {

    private int x, y, radius;
    private boolean outline;

    public CircleAction() {
        super("x y radius outline/fill", "circle");
    }

    @Override
    public void interpret(GraphiSession session, String args) {
        String[] split = args.split(" ");
        if (split.length > 4)
            throw new InvalidActionString(this, args, "Too many arguments - should be 4.");
        if (split.length < 4)
            throw new InvalidActionString(this, args, "Not enough arguments - should be 4.");
        if (!Arrays.asList("outline", "fill").contains(split[3].toLowerCase()))
            throw new InvalidActionString(this, args, "Last argument must be either outline or fill.");

        int[] ints = new int[2];
        int i = 0;
        try {
            for (i = 0; i < 2; i++) {
                ints[i] = Integer.parseInt(split[i]);
                if (ints[i] < 0)
                    throw new NotInCanvas();
            }
            if (ints[0] >= session.getWidth() || ints[1] >= session.getHeight())
                throw new NotInCanvas();
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(this, args, "Not an integer: '" + split[i] + "'");
        }

        x = ints[0];
        y = ints[1];

        try {
            radius = Integer.parseInt(split[2]);
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(this, args, "Not an integer: '" + split[2] + "'");
        }

        outline = split[3].equalsIgnoreCase("outline");

        if (x - radius < 0 || x + radius >= session.getWidth())
            throw new NotInCanvas();
        if (y - radius < 0 || y + radius >= session.getHeight())
            throw new NotInCanvas();
    }

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {
        int diameter = radius * 2;
        int loX = x - radius;
        int loY = y - radius;
        if (outline) {
            g.drawOval(loX, loY, diameter, diameter);
        } else {
            g.fillOval(loX, loY, diameter, diameter);
        }
    }

    @Override
    public String asString() {
        return String.format("%d %d %d %s", x, y, radius, outline ? "outline" : "fill");
    }
}
