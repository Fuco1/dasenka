package league.exceptions;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class UserManagerException extends Exception {

    public UserManagerException() {
    }

    public UserManagerException(String message) {
        super(message);
    }

    public UserManagerException(Throwable cause) {
        super(cause);
    }

    public UserManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
