package dtm.stools.component.panels.editor.word.api;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class WordTask<T> {
    private final CompletableFuture<T> result=new CompletableFuture<>();
    private final AtomicInteger progress=new AtomicInteger();
    private Future<?> worker;
    private boolean committing;
    public CompletionStage<T> completion(){return result.minimalCompletionStage();}
    public int progress(){return progress.get();}
    public synchronized boolean cancel(){if(committing||result.isDone())return false;boolean cancelled=result.cancel(false);if(cancelled&&worker!=null)worker.cancel(true);return cancelled;}
    public boolean isCancelled(){return result.isCancelled();}
    public boolean isDone(){return result.isDone();}
    public synchronized void attach(Future<?> value){worker=value;if(result.isCancelled())worker.cancel(true);}
    public void progress(int value){progress.set(Math.max(0,Math.min(100,value)));}
    public synchronized void complete(T value){if(!result.isDone()){progress.set(100);result.complete(value);}}
    public synchronized void fail(Throwable error){result.completeExceptionally(error);}
    public synchronized boolean beginCommit(){if(result.isDone())return false;committing=true;return true;}
    public synchronized void commit(Callable<T> operation){
        if(result.isDone())return;
        try{complete(operation.call());}catch(Throwable error){fail(error);}
    }
}
