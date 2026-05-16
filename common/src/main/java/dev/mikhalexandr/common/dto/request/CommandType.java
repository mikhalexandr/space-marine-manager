package dev.mikhalexandr.common.dto.request;

import java.io.Serializable;
import java.util.Arrays;

/** Типы команд, доступных в системе. */
public enum CommandType implements Serializable {
  HELP("help", true),
  INFO("info", true),
  SHOW("show", true),
  ADD("add", true),
  UPDATE("update", true),
  REMOVE_BY_ID("remove_by_id", true),
  CLEAR("clear", true),
  EXECUTE_SCRIPT("execute_script", true),
  EXIT("exit", false),
  HEAD("head", true),
  ADD_IF_MIN("add_if_min", true),
  SUM_OF_HEALTH("sum_of_health", true),
  MAX_BY_CHAPTER("max_by_chapter", true),
  COUNT_BY_CATEGORY("count_by_category", true),
  HISTORY("history", true),
  UNKNOWN("unknown", false);

  private final String wireName;
  private final boolean serverTransmittable;

  CommandType(String wireName, boolean serverTransmittable) {
    this.wireName = wireName;
    this.serverTransmittable = serverTransmittable;
  }

  /**
   * @return строковое имя команды в транспортном протоколе
   */
  public String getWireName() {
    return wireName;
  }

  /**
   * @return можно ли отправлять команду на сервер из клиента
   */
  public boolean isServerTransmittable() {
    return serverTransmittable;
  }

  /**
   * @param commandName строковое имя команды
   * @return найденный тип или UNKNOWN
   */
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
