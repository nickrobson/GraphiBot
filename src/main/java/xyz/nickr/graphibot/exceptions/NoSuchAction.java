package xyz.nickr.graphibot.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Nick Robson
 */
@AllArgsConstructor
public class NoSuchAction extends RuntimeException {

    @Getter
    private final String action;

}
