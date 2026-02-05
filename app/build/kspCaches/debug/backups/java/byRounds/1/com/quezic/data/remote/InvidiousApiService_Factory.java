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
public final class InvidiousApiService_Factory implements Factory<InvidiousApiService> {
  @Override
  public InvidiousApiService get() {
    return newInstance();
  }

  public static InvidiousApiService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static InvidiousApiService newInstance() {
    return new InvidiousApiService();
  }

  private static final class InstanceHolder {
    private static final InvidiousApiService_Factory INSTANCE = new InvidiousApiService_Factory();
  }
}
