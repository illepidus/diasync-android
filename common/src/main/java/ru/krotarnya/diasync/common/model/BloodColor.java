package ru.krotarnya.diasync.common.model;

import android.graphics.Color;

import lombok.Data;

@Data
public class BloodColor {
    private final Color low;
    private final Color normal;
    private final Color high;
}
