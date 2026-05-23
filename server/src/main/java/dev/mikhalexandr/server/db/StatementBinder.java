package dev.mikhalexandr.server.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class StatementBinder {
  private final PreparedStatement statement;
  private int index;

  StatementBinder(PreparedStatement statement) {
    this.statement = statement;
    this.index = 0;
  }

  StatementBinder string(String value) throws SQLException {
    statement.setString(++index, value);
    return this;
  }

  StatementBinder nullableString(String value) throws SQLException {
    if (value == null) {
      statement.setNull(++index, Types.VARCHAR);
    } else {
      statement.setString(++index, value);
    }
    return this;
  }

  StatementBinder doubleValue(double value) throws SQLException {
    statement.setDouble(++index, value);
    return this;
  }

  StatementBinder longValue(long value) throws SQLException {
    statement.setLong(++index, value);
    return this;
  }

  StatementBinder floatValue(float value) throws SQLException {
    statement.setFloat(++index, value);
    return this;
  }

  void intValue(int value) throws SQLException {
    statement.setInt(++index, value);
  }
}
