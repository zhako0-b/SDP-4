package airport.facade;
public record Journey(String id, String passenger, int weight, boolean prohibited,
                      boolean passportValid, boolean customsDeclared) {}
