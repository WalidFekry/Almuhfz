package com.walid.almuhfz.db.mapper;

import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.learning.models.Ayah;

public class AyahMapper {

    public static AyahEntity toEntity(Ayah ayah, int surahNumber) {
        return new AyahEntity(
                ayah.getNumber() != null ? ayah.getNumber() : 0,
                ayah.getNumberInSurah() != null ? ayah.getNumberInSurah() : 0,
                ayah.getText() != null ? ayah.getText() : "",
                ayah.getAudio() != null ? ayah.getAudio() : "",
                surahNumber
        );
    }

    public static Ayah fromEntity(AyahEntity entity) {
        Ayah ayah = new Ayah();
        ayah.setNumber(entity.getNumber());
        ayah.setNumberInSurah(entity.getNumberInSurah());
        ayah.setText(entity.getText());
        ayah.setAudio(entity.getAudio());
        return ayah;
    }
}
