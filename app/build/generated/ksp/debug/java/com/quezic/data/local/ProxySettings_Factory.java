package com.quezic.data.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ProxySettings_Factory implements Factory<ProxySettings> {
  private final Provider<Context> contextProvider;

  public ProxySettings_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ProxySettings get() {
    return newInstance(contextProvider.get());
  }

  public static ProxySettings_Factory create(Provider<Context> contextProvider) {
    return new ProxySettings_Factory(contextProvider);
  }

  public static ProxySettings newInstance(Context context) {
    return new ProxySettings(context);
  }
}
