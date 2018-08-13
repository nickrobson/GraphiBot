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
public class PolygonAction extends GraphiAction {

    private int[] x, y;
    private boolean outline;

    public PolygonAction() {
        super("x1 y1 x2 y2 ... outline/fill", "polygon");
    }

    @Override
    public void interpret(GraphiSession session, String args) {
        String[] split = args.split(" ");
        if (split.length < 7)
            throw new InvalidActionString(this, args, "Not enough arguments - should be at least 7.");
        if ((split.length & 1) == 0)
            throw new InvalidActionString(this, args, "There needs to be an even number of points, followed by 'outline' or 'fill'.");
        if (!Arrays.asList("outline", "fill").contains(split[split.length - 1].toLowerCase()))
            throw new InvalidActionString(this, args, "Last argument must be either outline or fill.");

        int numPoints = split.length / 2;
        this.x = new int[numPoints];
        this.y = new int[numPoints];

        int i = 0, j;
        int p = 0;
        boolean x = true;
        try {
            for (i = 0, j = split.length - 1; i < j; i++) {
                int a = Integer.parseInt(split[i]);
                if (a < 0)
                    throw new NotInCanvas();
                if (a >= (x ? session.getWidth() : session.getHeight()))
                    throw new NotInCanvas();
                (x ? this.x : this.y)[p] = a;
                if (x = !x)
                    p++;
            }
        } catch (NumberFormatException ex) {
            throw new InvalidActionString(this, args, "Not an integer: '" + split[i] + "'");
        }
        outline = split[split.length - 1].equalsIgnoreCase("outline");
    }

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {
        if (outline) {
            g.drawPolygon(x, y, x.length);
        } else {
            g.fillPolygon(x, y, x.length);
        }
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, j = x.length; i < j; i++)
            sb.append(x[i]).append(' ').append(y[i]).append(' ');
        return String.format("%s%s", sb, outline ? "outline" : "fill");
    }

}
