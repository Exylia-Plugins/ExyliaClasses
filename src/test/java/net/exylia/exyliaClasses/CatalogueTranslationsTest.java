package net.exylia.exyliaClasses;

import net.exylia.lib.config.CatalogueCheck;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every language carries every word of the catalogue, with its tokens and at the card's width. */
class CatalogueTranslationsTest {

    private static final List<String> LANGUAGES = List.of("es", "fr", "pt", "ru", "tr");
    private static final Pattern TEXT = Pattern.compile("display-name$|^energy\\.name$");

    @Test
    void everyLanguageCarriesEveryWord() {
        List<String> problems = new ArrayList<>();
        for (String resource : List.of("classes/Archer.yml", "classes/Bard.yml", "classes/Miner.yml", "classes/Rogue.yml", "classes/Sorcerer.yml", "classes/Tank.yml")) {
            problems.addAll(CatalogueCheck.problems(ExyliaClasses.class, resource, LANGUAGES, TEXT, 34));
        }
        assertEquals(List.of(), problems);
    }
}
