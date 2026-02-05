package com.quezic.di;

import com.quezic.data.remote.PipedApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvidePipedApiServiceFactory implements Factory<PipedApiService> {
  @Override
  public PipedApiService get() {
    return providePipedApiService();
  }

  public static AppModule_ProvidePipedApiServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PipedApiService providePipedApiService() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePipedApiService());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvidePipedApiServiceFactory INSTANCE = new AppModule_ProvidePipedApiServiceFactory();
  }
}
