package io.harness.pms.pipeline.mappers;

import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.bson.Document;

@UtilityClass
public class ModuleInfoMapper {
  public static Map<String, LinkedHashMap<String, Object>> getModuleInfo(Map<String, Document> moduleInfo) {
    if (EmptyPredicate.isEmpty(moduleInfo)) {
      return new LinkedHashMap<>();
    }
    Map<String, LinkedHashMap<String, Object>> moduleInfoReturn = new LinkedHashMap<>();
    for (Map.Entry<String, Document> moduleEntry : moduleInfo.entrySet()) {
      Document document = moduleEntry.getValue();
      moduleInfoReturn.put(moduleEntry.getKey(), new LinkedHashMap<>(CollectionUtils.emptyIfNull(document)));
    }
    return moduleInfoReturn;
  }
}
