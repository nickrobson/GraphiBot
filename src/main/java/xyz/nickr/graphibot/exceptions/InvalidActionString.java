package xyz.nickr.graphibot.exceptions;

import lombok.Getter;
import xyz.nickr.graphibot.actions.GraphiAction;

/**
 * @author Nick Robson
 */
@Getter
public class InvalidActionString extends GraphiException {

    private GraphiAction action;
    private String actionString;
    private String[] messages;

    public InvalidActionString(GraphiAction action, String actionString, String... messages) {
        this.action = action;
        this.actionString = actionString;
        this.messages = messages;
    }

}
