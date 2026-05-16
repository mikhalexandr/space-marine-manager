package dev.mikhalexandr.server.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ColorMessageConverter extends MessageConverter {
  @Override
  public String convert(ILoggingEvent event) {
    String msg = super.convert(event);
    if (msg == null) {
      return null;
    }

    msg = msg.replaceAll("(?i)\\btrue\\b", "\u001B[32mtrue\u001B[0m");
    msg = msg.replaceAll("(?i)\\bfalse\\b", "\u001B[31mfalse\u001B[0m");

    return msg;
  }
}
