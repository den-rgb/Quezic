package com.quezic.di;

import android.content.Context;
import com.quezic.data.local.ProxySettings;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideProxySettingsFactory implements Factory<ProxySettings> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideProxySettingsFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ProxySettings get() {
    return provideProxySettings(contextProvider.get());
  }

  public static AppModule_ProvideProxySettingsFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideProxySettingsFactory(contextProvider);
  }

  public static ProxySettings provideProxySettings(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideProxySettings(context));
  }
}
