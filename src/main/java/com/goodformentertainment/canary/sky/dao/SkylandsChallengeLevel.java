package com.goodformentertainment.canary.sky.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.canarymod.database.Column;
import net.canarymod.database.Column.ColumnType;
import net.canarymod.database.Column.DataType;
import net.canarymod.database.DataAccess;
import net.canarymod.database.Database;
import net.canarymod.database.exceptions.DatabaseReadException;
import net.canarymod.database.exceptions.DatabaseWriteException;

public class SkylandsChallengeLevel extends DataAccess {
    public static final String NAME = "name";
    public static final String ORDER = "order";

    public static List<SkylandsChallengeLevel> getAllXChallengeLevels()
            throws DatabaseReadException {
        final List<SkylandsChallengeLevel> levels = new ArrayList<SkylandsChallengeLevel>();

        final List<DataAccess> daos = new ArrayList<DataAccess>();
        final SkylandsChallengeLevel xChallengeLevel = new SkylandsChallengeLevel();
        Database.get().loadAll(xChallengeLevel, daos, new HashMap<String, Object>());
        for (final DataAccess dao : daos) {
            levels.add((SkylandsChallengeLevel) dao);
        }

        Collections.sort(levels, new Comparator<SkylandsChallengeLevel>() {
            @Override
            public int compare(final SkylandsChallengeLevel x1, final SkylandsChallengeLevel x2) {
                return new Integer(x1.order).compareTo(x2.order);
            }
        });

        return levels;
    }

    public SkylandsChallengeLevel() {
        super("skylands_xchallengelevel");
    }

    @Override
    public SkylandsChallengeLevel getInstance() {
        return new SkylandsChallengeLevel();
    }

    @Column(columnName = NAME, dataType = DataType.STRING, columnType = ColumnType.UNIQUE)
    public String name;

    @Column(columnName = ORDER, dataType = DataType.INTEGER, columnType = ColumnType.UNIQUE, autoIncrement = true)
    public int order;

    public void update() throws DatabaseWriteException {
        final Map<String, Object> filters = new HashMap<String, Object>();
        filters.put(SkylandsChallengeLevel.NAME, name);
        Database.get().update(this, filters);
    }
}
