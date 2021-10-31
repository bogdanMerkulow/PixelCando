package pixel.cando.utils.diffuser;

public interface DidChange<A> {
    boolean test(A oldValue, A newValue);
}
