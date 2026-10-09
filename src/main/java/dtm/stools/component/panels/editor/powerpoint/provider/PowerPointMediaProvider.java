package dtm.stools.component.panels.editor.powerpoint.provider;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Applications can supply licensed codecs without adding them to SwingTools. */
public interface PowerPointMediaProvider extends PowerPointProvider {
    boolean supports(String mimeType);
    Player open(String mimeType, byte[] bytes, Consumer<State> stateListener,
                Consumer<BufferedImage> frameListener) throws IOException;
    enum State { READY, PLAYING, PAUSED, STOPPED, FINISHED, FAILED }
    interface Player extends AutoCloseable {
        void play();
        void pause();
        void seek(long milliseconds);
        void stop();
        @Override void close();
    }
}
