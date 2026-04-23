package dev.mikhalexandr.common.dto.request.payload;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import java.util.Optional;
import java.util.function.Supplier;

public final class CommandPayloads {
  private CommandPayloads() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static Optional<IdPayload> findIdPayload(CommandRequest request) {
    return findPayload(request, IdPayload.class);
  }

  public static Optional<RawArgumentsPayload> findRawArgumentsPayload(CommandRequest request) {
    return findPayload(request, RawArgumentsPayload.class);
  }

  public static <X extends Exception> IdPayload requireIdPayload(
      CommandRequest request, Supplier<X> exceptionSupplier) throws X {
    return requirePayload(request, IdPayload.class, exceptionSupplier);
  }

  public static <X extends Exception> IdMarinePayload requireIdMarinePayload(
      CommandRequest request, Supplier<X> exceptionSupplier) throws X {
    return requirePayload(request, IdMarinePayload.class, exceptionSupplier);
  }

  public static <X extends Exception> MarinePayload requireMarinePayload(
      CommandRequest request, Supplier<X> exceptionSupplier) throws X {
    return requirePayload(request, MarinePayload.class, exceptionSupplier);
  }

  public static <X extends Exception> CategoryPayload requireCategoryPayload(
      CommandRequest request, Supplier<X> exceptionSupplier) throws X {
    return requirePayload(request, CategoryPayload.class, exceptionSupplier);
  }

  private static <T extends CommandPayload> Optional<T> findPayload(
      CommandRequest request, Class<T> payloadType) {
    if (request == null || request.getPayload() == null) {
      return Optional.empty();
    }
    CommandPayload payload = request.getPayload();
    if (!payloadType.isInstance(payload)) {
      return Optional.empty();
    }
    return Optional.of(payloadType.cast(payload));
  }

  private static <T extends CommandPayload, X extends Exception> T requirePayload(
      CommandRequest request, Class<T> payloadType, Supplier<X> exceptionSupplier) throws X {
    return findPayload(request, payloadType).orElseThrow(exceptionSupplier);
  }
}
