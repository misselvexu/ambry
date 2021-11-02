/**
 * Copyright 2021 LinkedIn Corp. All rights reserved.
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
package com.github.ambry.quota;

import com.github.ambry.messageformat.BlobInfo;
import com.github.ambry.rest.RestRequest;
import java.util.Map;


/**
 * {@link QuotaEnforcer} implementation for test that always rejects requests.
 */
public class RejectRequestQuotaEnforcer implements QuotaEnforcer {
  private static final float DUMMY_REJECTABLE_USAGE_PERCENTAGE = 101;
  private static final int REJECT_HTTP_STATUS = 429;
  private static final boolean SHOULD_THROTTLE = true;
  private final QuotaSource quotaSource;

  /**
   * Constructor for {@link RejectRequestQuotaEnforcer}.
   * @param quotaSource {@link QuotaSource} object.
   */
  public RejectRequestQuotaEnforcer(QuotaSource quotaSource) {
    this.quotaSource = quotaSource;
  }

  @Override
  public void init() {
  }

  @Override
  public QuotaRecommendation chargeAndRecommend(RestRequest restRequest, BlobInfo blobInfo,
      Map<QuotaName, Double> requestCostMap) {
    return new QuotaRecommendation(SHOULD_THROTTLE, DUMMY_REJECTABLE_USAGE_PERCENTAGE, QuotaName.READ_CAPACITY_UNIT,
        REJECT_HTTP_STATUS, 1);
  }

  @Override
  public QuotaRecommendation recommend(RestRequest restRequest) {
    return new QuotaRecommendation(SHOULD_THROTTLE, DUMMY_REJECTABLE_USAGE_PERCENTAGE, QuotaName.READ_CAPACITY_UNIT,
        REJECT_HTTP_STATUS, 1);
  }

  @Override
  public QuotaSource getQuotaSource() {
    return quotaSource;
  }

  @Override
  public void shutdown() {
  }
}
