package pixel.cando.utils.diffuser;

@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}
