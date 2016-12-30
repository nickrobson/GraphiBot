package xyz.nickr.graphibot.actions;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import xyz.nickr.graphibot.GraphiSession;

/**
 * @author Nick Robson
 */
public abstract class GraphiAction {

    public static final Set<Supplier<GraphiAction>> ACTIONS_SET = new LinkedHashSet<>();
    public static final Map<String, Supplier<GraphiAction>> ACTIONS = new LinkedHashMap<String, Supplier<GraphiAction>>() {
        {
            Arrays.<Supplier<GraphiAction>>asList(
                    ColorAction::new, LineAction::new, RectAction::new, PolygonAction::new, CircleAction::new, TextAction::new, FontAction::new
            ).forEach(a -> {
                ACTIONS_SET.add(a);
                for (String name : a.get().getNames())
                    put(name, a);
            });
        }
    };

    public static GraphiAction create(String name) {
        Supplier<GraphiAction> supplier = ACTIONS.get(name.toLowerCase());
        return supplier != null ? supplier.get() : null;
    }

    private final String desc, names[];

    public GraphiAction(String desc, String... names) {
        this.desc = desc;
        this.names = names;
    }

    public String[] getNames() {
        return names;
    }

    public String getName() {
        return names[0];
    }

    public String getDescription() { return desc; }

    public abstract void interpret(GraphiSession session, String args);

    public abstract void draw(GraphiSession session, BufferedImage image, Graphics2D g);

    public abstract String asString();

    public final String toString() {
        return names[0] + " " + asString();
    }
}
