package com.quezic;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class QuezicApp_MembersInjector implements MembersInjector<QuezicApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public QuezicApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<QuezicApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new QuezicApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(QuezicApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.quezic.QuezicApp.workerFactory")
  public static void injectWorkerFactory(QuezicApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
