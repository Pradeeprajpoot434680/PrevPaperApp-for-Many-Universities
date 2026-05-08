package com.prevpaper.comman.exception;

public class ContentAlreadyExist extends RuntimeException{
    public ContentAlreadyExist(){
        super("Content Already Exist");
    }
    public ContentAlreadyExist(String message){
        super(message);
    }
}
