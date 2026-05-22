// Stub of javax.security.sasl.SaslClient.
//
// Android omits the javax.security.sasl package entirely. The MongoDB driver's
// SaslAuthenticator$SaslClientImpl implements this interface; without it on the
// classpath the driver class fails to load with NoClassDefFoundError and SCRAM
// auth dies before sending the first challenge. The driver supplies its own
// implementation of the interface — we only need the type to exist so the JVM
// can resolve the class hierarchy.
//
// Signatures match the OpenJDK public API; behaviour is irrelevant because the
// driver never calls into our stubs, only into its own SaslClientImpl.
package javax.security.sasl;

public interface SaslClient {
    String getMechanismName();

    boolean hasInitialResponse();

    byte[] evaluateChallenge(byte[] challenge) throws SaslException;

    boolean isComplete();

    byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException;

    byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException;

    Object getNegotiatedProperty(String propName);

    void dispose() throws SaslException;
}
