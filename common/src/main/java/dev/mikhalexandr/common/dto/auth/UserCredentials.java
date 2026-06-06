package dev.mikhalexandr.common.dto.auth;

import java.io.Serial;
import java.io.Serializable;

public record UserCredentials(String login, String password) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
