package com.saltyfish.backend.constant;

public class DatabaseConstant {

    public static final Integer USERNAME_LENGTH = 20;
    public static final Integer ACCOUNT_LENGTH = 20;
    public static final Integer PASSWORD_LENGTH = 20;

    public static enum CHANGE_TYPE {
        OTHER,
        REGISTER,
        CLOCK_IN,
        GAME
    }
}
