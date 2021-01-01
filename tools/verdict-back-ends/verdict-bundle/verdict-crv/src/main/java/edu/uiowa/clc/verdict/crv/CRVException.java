package edu.uiowa.clc.verdict.crv;

public class CRVException extends Exception {

    /** */
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public CRVException(ErrorCode code) {
        super();
        this.code = code;
    }

    public CRVException(String message, Throwable cause, ErrorCode code) {
        super(message, cause);
        this.code = code;
    }

    public CRVException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    public CRVException(Throwable cause, ErrorCode code) {
        super(cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return this.code;
    }
}
