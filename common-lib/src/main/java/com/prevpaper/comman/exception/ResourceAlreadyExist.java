package com.prevpaper.comman.exception;

public class ResourceAlreadyExist extends RuntimeException {
    public  ResourceAlreadyExist(){
        super("Resource already Exist");
    }
    public ResourceAlreadyExist(String message){
        super(message);
    }
}
