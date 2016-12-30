package xyz.nickr.graphibot.actions;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import xyz.nickr.graphibot.GraphiSession;

/**
 * @author Nick Robson
 */
public class FontAction extends GraphiAction {

    private String string;

    public FontAction() {
        super("name style size", "font");
    }

    @Override
    public void interpret(GraphiSession session, String args) {
        string = args;
    }

    @Override
    public void draw(GraphiSession session, BufferedImage image, Graphics2D g) {
        Font font = Font.decode(string);
        g.setFont(font);
    }

    @Override
    public String asString() {
        return String.format("%s", string);
    }
}
