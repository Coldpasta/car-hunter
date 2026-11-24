package dev.carhunter.imports;
public class UnsupportedDestinationException extends RuntimeException {
 public UnsupportedDestinationException(String countryCode) {
  super("A complete import-cost profile is not available for destination country: " + countryCode);
 }
}
