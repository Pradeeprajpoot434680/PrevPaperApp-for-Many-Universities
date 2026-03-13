package com.prevpaper.comman.exception;

public class EmptyInputBoxException extends RuntimeException {
    public  EmptyInputBoxException(){
        super("Fields Are Required");
    }

    public EmptyInputBoxException(String message){
        super(message);
    }
}
