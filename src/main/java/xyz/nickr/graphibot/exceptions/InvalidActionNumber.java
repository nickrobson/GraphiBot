package xyz.nickr.graphibot.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Nick Robson
 */
@AllArgsConstructor
public class InvalidActionNumber extends GraphiException {

    @Getter
    private final int number;

}
