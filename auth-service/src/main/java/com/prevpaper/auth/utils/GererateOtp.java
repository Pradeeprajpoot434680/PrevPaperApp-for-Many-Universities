package com.prevpaper.auth.utils;

import java.util.Random;

public class GererateOtp {
    public  static String getOTP(){
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        return otp;
    }
}
