/*
 * Copyright 2020 LinkedIn Corp. All rights reserved.
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
 *
 */

package com.github.ambry.named;

/**
 * A factory to build instances of {@link NamedBlobDb}.
 */
public interface NamedBlobDbFactory {
  /**
   * @return an instance of {@link NamedBlobDb}.
   * @throws Exception if there is an error during instantiation.
   */
  NamedBlobDb getNamedBlobDb() throws Exception;
}
