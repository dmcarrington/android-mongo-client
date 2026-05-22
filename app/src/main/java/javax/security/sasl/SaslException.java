package javax.security.sasl;

public class SaslException extends java.io.IOException {
    private Throwable _cause;

    public SaslException() {
        super();
    }

    public SaslException(String detail) {
        super(detail);
    }

    public SaslException(String detail, Throwable ex) {
        super(detail);
        this._cause = ex;
    }

    @Override
    public synchronized Throwable getCause() {
        return _cause;
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        this._cause = cause;
        return this;
    }

    @Override
    public String toString() {
        String msg = super.toString();
        return _cause == null ? msg : msg + ", caused by " + _cause;
    }
}
