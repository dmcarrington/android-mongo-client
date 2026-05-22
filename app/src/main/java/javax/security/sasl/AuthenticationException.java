package javax.security.sasl;

public class AuthenticationException extends SaslException {
    public AuthenticationException() {
        super();
    }

    public AuthenticationException(String detail) {
        super(detail);
    }

    public AuthenticationException(String detail, Throwable ex) {
        super(detail, ex);
    }
}
