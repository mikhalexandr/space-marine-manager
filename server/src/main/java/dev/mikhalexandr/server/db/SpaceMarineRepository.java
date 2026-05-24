package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.models.SpaceMarine;
import java.util.List;

public interface SpaceMarineRepository {

  void insert(SpaceMarine marine, String ownerLogin);

  boolean update(int id, SpaceMarine marine);

  boolean deleteById(int id);

  int deleteByOwner(String ownerLogin);

  List<SpaceMarine> findAll();
}
