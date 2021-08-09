package dev.viskar.typesafe.config.strategy.function;

@FunctionalInterface
public interface Func2<P1, P2, V> {

    public V apply(P1 p1, P2 p2) throws Exception;

}