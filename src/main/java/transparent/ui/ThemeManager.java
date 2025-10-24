package transparent.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import transparent.repository.FileBackedLibraryRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Centralises application theme handling (light/dark).  Themes are applied to
 * scenes and persisted to the file-backed repository so the user's choice is
 * remembered between launches.
 */
public final class ThemeManager {
    public enum Theme {
        LIGHT,
        DARK;

        static Theme fromPreference(String value) {
            if (value == null) {
                return LIGHT;
            }
            try {
                return Theme.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return LIGHT;
            }
        }
    }

    private final FileBackedLibraryRepository repository = FileBackedLibraryRepository.getInstance();
    private final Set<Scene> scenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final List<Consumer<Theme>> listeners = new CopyOnWriteArrayList<>();
    private final String lightStylesheet;
    private final String darkStylesheet;
    private Theme activeTheme;

    private ThemeManager() {
        lightStylesheet = Objects.requireNonNull(getClass().getResource("/css/light-theme.css"))
                .toExternalForm();
        darkStylesheet = Objects.requireNonNull(getClass().getResource("/css/dark-theme.css"))
                .toExternalForm();
        activeTheme = Theme.fromPreference(repository.getTheme());
        repository.addThemeListener(name -> {
            Theme theme = Theme.fromPreference(name);
            if (theme != activeTheme) {
                activeTheme = theme;
                Platform.runLater(() -> {
                    applyThemeToScenes();
                    notifyListeners(theme);
                });
            }
        });
    }

    private static final class Holder {
        private static final ThemeManager INSTANCE = new ThemeManager();
    }

    public static ThemeManager getInstance() {
        return Holder.INSTANCE;
    }

    public Theme getActiveTheme() {
        return activeTheme;
    }

    public void register(Scene scene) {
        if (scene == null) {
            return;
        }
        scenes.add(scene);
        applyTheme(scene, activeTheme);
    }

    public void setTheme(Theme theme) {
        if (theme == null || theme == activeTheme) {
            return;
        }
        activeTheme = theme;
        applyThemeToScenes();
        notifyListeners(theme);
        repository.setTheme(theme.name());
    }

    public void addListener(Consumer<Theme> listener) {
        listeners.add(listener);
        Platform.runLater(() -> listener.accept(activeTheme));
    }

    private void applyThemeToScenes() {
        for (Scene scene : scenes) {
            applyTheme(scene, activeTheme);
        }
    }

    private void applyTheme(Scene scene, Theme theme) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().removeIf(sheet -> sheet.equals(lightStylesheet) || sheet.equals(darkStylesheet));
        scene.getStylesheets().add(theme == Theme.DARK ? darkStylesheet : lightStylesheet);
    }

    private void notifyListeners(Theme theme) {
        for (Consumer<Theme> listener : listeners) {
            listener.accept(theme);
        }
    }
}
