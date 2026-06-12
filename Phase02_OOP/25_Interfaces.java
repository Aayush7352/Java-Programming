package phase02.oop;

// Interface with abstract, default, static, and private methods
interface MediaPlayer {
    // Abstract methods (implicitly public)
    void play();
    void pause();
    void stop();

    // Default method (Java 8)
    default void next() {
        System.out.println("Skipping to next track");
    }

    default void previous() {
        System.out.println("Going back to previous track");
    }

    // Static method (Java 8)
    static String getMediaType() {
        return "Audio/Video";
    }

    // Private method (Java 9) - helper for default methods
    private void log(String action) {
        System.out.println("[MediaPlayer] " + action);
    }

    // Private static method (Java 9)
    private static void checkLicense() {
        System.out.println("[License] Valid license detected");
    }

    // Default method using private method
    default void playWithLog() {
        log("play() called");
        checkLicense();
        play();
    }
}

// Another interface for multiple inheritance demonstration
interface VolumeControl {
    void increaseVolume();
    void decreaseVolume();

    default void mute() {
        System.out.println("Muted");
    }
}

// Implementing class
class MusicPlayer implements MediaPlayer, VolumeControl {
    private String track;

    public MusicPlayer(String track) {
        this.track = track;
    }

    @Override
    public void play() {
        System.out.println("Playing: " + track);
    }

    @Override
    public void pause() {
        System.out.println("Paused: " + track);
    }

    @Override
    public void stop() {
        System.out.println("Stopped: " + track);
    }

    @Override
    public void increaseVolume() {
        System.out.println("Volume increased");
    }

    @Override
    public void decreaseVolume() {
        System.out.println("Volume decreased");
    }

    @Override
    public void next() {
        System.out.println("Next track after: " + track);
    }
}

// Marker interface (no methods)
interface Playable {}

class Podcast implements Playable {
    private String title;

    public Podcast(String title) {
        this.title = title;
    }

    public String getTitle() { return title; }
}

// Functional interface (for lambda)
@FunctionalInterface
interface PlayAction {
    void execute(String content);
}

class Interfaces {
    public static void main(String[] args) {
        System.out.println("=== Interface Implementation ===");
        MusicPlayer player = new MusicPlayer("Bohemian Rhapsody");
        player.play();
        player.pause();
        player.stop();
        player.next();
        player.previous();
        player.increaseVolume();
        player.decreaseVolume();
        player.mute();

        // Default method
        System.out.println("\n=== Default Method ===");
        player.playWithLog();

        // Static method
        System.out.println("\n=== Static Interface Method ===");
        System.out.println("Media type: " + MediaPlayer.getMediaType());

        // Marker interface
        System.out.println("\n=== Marker Interface ===");
        Podcast podcast = new Podcast("Tech Talk");
        if (podcast instanceof Playable) {
            System.out.println(podcast.getTitle() + " is playable");
        }

        // Functional interface with lambda
        System.out.println("\n=== Functional Interface with Lambda ===");
        PlayAction action = content -> System.out.println("Playing: " + content);
        action.execute("Lambda Demo Track");

        // Multiple inheritance through interfaces
        System.out.println("\n=== Multiple Interface Inheritance ===");
        MediaPlayer player2 = new MusicPlayer("Stairway to Heaven");
        VolumeControl volume = (VolumeControl) player2;
        player2.play();
        volume.increaseVolume();
    }
}
