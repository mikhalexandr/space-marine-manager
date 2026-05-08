package dev.mikhalexandr.common.util;

/** Утилитка для работы с массивами байт */
public final class Bytes {

  private Bytes() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * Соединяет два массива в один
   *
   * @param left первый массив
   * @param right второй массив
   * @return новый массив длины {@code left.length + right.length}
   */
  public static byte[] concat(byte[] left, byte[] right) {
    byte[] out = new byte[left.length + right.length];
    System.arraycopy(left, 0, out, 0, left.length);
    System.arraycopy(right, 0, out, left.length, right.length);
    return out;
  }
}
