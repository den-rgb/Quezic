package com.quezic.ui.screens.settings;

import com.quezic.data.local.ProxySettings;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<ProxySettings> proxySettingsProvider;

  public SettingsViewModel_Factory(Provider<ProxySettings> proxySettingsProvider) {
    this.proxySettingsProvider = proxySettingsProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(proxySettingsProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<ProxySettings> proxySettingsProvider) {
    return new SettingsViewModel_Factory(proxySettingsProvider);
  }

  public static SettingsViewModel newInstance(ProxySettings proxySettings) {
    return new SettingsViewModel(proxySettings);
  }
}
