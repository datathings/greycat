package greycat.mqtt.exceptions;

/**
 * Created by Cyril Cecchinel - I3S Laboratory on 04/08/2017.
 */
public class BadPayloadException extends Exception {
    public BadPayloadException(String reason){
        super(reason);
    }
}
