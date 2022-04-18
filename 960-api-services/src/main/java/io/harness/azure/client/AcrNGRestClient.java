/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.client;

public interface AcrNGRestClient {
  @retrofit2.http.GET("/v2/_catalog")
  retrofit2.Call<software.wings.helpers.ext.azure.AcrGetRepositoriesResponse> listRepositories(
      @retrofit2.http.Header("Authorization") String basicAuthHeader, @retrofit2.http.Query("last") String last);

  @retrofit2.http.GET("/v2/{repositoryName}/tags/list?n=500&orderby=timedesc")
  retrofit2.Call<software.wings.helpers.ext.azure.AcrGetRepositoryTagsResponse> listRepositoryTags(
      @retrofit2.http.Header("Authorization") String basicAuthHeader,
      @retrofit2.http.Path(value = "repositoryName", encoded = true) String repositoryName);
}
