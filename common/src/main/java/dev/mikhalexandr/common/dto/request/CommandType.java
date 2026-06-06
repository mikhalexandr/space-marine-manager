package dev.mikhalexandr.common.dto.request;

import java.io.Serializable;
import java.util.Arrays;

public enum CommandType implements Serializable {
  HELP("help", true, false),
  INFO("info", true, false),
  SHOW("show", true, false),
  ADD("add", true, true),
  UPDATE("update", true, true),
  REMOVE_BY_ID("remove_by_id", true, true),
  CLEAR("clear", true, true),
  EXECUTE_SCRIPT("execute_script", true, false),
  EXIT("exit", false, false),
  HEAD("head", true, false),
  ADD_IF_MIN("add_if_min", true, true),
  SUM_OF_HEALTH("sum_of_health", true, false),
  MAX_BY_CHAPTER("max_by_chapter", true, false),
  COUNT_BY_CATEGORY("count_by_category", true, false),
  HISTORY("history", true, false),
  REGISTER("register", false, false),
  LOGIN("login", false, false),
  UNKNOWN("unknown", false, false);

  private final String wireName;
  private final boolean serverTransmittable;
  private final boolean mutating;

  CommandType(String wireName, boolean serverTransmittable, boolean mutating) {
    this.wireName = wireName;
    this.serverTransmittable = serverTransmittable;
    this.mutating = mutating;
  }

  public String getWireName() {
    return wireName;
  }

  public boolean isServerTransmittable() {
    return serverTransmittable;
  }

  public boolean isMutating() {
    return mutating;
  }

  public static CommandType fromWireName(String commandName) {
    if (commandName == null || commandName.isBlank()) {
      return UNKNOWN;
    }
    String normalized = commandName.trim().toLowerCase();
    return Arrays.stream(values())
        .filter(type -> type.wireName.equals(normalized))
        .findFirst()
        .orElse(UNKNOWN);
  }
}
