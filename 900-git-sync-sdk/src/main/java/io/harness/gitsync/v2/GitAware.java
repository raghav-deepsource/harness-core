package io.harness.gitsync.v2;

import io.harness.beans.WithIdentifier;
import io.harness.persistence.PersistentEntity;

/**
 * All the persistent entities which want to be part of the git experience need to implement this interface
 * The SDK should never deal with the entity itself, all the interactions will be driven by the interface
 */
public interface GitAware extends PersistentEntity, WithIdentifier {
  // Defines where this entity is stored
  StoreType getStoreType();

  // TODO : As this will be a blob does it make sense for this to be a byte[]
  // The String representation of the resource mostly yaml string
  String getData();

  // Repo in which the entity will be saved
  String getRepo();

  // The path of the file in the repo
  String getPath();

  // Connector Identifier which will be used connect to the repo
  String getConnectorRef();
}
