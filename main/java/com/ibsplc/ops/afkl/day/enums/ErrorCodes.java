package com.ibsplc.ops.afkl.day.enums;

import lombok.Getter;

@Getter
public enum ErrorCodes {

    E0001(" Received message is neither AF nor KL. Cannot process the message further."),
    E0002("  XSD validation error in the input message. Message will not be processed further."),
    E0003(" No body found in the soap message");


    private final String description;

    ErrorCodes(String description) {
        this.description = description;
    }

}