package com.nottingham.messenger.security;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by user on 11/03/2017.
 */

public class SettingsManager {
    public static volatile char[] enteredPassword = null;

    public static int numberOfDaysAesKeyIsValidFor = 3;

    public static void removePasswordFromMemory() {
        Arrays.fill(enteredPassword, ' ');
    }
}
