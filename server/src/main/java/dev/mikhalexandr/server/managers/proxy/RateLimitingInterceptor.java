package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RateLimitingInterceptor implements CommandInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingInterceptor.class);
  private static final String ANONYMOUS = "strannij_chel";

  private final RateLimiter rateLimiter;

  public RateLimitingInterceptor(RateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  public CommandResponse intercept(CommandRequest request, CommandExecutor next) {
    String userId = resolveUserId(request);
    if (!rateLimiter.allow(userId)) {
      LOGGER.warn("Превышен лимит запросов для '{}'", userId);
      return CommandResponse.error("Слишком много запросов, чилани и повтори позже");
    }
    return next.execute(request);
  }

  private static String resolveUserId(CommandRequest request) {
    UserCredentials credentials = request.getCredentials();
    if (credentials == null || credentials.login() == null || credentials.login().isBlank()) {
      return ANONYMOUS;
    }
    return credentials.login().trim();
  }
}
