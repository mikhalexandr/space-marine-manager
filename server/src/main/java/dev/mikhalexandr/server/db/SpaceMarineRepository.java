package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.models.SpaceMarine;
import java.util.List;
import java.util.Optional;

public interface SpaceMarineRepository {

  void insert(SpaceMarine marine, String ownerLogin);

  boolean update(int id, SpaceMarine marine);

  boolean deleteByIdAndOwner(int id, String ownerLogin);

  int deleteByOwner(String ownerLogin);

  Optional<SpaceMarine> findById(int id);

  List<SpaceMarine> findAll();
}
