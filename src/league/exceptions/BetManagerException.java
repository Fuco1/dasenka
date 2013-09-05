/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.exceptions;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class BetManagerException extends Exception {

    public BetManagerException() {
    }

    public BetManagerException(String message) {
        super(message);
    }

    public BetManagerException(Throwable cause) {
        super(cause);
    }

    public BetManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
