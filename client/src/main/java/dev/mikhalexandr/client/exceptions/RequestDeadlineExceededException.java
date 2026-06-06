package dev.mikhalexandr.client.exceptions;

import java.io.IOException;

public final class RequestDeadlineExceededException extends IOException {
  public RequestDeadlineExceededException(String message) {
    super(message);
  }
}
