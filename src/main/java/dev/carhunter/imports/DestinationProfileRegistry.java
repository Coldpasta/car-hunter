package dev.carhunter.imports;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Component
public class DestinationProfileRegistry {
 private final Map<String, DestinationProfile> profiles;
 public DestinationProfileRegistry(List<DestinationProfile> profiles) {
  this.profiles = profiles.stream().collect(Collectors.toUnmodifiableMap(
   profile -> profile.countryCode().toUpperCase(), Function.identity()));
 }
 public boolean isSupported(String countryCode) { return profiles.containsKey(countryCode.toUpperCase()); }
 public DestinationProfile getRequired(String countryCode) {
  DestinationProfile profile = profiles.get(countryCode.toUpperCase());
  if (profile == null) throw new UnsupportedDestinationException(countryCode);
  return profile;
 }
}
