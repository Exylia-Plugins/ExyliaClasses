package net.exylia.exyliaClasses.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Energy {
    private final String name;
    private final String symbol;
    private final String color;
    private final double max;
    private final double regenPerTick;
}
