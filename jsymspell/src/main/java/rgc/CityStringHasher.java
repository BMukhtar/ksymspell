package rgc;

import net.openhft.hashing.LongHashFunction;

public class CityStringHasher implements StringHasher {

  private final LongHashFunction cityHash = LongHashFunction.city_1_1();

  @Override
  public long hash(String input) {
    return cityHash.hashChars(input);
  }
}