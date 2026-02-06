package com.quezic.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class PipedApiService_Factory implements Factory<PipedApiService> {
  @Override
  public PipedApiService get() {
    return newInstance();
  }

  public static PipedApiService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PipedApiService newInstance() {
    return new PipedApiService();
  }

  private static final class InstanceHolder {
    private static final PipedApiService_Factory INSTANCE = new PipedApiService_Factory();
  }
}
