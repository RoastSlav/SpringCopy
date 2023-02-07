package Spring.Exceptions;

public class NoUniqueBeanDefinitionException extends Exception {
    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }

    public NoUniqueBeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoUniqueBeanDefinitionException(Throwable cause) {
        super(cause);
    }
}
