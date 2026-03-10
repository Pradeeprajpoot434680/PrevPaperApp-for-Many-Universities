package com.prevpaper.comman.exception;

public class InvalidOtpException extends RuntimeException{
    public  InvalidOtpException(){
        super("Invalid OTP");
    }

    public  InvalidOtpException(String message){
        super(message);
    }
}
