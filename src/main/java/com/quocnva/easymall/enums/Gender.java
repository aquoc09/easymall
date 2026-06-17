package com.quocnva.easymall.enums;

/**
 * User gender values — matches the `gender` column in the users table.
 * 0: Nữ (Female), 1: Nam (Male), 2: Khác (Other)
 */
public enum Gender {
    FEMALE(0),
    MALE(1),
    OTHER(2);

    private final short value;

    Gender(int value) {
        this.value = (short) value;
    }

    public short getValue() {
        return value;
    }
}
