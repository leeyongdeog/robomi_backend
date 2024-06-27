package com.robomi.object;

public enum OBJECT_STATUS {
    OK,
    DAMAGE,
    TILT,
    LOST,
    UNKNOWN;

    public long toLong() {
        return (long) this.ordinal();
    }
}
