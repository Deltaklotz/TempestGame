package dev.corveric.spellObjects;

import java.util.ArrayList;

public class SpellUtil {
    private ArrayList<String> spells = new ArrayList<>();
    public SpellUtil(){
        spells.add("none");
        spells.add("plasmaball");
        //spells.add("fireball");
    }
    public ArrayList<String> getCastable(){
        return spells;
    }
}
