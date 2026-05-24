package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandExecutor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;


public final class CommandExecutorProxyFactory {
  private CommandExecutorProxyFactory() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static CommandExecutor create(CommandExecutor target, CommandInterceptor... interceptors) {
    InvocationHandler handler = new ChainInvocationHandler(target, List.of(interceptors));
    return (CommandExecutor)
        Proxy.newProxyInstance(
            CommandExecutor.class.getClassLoader(),
            new Class<?>[] {CommandExecutor.class},
            handler);
  }

  private static final class ChainInvocationHandler implements InvocationHandler {
    private final CommandExecutor target;
    private final List<CommandInterceptor> interceptors;

    private ChainInvocationHandler(CommandExecutor target, List<CommandInterceptor> interceptors) {
      this.target = target;
      this.interceptors = interceptors;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (!"execute".equals(method.getName())) {
        return method.invoke(target, args);
      }
      return proceed(0, (CommandRequest) args[0]);
    }

    private CommandResponse proceed(int index, CommandRequest request) {
      if (index == interceptors.size()) {
        return target.execute(request);
      }
      CommandExecutor next = nextRequest -> proceed(index + 1, nextRequest);
      return interceptors.get(index).intercept(request, next);
    }
  }
}
