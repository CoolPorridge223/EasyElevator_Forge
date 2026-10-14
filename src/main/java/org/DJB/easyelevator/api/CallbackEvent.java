package org.DJB.easyelevator.api;
import java.lang.reflect.Array;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
/** 按注册顺序同步派发的线程安全监听器容器；用于原有电梯扩展接口。 */
public final class CallbackEvent<T> {
    private final Class<T> type;private final Function<T[],T> factory;
    private final CopyOnWriteArrayList<T> listeners=new CopyOnWriteArrayList<>();
    private volatile T invoker;
    private CallbackEvent(Class<T> type,Function<T[],T> factory){this.type=type;this.factory=factory;rebuild();}
    public static <T> CallbackEvent<T> createArrayBacked(Class<T> type,Function<T[],T> factory){return new CallbackEvent<>(type,factory);}
    @SuppressWarnings("unchecked") private void rebuild(){invoker=factory.apply(listeners.toArray((T[])Array.newInstance(type,listeners.size())));}
    public synchronized void register(T listener){listeners.add(listener);rebuild();}
    public T invoker(){return invoker;}
}
