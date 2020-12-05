/**
 * Copyright 2019 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.github.ambry.cloud;

import com.codahale.metrics.MetricRegistry;
import com.github.ambry.account.AccountService;
import com.github.ambry.clustermap.ClusterMap;
import com.github.ambry.clustermap.VirtualReplicatorCluster;
import com.github.ambry.clustermap.VirtualReplicatorClusterFactory;
import com.github.ambry.config.CloudConfig;
import com.github.ambry.config.ClusterMapConfig;
import com.github.ambry.config.StoreConfig;


/**
 * {@link StaticVcrClusterFactory} to generate VCR Cluster for static partition assignment.
 */
public class StaticVcrClusterFactory implements VirtualReplicatorClusterFactory {

  private final CloudConfig cloudConfig;
  private final ClusterMapConfig clusterMapConfig;
  private final ClusterMap clusterMap;
  private VirtualReplicatorCluster virtualReplicatorCluster;

  public StaticVcrClusterFactory(CloudConfig cloudConfig, ClusterMapConfig clusterMapConfig, ClusterMap clusterMap,
      AccountService accountService, StoreConfig storeConfig, CloudDestination cloudDestination,
      MetricRegistry metricRegistry) {
    this.cloudConfig = cloudConfig;
    this.clusterMapConfig = clusterMapConfig;
    this.clusterMap = clusterMap;
  }

  @Override
  synchronized public VirtualReplicatorCluster getVirtualReplicatorCluster() {
    if (virtualReplicatorCluster == null) {
      virtualReplicatorCluster = new StaticVcrCluster(cloudConfig, clusterMapConfig, clusterMap);
    }
    return virtualReplicatorCluster;
  }
}
