package xyz.nickr.graphibot.actions;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import xyz.nickr.graphibot.GraphiSession;
import xyz.nickr.graphibot.exceptions.InvalidActionString;

/**
 * @author Nick Robson
 */
public class ColorAction extends GraphiAction {

    private String name;
    private Color color;

    public ColorAction() {
        super("r g b (a)", "color", "colour");
    }

    @Override
    public void interpret(GraphiSession session, String args) {
        try {
            Color c = getColor(args, false);
            if (c != null) {
                color = c;
                name = args;
            } else {
                color = getColor(args, true);
                name = null;
            }
        } catch (InvalidActionString e) {
            throw new InvalidActionString(this, e.getActionString(), e.getMessages());
        }
    }

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {
        g.setColor(color);
    }

    @Override
    public String asString() {
        return name != null ? name : String.format("%d %d %d %d", color.getRed(), color.getBlue(), color.getGreen(), color.getAlpha());
    }

    public static Color getColor(String string, boolean checkInts) {
        try {
            Field field = Color.class.getField(string.toUpperCase());
            field.setAccessible(true);
            return (Color) field.get(null);
        } catch (Exception ignored) {
            if (!checkInts)
                return null;
        }
        String[] split = string.split(" ");
        if (split.length > 4)
            throw new InvalidActionString(null, string, "Too many arguments - should be 3 or 4.");
        if (split.length < 3)
            throw new InvalidActionString(null, string, "Not enough arguments - should be 3 or 4.");
        int[] ints = new int[4];
        Arrays.fill(ints, 255);

        int i = 0, j;
        try {
            for (i = 0, j = split.length; i < j; i++) {
                ints[i] = Integer.parseInt(split[i]);
                if (ints[i] < 0 || ints[i] >= 255)
                    throw new InvalidActionString(null, string, "components must be in ");
            }
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(null, string, "Not an integer: '" + split[i] + "'");
        }
        return null;
    }

}
