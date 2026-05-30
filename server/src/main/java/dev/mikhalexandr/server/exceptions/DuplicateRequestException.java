package dev.mikhalexandr.server.exceptions;

public final class DuplicateRequestException extends RuntimeException {
  public DuplicateRequestException(String message) {
    super(message);
  }
}
