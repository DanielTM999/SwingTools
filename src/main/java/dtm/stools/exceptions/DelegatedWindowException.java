package dtm.stools.exceptions;

public class DelegatedWindowException extends RuntimeException{

    public DelegatedWindowException(String msg, Throwable th){
        super(msg, th);
    }

}
