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
package com.github.ambry.cloud.azure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ambry.cloud.CloudBlobMetadata;
import com.github.ambry.cloud.CloudRequestAgent;
import com.github.ambry.cloud.CloudStorageException;
import com.github.ambry.clustermap.MockClusterMap;
import com.github.ambry.clustermap.MockPartitionId;
import com.github.ambry.clustermap.PartitionId;
import com.github.ambry.commons.BlobId;
import com.github.ambry.utils.TestUtils;
import com.github.ambry.utils.Utils;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import com.microsoft.azure.cosmosdb.StoredProcedureResponse;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;
import rx.Observable;
import rx.observables.BlockingObservable;

import static com.github.ambry.commons.BlobId.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Class to define utilities for Azure tests.
 */
class AzureTestUtils {

  static final String base64key = Base64.encodeBase64String("ambrykey".getBytes());
  static final String storageConnection =
      "DefaultEndpointsProtocol=https;AccountName=ambry;AccountKey=" + base64key + ";EndpointSuffix=core.windows.net";
  static final byte dataCenterId = 66;
  static final short accountId = 101;
  static final short containerId = 5;
  static final long partition = 666;
  static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create {@link Document} object from {@link CloudBlobMetadata} object.
   * @param cloudBlobMetadata {@link CloudBlobMetadata} object.
   * @return {@link Document} object.
   */
  static Document createDocumentFromCloudBlobMetadata(CloudBlobMetadata cloudBlobMetadata) throws IOException {
    Document document = new Document(objectMapper.writeValueAsString(cloudBlobMetadata));
    document.set(CosmosDataAccessor.COSMOS_LAST_UPDATED_COLUMN, System.currentTimeMillis());
    return document;
  }

  /**
   * Create {@link Document} object from {@link CloudBlobMetadata} object with specified updateTime.
   * @param cloudBlobMetadata {@link CloudBlobMetadata} object.
   * @param uploadTime specified upload time.
   * @return {@link Document} object.
   */
  static Document createDocumentFromCloudBlobMetadata(CloudBlobMetadata cloudBlobMetadata, long uploadTime)
      throws JsonProcessingException {
    Document document = new Document(objectMapper.writeValueAsString(cloudBlobMetadata));
    document.set(CosmosDataAccessor.COSMOS_LAST_UPDATED_COLUMN, uploadTime);
    return document;
  }

  static void setConfigProperties(Properties configProps) {
    configProps.setProperty(AzureCloudConfig.AZURE_STORAGE_CONNECTION_STRING, storageConnection);
    configProps.setProperty(AzureCloudConfig.COSMOS_ENDPOINT, "http://ambry.beyond-the-cosmos.com");
    configProps.setProperty(AzureCloudConfig.COSMOS_COLLECTION_LINK, "ambry/metadata");
    configProps.setProperty(AzureCloudConfig.COSMOS_DELETED_CONTAINER_COLLECTION_LINK, "ambry/deletedContainer");
    configProps.setProperty(AzureCloudConfig.COSMOS_KEY, "cosmos-key");
    configProps.setProperty(AzureCloudConfig.AZURE_STORAGE_AUTHORITY,
        "https://login.microsoftonline.com/test-account/");
    configProps.setProperty(AzureCloudConfig.AZURE_STORAGE_CLIENTID, "client-id");
    configProps.setProperty(AzureCloudConfig.AZURE_STORAGE_SECRET, "client-secret");
    configProps.setProperty(AzureCloudConfig.AZURE_STORAGE_ENDPOINT, "https://azure_storage.blob.core.windows.net");
    configProps.setProperty(AzureCloudConfig.AZURE_STORAGE_CLIENT_CLASS,
        "com.github.ambry.cloud.azure.ConnectionStringBasedStorageClient");
    configProps.setProperty("clustermap.cluster.name", "main");
    configProps.setProperty("clustermap.datacenter.name", "uswest");
    configProps.setProperty("clustermap.host.name", "localhost");
  }

  /**
   * Utility method to generate a BlobId.
   * @return a BlobId for the default attributes.
   */
  static BlobId generateBlobId() {
    PartitionId partitionId = new MockPartitionId(partition, MockClusterMap.DEFAULT_PARTITION_CLASS);
    return new BlobId(BLOB_ID_V6, BlobIdType.NATIVE, dataCenterId, accountId, containerId, partitionId, false,
        BlobDataType.DATACHUNK);
  }

  /**
   * Utility method to get blob input stream.
   * @param blobSize size of blob to consider.
   * @return the blob input stream.
   */
  static InputStream getBlobInputStream(int blobSize) {
    byte[] randomBytes = TestUtils.getRandomBytes(blobSize);
    return new ByteArrayInputStream(randomBytes);
  }

  /**
   * Utility to mock the call chain to get mocked {@link Observable} for single resource from {@link AsyncDocumentClient}.
   * @param metadata the {@link CloudBlobMetadata} to return as a document.
   * @return {@link Observable< ResourceResponse <Document>>} object.
   */
  static Observable<ResourceResponse<Document>> getMockedObservableForSingleResource(CloudBlobMetadata metadata)
      throws IOException {
    Observable<ResourceResponse<Document>> mockResponse = mock(Observable.class);
    BlockingObservable<ResourceResponse<Document>> mockBlockingObservable = mock(BlockingObservable.class);
    when(mockResponse.toBlocking()).thenReturn(mockBlockingObservable);
    ResourceResponse<Document> mockResourceResponse = mock(ResourceResponse.class);
    when(mockBlockingObservable.single()).thenReturn(mockResourceResponse);
    Document metadataDoc = createDocumentFromCloudBlobMetadata(metadata);
    when(mockResourceResponse.getResource()).thenReturn(metadataDoc);
    return mockResponse;
  }

  static Observable<StoredProcedureResponse> getMockBulkDeleteResponse(int count) {
    Observable<StoredProcedureResponse> mockObservable = mock(Observable.class);
    BlockingObservable<StoredProcedureResponse> mockBlockingObservable = mock(BlockingObservable.class);
    when(mockObservable.toBlocking()).thenReturn(mockBlockingObservable);
    StoredProcedureResponse mockResponse = mock(StoredProcedureResponse.class);
    when(mockBlockingObservable.single()).thenReturn(mockResponse);
    Document responseDoc = new Document();
    responseDoc.set(CosmosDataAccessor.PROPERTY_CONTINUATION, "false");
    responseDoc.set(CosmosDataAccessor.PROPERTY_DELETED, String.valueOf(count));
    when(mockResponse.getResponseAsDocument()).thenReturn(responseDoc);
    return mockObservable;
  }

  /**
   * Utility to mock the query call chain of {@link AsyncDocumentClient} such that query returns {@code documentList}.
   * @param documentList {@link List <Document>} of documents to return from mocked call.
   * @param mockResponse {@link Observable} mocked response.
   */
  static void mockObservableForQuery(List<Document> documentList, Observable<FeedResponse<Document>> mockResponse) {
    FeedResponse<Document> feedResponse = mock(FeedResponse.class);
    BlockingObservable<FeedResponse<Document>> mockBlockingObservable = mock(BlockingObservable.class);
    when(mockResponse.toBlocking()).thenReturn(mockBlockingObservable);
    lenient().when(mockBlockingObservable.single()).thenReturn(feedResponse);
    when(mockBlockingObservable.getIterator()).thenAnswer(invocation -> {
      Iterator<FeedResponse<Document>> feedResponseIterator = mock(Iterator.class);
      when(feedResponseIterator.hasNext()).thenReturn(true).thenReturn(false);
      when(feedResponseIterator.next()).thenReturn(feedResponse);
      return feedResponseIterator;
    });
    when(feedResponse.getResults()).thenReturn(documentList);
  }

  /**
   * Utility to mock the query call chain of {@link AsyncDocumentClient} such that query returns {@code documentList}.
   * @param documentList {@link List <Document>} of documents to return from mocked call.
   * @param mockResponse {@link Observable} mocked response.
   */
  static void mockObservableForChangeFeedQuery(List<Document> documentList,
      Observable<FeedResponse<Document>> mockResponse) {
    FeedResponse<Document> feedResponse = mock(FeedResponse.class);
    BlockingObservable<FeedResponse<Document>> mockBlockingObservable = mock(BlockingObservable.class);
    Observable<FeedResponse<Document>> mockObservable = mock(Observable.class);
    when(mockResponse.limit(anyInt())).thenReturn(mockObservable);
    when(mockObservable.toBlocking()).thenReturn(mockBlockingObservable);
    when(mockBlockingObservable.single()).thenReturn(feedResponse);
    when(feedResponse.getResults()).thenReturn(documentList);
    Iterator<FeedResponse<Document>> iterator = mock(Iterator.class);
  }

  /**
   * Utility to mock the query call chain of {@link AsyncDocumentClient} such that query returns empty list.
   */
  static Observable<FeedResponse<Document>> getMockedObservableForQueryWithNoResults() {
    Observable<FeedResponse<Document>> mockResponse = mock(Observable.class);
    BlockingObservable<FeedResponse<Document>> mockBlockingObservable = mock(BlockingObservable.class);
    when(mockResponse.toBlocking()).thenReturn(mockBlockingObservable);
    when(mockBlockingObservable.getIterator()).thenReturn(Collections.emptyIterator());
    return mockResponse;
  }

  /**
   * Utility method to create specified number of unencrypted blobs with permanent ttl and with specified properties.
   * @param numBlobs number of blobs to create.
   * @param dataCenterId datacenter id.
   * @param accountId account id.
   * @param containerId container id.
   * @param partitionId {@link PartitionId} of the partition in which blobs will be created.
   * @param blobSize size of blobs.
   * @param cloudRequestAgent {@link CloudRequestAgent} object.
   * @param azureDest {@link AzureCloudDestination} object.
   * @param creationTime blob creation time.
   * @return A {@link Map} of create blobs' {@link BlobId} and data.
   * @throws CloudStorageException in case of any exception while uploading blob.
   */
  static Map<BlobId, byte[]> createUnencryptedPermanentBlobs(int numBlobs, byte dataCenterId, short accountId,
      short containerId, PartitionId partitionId, int blobSize, CloudRequestAgent cloudRequestAgent,
      AzureCloudDestination azureDest, long creationTime) throws CloudStorageException {
    Map<BlobId, byte[]> blobIdtoDataMap = new HashMap<>();
    for (int j = 0; j < numBlobs; j++) {
      BlobId blobId =
          new BlobId(BLOB_ID_V6, BlobIdType.NATIVE, dataCenterId, accountId, containerId, partitionId, false,
              BlobDataType.DATACHUNK);
      byte[] randomBytes = TestUtils.getRandomBytes(blobSize);
      blobIdtoDataMap.put(blobId, randomBytes);
      CloudBlobMetadata cloudBlobMetadata = new CloudBlobMetadata(blobId, creationTime, Utils.Infinite_Time, blobSize,
          CloudBlobMetadata.EncryptionOrigin.NONE);
      assertTrue("Expected upload to return true",
          uploadBlobWithRetry(blobId, blobSize, cloudBlobMetadata, new ByteArrayInputStream(randomBytes),
              cloudRequestAgent, azureDest));
    }
    return blobIdtoDataMap;
  }

  /**
   * Upload blob. Retry with appropriate throttling if required.
   * Upload blob to the cloud destination.
   * @param blobId id of the Ambry blob
   * @param blobSize the length of the input stream, if known, -1 if unknown.
   * @param cloudBlobMetadata the {@link CloudBlobMetadata} for the blob being uploaded.
   * @param inputStream the stream to read blob data
   * @param cloudRequestAgent {@link CloudRequestAgent} object.
   * @param azureDest {@link AzureCloudDestination} object.
   * @return flag indicating whether the blob was uploaded
   * @throws CloudStorageException if the upload encounters an error.
   */
  static boolean uploadBlobWithRetry(BlobId blobId, long blobSize, CloudBlobMetadata cloudBlobMetadata,
      InputStream inputStream, CloudRequestAgent cloudRequestAgent, AzureCloudDestination azureDest)
      throws CloudStorageException {
    return cloudRequestAgent.doWithRetries(() -> azureDest.uploadBlob(blobId, blobSize, cloudBlobMetadata, inputStream),
        "UploadBlob", blobId.getPartition().toPathString());
  }

  /**
   * Get metadata for blobs. Retry with appropriate throttling if required.
   * @param blobIds list of blob Ids to query.
   * @param partitionPath partition of the blobs.
   * @param cloudRequestAgent {@link CloudRequestAgent} object.
   * @param azureDest {@link AzureCloudDestination} object.
   * @return a {@link Map} of blobId strings to {@link CloudBlobMetadata}.  If metadata for a blob could not be found,
   * it will not be included in the returned map.
   * @throws CloudStorageException if query encounters an error.
   */
  static Map<String, CloudBlobMetadata> getBlobMetadataWithRetry(List<BlobId> blobIds, String partitionPath,
      CloudRequestAgent cloudRequestAgent, AzureCloudDestination azureDest) throws CloudStorageException {
    return cloudRequestAgent.doWithRetries(() -> azureDest.getBlobMetadata(blobIds), "GetBlobMetadata", partitionPath);
  }
}
