package com.prevpaper.comman.exception;

public class WrongCredentialsException extends RuntimeException{
    public WrongCredentialsException(){
        super("Wrong Credentials");
    }
    public WrongCredentialsException(String message){
        super(message);
    }
}
