package xyz.nickr.graphibot.actions;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import xyz.nickr.graphibot.GraphiSession;
import xyz.nickr.graphibot.exceptions.InvalidActionString;
import xyz.nickr.graphibot.exceptions.NotInCanvas;

/**
 * @author Nick Robson
 */
public class LineAction extends GraphiAction {

    private int x1, y1, x2, y2;

    public LineAction() {
        super("x1 y1 x2 y2", "line");
    }

    @Override
    public void interpret(GraphiSession session, String args) {
        String[] split = args.split(" ");
        if (split.length > 4)
            throw new InvalidActionString(this, args, "Too many arguments - should be 4.");
        if (split.length < 4)
            throw new InvalidActionString(this, args, "Not enough arguments - should be 4.");
        int[] ints = new int[4];
        int i = 0;
        try {
            for (i = 0; i < 4; i++) {
                ints[i] = Integer.parseInt(split[i]);
                if (ints[i] < 0 || ints[i] >= session.getSize())
                    throw new NotInCanvas();
            }
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(this, args, "Not an integer: '" + split[i] + "'");
        }
        x1 = ints[0];
        y1 = ints[1];
        x2 = ints[2];
        y2 = ints[3];
    }

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {
        g.drawLine(x1, y1, x2, y2);
    }

    @Override
    public String asString() {
        return String.format("%d %d %d %d", x1, y1, x2, y2);
    }

}
