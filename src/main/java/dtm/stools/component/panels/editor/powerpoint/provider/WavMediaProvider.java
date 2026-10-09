package dtm.stools.component.panels.editor.powerpoint.provider;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import java.util.function.Consumer;

/** JDK sampled-audio implementation for PCM WAVE. */
public final class WavMediaProvider implements PowerPointMediaProvider {
    @Override public String id(){return "powerpoint.media.wav";}
    @Override public boolean supports(String mime){return mime!=null&&switch(mime.toLowerCase(Locale.ROOT)){case "audio/wav","audio/x-wav","audio/wave","audio/vnd.wave"->true;default->false;};}
    @Override public Player open(String mime,byte[] bytes,Consumer<State> states,Consumer<BufferedImage> frames)throws IOException {
        try {
            AudioInputStream input=AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
            AudioFormat format=input.getFormat();
            if(!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())&&!AudioFormat.Encoding.PCM_UNSIGNED.equals(format.getEncoding()))
                throw new IOException("Only PCM WAV is supported by the built-in player");
            Clip clip=AudioSystem.getClip();clip.open(input);input.close();states.accept(State.READY);
            return new Player(){
                @Override public void play(){clip.start();states.accept(State.PLAYING);}
                @Override public void pause(){clip.stop();states.accept(State.PAUSED);}
                @Override public void seek(long ms){clip.setMicrosecondPosition(Math.min(Math.max(0,ms*1000),clip.getMicrosecondLength()));}
                @Override public void stop(){clip.stop();clip.setFramePosition(0);states.accept(State.STOPPED);}
                @Override public void close(){clip.close();}
            };
        }catch(UnsupportedAudioFileException|LineUnavailableException error){throw new IOException("Cannot play PCM WAV",error);}
    }
}
