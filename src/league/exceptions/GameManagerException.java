/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.exceptions;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class GameManagerException extends Exception {

    public GameManagerException() {
    }

    public GameManagerException(String message) {
        super(message);
    }

    public GameManagerException(Throwable cause) {
        super(cause);
    }

    public GameManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
