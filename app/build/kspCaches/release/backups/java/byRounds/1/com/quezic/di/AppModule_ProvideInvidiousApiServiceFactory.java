package com.quezic.di;

import com.quezic.data.remote.InvidiousApiService;
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
public final class AppModule_ProvideInvidiousApiServiceFactory implements Factory<InvidiousApiService> {
  @Override
  public InvidiousApiService get() {
    return provideInvidiousApiService();
  }

  public static AppModule_ProvideInvidiousApiServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static InvidiousApiService provideInvidiousApiService() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideInvidiousApiService());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideInvidiousApiServiceFactory INSTANCE = new AppModule_ProvideInvidiousApiServiceFactory();
  }
}
