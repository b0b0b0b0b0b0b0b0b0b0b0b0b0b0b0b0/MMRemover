package bm.b0b0b0.util.update;

public final class VersionCompare {

    private VersionCompare() {
    }

    public static boolean isNewer(String remote, String current) {
        if (remote == null || remote.isBlank() || current == null || current.isBlank()) {
            return false;
        }
        int[] remoteParts = parse(remote);
        int[] currentParts = parse(current);
        int length = Math.max(remoteParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int remoteValue = i < remoteParts.length ? remoteParts[i] : 0;
            int currentValue = i < currentParts.length ? currentParts[i] : 0;
            if (remoteValue > currentValue) {
                return true;
            }
            if (remoteValue < currentValue) {
                return false;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        String normalized = version.trim();
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        String[] parts = normalized.split("\\.");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String digits = parts[i].replaceAll("[^0-9].*", "");
            if (digits.isEmpty()) {
                values[i] = 0;
            } else {
                try {
                    values[i] = Integer.parseInt(digits);
                } catch (NumberFormatException ignored) {
                    values[i] = 0;
                }
            }
        }
        return values;
    }
}
