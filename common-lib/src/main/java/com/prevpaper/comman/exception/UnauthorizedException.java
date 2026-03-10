package com.prevpaper.comman.exception;

public class UnauthorizedException extends RuntimeException{
    public  UnauthorizedException(){
        super("Unauthorized User");
    }
    public UnauthorizedException(String message) {
        super(message);
    }
}
