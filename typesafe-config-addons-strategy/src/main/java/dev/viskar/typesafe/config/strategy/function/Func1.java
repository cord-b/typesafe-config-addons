package dev.viskar.typesafe.config.strategy.function;

@FunctionalInterface
public interface Func1<P1, V> {

    public V apply(P1 p1) throws Exception;

}
