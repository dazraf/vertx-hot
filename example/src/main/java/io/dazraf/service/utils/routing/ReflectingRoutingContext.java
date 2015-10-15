package io.dazraf.service.utils.routing;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RoutingContextDecorator;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;

class ReflectingRoutingContext extends RoutingContextDecorator {
  private final Object data;
  private final AtomicReference<Map<String, Object>> map = new AtomicReference<>();

  @Override
  public boolean removeBodyEndHandler(int handlerID) {
    return super.removeBodyEndHandler(handlerID);
  }

  public ReflectingRoutingContext(RoutingContext decoratedContext, Object data) {
    super(decoratedContext.currentRoute(), decoratedContext);
    this.data = data;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T get(String key) {
    return (T) data().get(key);
  }

  @Override
  public Map<String, Object> data() {
    return map.updateAndGet(m -> {
      if (m == null) {
        return buildInvocableMap();
      } else {
        return m;
      }
    });
  }

  private Map<String, Object> buildInvocableMap() {
    Map<String, Supplier<Object>> stringCallableMap = buildFnMap();
    return new Map<String, Object>() {
      @Override
      public int size() {
        return stringCallableMap.size();
      }

      @Override
      public boolean isEmpty() {
        return stringCallableMap.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
        return stringCallableMap.containsKey(key);
      }

      @Override
      public boolean containsValue(Object value) {
        return this.values().stream().filter(value::equals).findFirst().isPresent();
      }

      @Override
      public Object get(Object key) {
        return ofNullable(stringCallableMap.get(key)).map(Supplier::get).orElse(null);
      }

      @Override
      public Object put(String key, Object value) {
        throw new UnsupportedOperationException("unsupported");
      }

      @Override
      public Object remove(Object key) {
        throw new UnsupportedOperationException("unsupported");
      }

      @Override
      public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("unsupported");
      }

      @Override
      public void clear() {
        throw new UnsupportedOperationException("unsupported");
      }

      @Override
      public Set<String> keySet() {
        return stringCallableMap.keySet();
      }

      @Override
      public Collection<Object> values() {
        return stringCallableMap.values().stream().map(Supplier::get).collect(Collectors.toList());
      }

      @Override
      public Set<Entry<String, Object>> entrySet() {
        return stringCallableMap.entrySet().stream()
          .map(e -> new Entry<String, Object>() {

            @Override
            public String getKey() {
              return e.getKey();
            }

            @Override
            public Object getValue() {
              return e.getValue().get();
            }

            @Override
            public Object setValue(Object value) {
              throw new UnsupportedOperationException("unsupported");
            }
          }).collect(Collectors.toSet());
      }
    };
  }

  private Map<String, Supplier<Object>> buildFnMap() {
    Method[] gets = getClassGetters(data);
    return of(gets)
      .collect(
        Collectors.toMap(
          this::getPropertyName,
          this::getMethodSupplierFunction));
  }

  private String getPropertyName(Method m) {
    return m.getName().substring(3);
  }

  private Supplier<Object> getMethodSupplierFunction(Method m) {
    return () -> {
      try {
        return m.invoke(data);
      } catch (Exception e) {
        return e.getMessage();
      }
    };
  }

  private static Map<Class<?>, Method[]> cachedClasses = new ConcurrentHashMap<>();

  private static Method[] getClassGetters(Object data) {
    return cachedClasses.computeIfAbsent(data.getClass(),
      aClass -> of(aClass.getDeclaredMethods())
        .filter(filterMethod())
        .toArray(Method[]::new));
  }

  private static Predicate<Method> filterMethod() {
    return m -> {
      String name = m.getName();
      return
        name.startsWith("get") &&
          name.length() > 3 &&
          m.getParameterCount() == 0 &&
          !m.getReturnType().equals(Void.TYPE);
    };
  }
}
