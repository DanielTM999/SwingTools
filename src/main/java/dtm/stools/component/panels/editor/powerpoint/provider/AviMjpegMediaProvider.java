package dtm.stools.component.panels.editor.powerpoint.provider;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Minimal RIFF AVI Motion JPEG player; no native library or third-party codec. */
public final class AviMjpegMediaProvider implements PowerPointMediaProvider {
    @Override public String id(){return "powerpoint.media.avi-mjpeg";}
    @Override public boolean supports(String mime){return "video/x-msvideo".equalsIgnoreCase(mime)||"video/avi".equalsIgnoreCase(mime);}
    /** Returns the first JPEG frame for a PPTX preview image. */
    public static BufferedImage firstFrame(byte[] bytes)throws IOException {
        BufferedImage image=ImageIO.read(new ByteArrayInputStream(parse(bytes).frames().getFirst()));
        if(image==null)throw new IOException("Invalid MJPEG frame");
        return image;
    }
    @Override public Player open(String mime,byte[] bytes,Consumer<State> states,Consumer<BufferedImage> frames)throws IOException {
        Avi avi=parse(bytes);states.accept(State.READY);
        return new Player(){
            private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"PowerPointEditor-avi");t.setDaemon(true);return t;});
            private ScheduledFuture<?> running;
            private int frame;
            private boolean closed;
            @Override public synchronized void play(){if(closed||running!=null)return;
                running=timer.scheduleAtFixedRate(()->{
                    int current;
                    synchronized(this){if(frame>=avi.frames.size()){pause();states.accept(State.FINISHED);return;}current=frame++;}
                    try{BufferedImage image=ImageIO.read(new ByteArrayInputStream(avi.frames.get(current)));
                        if(image==null)throw new IOException("Invalid MJPEG frame");SwingUtilities.invokeLater(()->frames.accept(image));
                    }catch(IOException error){pause();states.accept(State.FAILED);}
                },0,avi.frameMillis,TimeUnit.MILLISECONDS);states.accept(State.PLAYING);
            }
            @Override public synchronized void pause(){if(running!=null){running.cancel(false);running=null;}states.accept(State.PAUSED);}
            @Override public synchronized void seek(long ms){frame=(int)Math.min(avi.frames.size()-1,Math.max(0,ms/avi.frameMillis));}
            @Override public synchronized void stop(){pause();frame=0;states.accept(State.STOPPED);}
            @Override public synchronized void close(){if(closed)return;closed=true;if(running!=null)running.cancel(false);timer.shutdownNow();}
        };
    }
    private record Avi(List<byte[]> frames,long frameMillis){}
    private static Avi parse(byte[] bytes)throws IOException {
        if(bytes.length<12||!fourcc(bytes,0).equals("RIFF")||!fourcc(bytes,8).equals("AVI "))throw new IOException("Not an AVI file");
        if(bytes.length>128*1024*1024)throw new IOException("AVI exceeds built-in player limit");
        List<byte[]> frames=new ArrayList<>();long frameMillis=40;
        for(int pos=12;pos+8<=bytes.length;){
            String id=fourcc(bytes,pos);long length=u32(bytes,pos+4);long end=(long)pos+8+length;
            if(end>bytes.length)throw new IOException("Invalid AVI chunk");
            if(id.equals("LIST")&&length>=4){
                String type=fourcc(bytes,pos+8);
                if(type.equals("hdrl"))frameMillis=headerTiming(bytes,pos+12,(int)end,frameMillis);
                if(type.equals("movi"))readFrames(bytes,pos+12,(int)end,frames);
            }
            pos=(int)(end+(length&1));
        }
        if(frames.isEmpty())throw new IOException("No MJPEG frames in AVI");
        return new Avi(List.copyOf(frames),Math.max(10,frameMillis));
    }
    private static long headerTiming(byte[] bytes,int start,int end,long fallback)throws IOException {
        for(int pos=start;pos+8<=end;){long length=u32(bytes,pos+4);long next=(long)pos+8+length;if(next>end)throw new IOException("Invalid AVI header");
            if(fourcc(bytes,pos).equals("avih")&&length>=4){long micros=u32(bytes,pos+8);return Math.max(10,micros/1000);}
            pos=(int)(next+(length&1));}
        return fallback;
    }
    private static void readFrames(byte[] bytes,int start,int end,List<byte[]> frames)throws IOException {
        for(int pos=start;pos+8<=end;){String id=fourcc(bytes,pos);long length=u32(bytes,pos+4),next=(long)pos+8+length;
            if(next>end)throw new IOException("Invalid AVI frame chunk");
            if(id.endsWith("dc")||id.endsWith("db")){
                if(frames.size()>=6000)throw new IOException("Too many AVI frames");
                byte[] frame=Arrays.copyOfRange(bytes,pos+8,(int)next);
                if(frame.length>=2&&(frame[0]&255)==0xFF&&(frame[1]&255)==0xD8)frames.add(frame);
            }
            pos=(int)(next+(length&1));}
    }
    private static String fourcc(byte[] bytes,int pos){return new String(bytes,pos,4,StandardCharsets.US_ASCII);}
    private static long u32(byte[] bytes,int pos){return ByteBuffer.wrap(bytes,pos,4).order(ByteOrder.LITTLE_ENDIAN).getInt()&0xffffffffL;}
}
