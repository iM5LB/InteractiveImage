package im5lb.interactiveimage.editor;

public record EditorSession(
        boolean enabled,
        String providerId,
        String mapName,
        EditorInputType awaitingInput
) {
    public static EditorSession disabled() {
        return new EditorSession(false, null, null, null);
    }

    public EditorSession withEnabled(boolean enabled) {
        return new EditorSession(enabled, providerId, mapName, awaitingInput);
    }

    public EditorSession withTarget(String providerId, String mapName) {
        return new EditorSession(enabled, providerId, mapName, awaitingInput);
    }

    public EditorSession awaiting(EditorInputType type) {
        return new EditorSession(enabled, providerId, mapName, type);
    }

    public EditorSession clearAwaiting() {
        return new EditorSession(enabled, providerId, mapName, null);
    }
}
