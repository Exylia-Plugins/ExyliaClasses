package net.exylia.exyliaClasses.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Warmup {
    private final int time;
    private final String tickSound;
    private final String tickParticle;
    private final String completedSound;
    private final String completedParticle;
    private final String cancelSound;
    private final String cancelParticle;
}
