package net.livaddons.feature;

public final class FeatureSettings {
    private static boolean terminalWaypoints;
    private static boolean terminalSolver;
    private static boolean deviceSolver;
    private static boolean melodyAlert;
    private static String melodyAlertMessage = "[LivAddons] Melody terminal started";

    private FeatureSettings() {
    }

    public static boolean terminalWaypointsEnabled() {
        return terminalWaypoints;
    }

    public static void setTerminalWaypointsEnabled(boolean enabled) {
        terminalWaypoints = enabled;
    }

    public static boolean terminalSolverEnabled() {
        return terminalSolver;
    }

    public static void setTerminalSolverEnabled(boolean enabled) {
        terminalSolver = enabled;
    }

    public static boolean deviceSolverEnabled() {
        return deviceSolver;
    }

    public static void setDeviceSolverEnabled(boolean enabled) {
        deviceSolver = enabled;
    }

    public static boolean melodyAlertEnabled() {
        return melodyAlert;
    }

    public static void setMelodyAlertEnabled(boolean enabled) {
        melodyAlert = enabled;
    }

    public static String melodyAlertMessage() {
        return melodyAlertMessage;
    }

    public static void setMelodyAlertMessage(String message) {
        melodyAlertMessage = message == null || message.isBlank()
                ? "[LivAddons] Melody terminal started" : message;
    }
}
