package net.exylia.exyliaClasses.model.specialpassive;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SpecialPassive {
    private final SpecialPassiveType type;
    private final String condition;
    private final List<String> effects;

    public boolean check(Player player) {
        return type.check(player, condition);
    }
}
