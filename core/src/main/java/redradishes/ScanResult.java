package redradishes;

public class ScanResult<T> {
  public final long cursor;
  public final T elements;

  public ScanResult(long cursor, T elements) {
    this.cursor = cursor;
    this.elements = elements;
  }
}
