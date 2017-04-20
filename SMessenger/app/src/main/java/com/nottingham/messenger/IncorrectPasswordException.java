package com.nottingham.messenger;

/**
 * Created by user on 10/03/2017.
 */

public class IncorrectPasswordException extends Throwable {
    public IncorrectPasswordException(Exception e) {
        super(e);
    }
}
