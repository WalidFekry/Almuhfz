package com.walid.almuhfz.db.mapper;

import com.walid.almuhfz.db.entity.ReciterEntity;
import com.walid.almuhfz.learning.models.Reciter;

public class ReciterMapper {
    public static Reciter fromEntity(ReciterEntity entity) {
        Reciter reciter = new Reciter();
        reciter.setName(entity.getName());
        reciter.setIdentifier(entity.getId());
        return reciter;
    }
}
