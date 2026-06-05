package dev.mikhalexandr.client.gui;

import dev.mikhalexandr.common.models.Chapter;
import dev.mikhalexandr.common.models.SpaceMarine;
import java.util.List;
import java.util.function.Function;
import programming.lab8.gui.api.FieldDescriptor;
import programming.lab8.gui.api.FieldType;
import programming.lab8.gui.api.ObjectDescriptor;

/** Описывает SpaceMarine для гуи */
public final class SpaceMarineDescriptor implements ObjectDescriptor<SpaceMarine> {
  private static final double MIN_RADIUS = 8.0;
  private static final double MAX_RADIUS = 60.0;
  private static final double HEALTH_SCALE = 5.0;

  @Override
  public long id(SpaceMarine object) {
    return object.getId() == null ? 0L : object.getId();
  }

  @Override
  public String owner(SpaceMarine object) {
    return object.getOwner();
  }

  @Override
  public double x(SpaceMarine object) {
    return object.getCoordinates().getX();
  }

  @Override
  public double y(SpaceMarine object) {
    return object.getCoordinates().getY();
  }

  @Override
  public double radius(SpaceMarine object) {
    return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, object.getHealth() / HEALTH_SCALE));
  }

  @Override
  public List<FieldDescriptor<SpaceMarine>> fields() {
    return List.of(
        field("id", "field.id", FieldType.INTEGER, SpaceMarine::getId),
        field("name", "field.name", FieldType.TEXT, SpaceMarine::getName),
        field("x", "field.x", FieldType.DECIMAL, sm -> sm.getCoordinates().getX()),
        field("y", "field.y", FieldType.INTEGER, sm -> sm.getCoordinates().getY()),
        field(
            "creationDate",
            "field.creationDate",
            FieldType.DATE_TIME,
            SpaceMarine::getCreationDate),
        field("health", "field.health", FieldType.DECIMAL, SpaceMarine::getHealth),
        field("height", "field.height", FieldType.INTEGER, SpaceMarine::getHeight),
        field("category", "field.category", FieldType.ENUM, SpaceMarine::getCategory),
        field("meleeWeapon", "field.meleeWeapon", FieldType.ENUM, SpaceMarine::getMeleeWeapon),
        field(
            "chapterName", "field.chapterName", FieldType.TEXT, SpaceMarineDescriptor::chapterName),
        field(
            "chapterLegion",
            "field.chapterLegion",
            FieldType.TEXT,
            SpaceMarineDescriptor::chapterLegion),
        field("owner", "field.owner", FieldType.TEXT, SpaceMarine::getOwner));
  }

  private static FieldDescriptor<SpaceMarine> field(
      String key, String labelKey, FieldType type, Function<SpaceMarine, ?> ext) {
    return FieldDescriptor.<SpaceMarine>builder(key, labelKey, type).extractor(ext).build();
  }

  private static String chapterName(SpaceMarine sm) {
    Chapter chapter = sm.getChapter();
    return chapter == null ? null : chapter.getName();
  }

  private static String chapterLegion(SpaceMarine sm) {
    Chapter chapter = sm.getChapter();
    return chapter == null ? null : chapter.getParentLegion();
  }
}
