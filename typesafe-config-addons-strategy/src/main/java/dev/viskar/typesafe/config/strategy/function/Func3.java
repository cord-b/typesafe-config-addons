package dev.viskar.typesafe.config.strategy.function;

@FunctionalInterface
public interface Func3<P1, P2, P3, V> {

    public V apply(P1 p1, P2 p2, P3 p3) throws Exception;

}
