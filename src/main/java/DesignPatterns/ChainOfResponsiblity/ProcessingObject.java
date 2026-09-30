package DesignPatterns.ChainOfResponsiblity;

/**
 * Handler: process a request, then pass the result to the next handler if present.
 */
public abstract class ProcessingObject<T> {
    protected ProcessingObject<T> successor;

    /**
     * Link handlers at runtime to choose this chain's order.
     */
    public void setSuccessor(ProcessingObject<T> successor) {
        this.successor = successor;
    }

    /** Each handler does one piece of work before delegating onward. */
    public T handle(T input) {
        T result = handleWork(input);
        if (successor != null) {
            return successor.handle(result);
        }
        return result;
    }

    /**
     * Concrete handlers implement one transformation in the chain.
     */
    protected abstract T handleWork(T input);
}