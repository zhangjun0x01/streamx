/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flink.kubernetes.kubeclient.resources;

import io.fabric8.kubernetes.api.model.HasMetadata;

/**
 * Compatible AbstractKubernetesWatcher for fabric8.
 */
public abstract class CompatibleKubernetesWatcher<T extends HasMetadata, K extends KubernetesResource<T>>
    extends AbstractKubernetesWatcher<T, K> {

    public CompatibleKubernetesWatcher() {
        super(new SilentWatchCallbackHandler<>());
    }

}

