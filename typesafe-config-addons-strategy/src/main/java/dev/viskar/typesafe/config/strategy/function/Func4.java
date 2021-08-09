package dev.viskar.typesafe.config.strategy.function;

@FunctionalInterface
public interface Func4<P1, P2, P3, P4, V> {

    V apply(P1 p1, P2 p2, P3 p3, P4 p4) throws Exception;

}
