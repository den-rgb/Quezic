package com.quezic;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.quezic.data.local.ProxySettings;
import com.quezic.data.local.QuezicDatabase;
import com.quezic.data.local.dao.PlaylistDao;
import com.quezic.data.local.dao.SongDao;
import com.quezic.data.remote.InvidiousApiService;
import com.quezic.data.remote.LastFmService;
import com.quezic.data.remote.MusicExtractorService;
import com.quezic.data.remote.PipedApiService;
import com.quezic.data.remote.SoundCloudApiService;
import com.quezic.data.remote.SpotifyApiService;
import com.quezic.di.AppModule_ProvideInvidiousApiServiceFactory;
import com.quezic.di.AppModule_ProvideMusicExtractorServiceFactory;
import com.quezic.di.AppModule_ProvideMusicRepositoryFactory;
import com.quezic.di.AppModule_ProvidePipedApiServiceFactory;
import com.quezic.di.AppModule_ProvidePlaylistRepositoryFactory;
import com.quezic.di.AppModule_ProvideProxySettingsFactory;
import com.quezic.di.AppModule_ProvideSongMatcherServiceFactory;
import com.quezic.di.AppModule_ProvideSpotifyApiServiceFactory;
import com.quezic.di.DatabaseModule_ProvideDatabaseFactory;
import com.quezic.di.DatabaseModule_ProvidePlaylistDaoFactory;
import com.quezic.di.DatabaseModule_ProvideSongDaoFactory;
import com.quezic.di.DownloadModule_ProvideDownloadManagerFactory;
import com.quezic.di.PlayerModule_ProvidePlayerControllerFactory;
import com.quezic.di.RecommendationModule_ProvideLastFmServiceFactory;
import com.quezic.di.RecommendationModule_ProvideRecommendationEngineFactory;
import com.quezic.domain.recommendation.RecommendationEngine;
import com.quezic.domain.repository.MusicRepository;
import com.quezic.domain.repository.PlaylistRepository;
import com.quezic.domain.service.SongMatcherService;
import com.quezic.download.DownloadManager;
import com.quezic.download.DownloadService;
import com.quezic.download.DownloadWorker;
import com.quezic.download.DownloadWorker_AssistedFactory;
import com.quezic.player.AudioVisualizerHelper;
import com.quezic.player.PlaybackService;
import com.quezic.player.PlaybackService_MembersInjector;
import com.quezic.player.PlayerController;
import com.quezic.ui.screens.home.HomeViewModel;
import com.quezic.ui.screens.home.HomeViewModel_HiltModules_KeyModule_ProvideFactory;
import com.quezic.ui.screens.library.LibraryViewModel;
import com.quezic.ui.screens.library.LibraryViewModel_HiltModules_KeyModule_ProvideFactory;
import com.quezic.ui.screens.playlist.PlaylistViewModel;
import com.quezic.ui.screens.playlist.PlaylistViewModel_HiltModules_KeyModule_ProvideFactory;
import com.quezic.ui.screens.search.SearchViewModel;
import com.quezic.ui.screens.search.SearchViewModel_HiltModules_KeyModule_ProvideFactory;
import com.quezic.ui.screens.settings.SettingsViewModel;
import com.quezic.ui.screens.settings.SettingsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.quezic.ui.screens.soundcloud.ImportSoundCloudViewModel;
import com.quezic.ui.screens.soundcloud.ImportSoundCloudViewModel_HiltModules_KeyModule_ProvideFactory;
import com.quezic.ui.screens.spotify.ImportSpotifyViewModel;
import com.quezic.ui.screens.spotify.ImportSpotifyViewModel_HiltModules_KeyModule_ProvideFactory;
import com.quezic.ui.viewmodel.PlayerViewModel;
import com.quezic.ui.viewmodel.PlayerViewModel_HiltModules_KeyModule_ProvideFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerQuezicApp_HiltComponents_SingletonC {
  private DaggerQuezicApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public QuezicApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements QuezicApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public QuezicApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements QuezicApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public QuezicApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements QuezicApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public QuezicApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements QuezicApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public QuezicApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements QuezicApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public QuezicApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements QuezicApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public QuezicApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements QuezicApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public QuezicApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends QuezicApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends QuezicApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends QuezicApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends QuezicApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Set<String> getViewModelKeys() {
      return ImmutableSet.<String>of(HomeViewModel_HiltModules_KeyModule_ProvideFactory.provide(), ImportSoundCloudViewModel_HiltModules_KeyModule_ProvideFactory.provide(), ImportSpotifyViewModel_HiltModules_KeyModule_ProvideFactory.provide(), LibraryViewModel_HiltModules_KeyModule_ProvideFactory.provide(), PlayerViewModel_HiltModules_KeyModule_ProvideFactory.provide(), PlaylistViewModel_HiltModules_KeyModule_ProvideFactory.provide(), SearchViewModel_HiltModules_KeyModule_ProvideFactory.provide(), SettingsViewModel_HiltModules_KeyModule_ProvideFactory.provide());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends QuezicApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<ImportSoundCloudViewModel> importSoundCloudViewModelProvider;

    private Provider<ImportSpotifyViewModel> importSpotifyViewModelProvider;

    private Provider<LibraryViewModel> libraryViewModelProvider;

    private Provider<PlayerViewModel> playerViewModelProvider;

    private Provider<PlaylistViewModel> playlistViewModelProvider;

    private Provider<SearchViewModel> searchViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.importSoundCloudViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.importSpotifyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.libraryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.playerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.playlistViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.searchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
    }

    @Override
    public Map<String, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(8).put("com.quezic.ui.screens.home.HomeViewModel", ((Provider) homeViewModelProvider)).put("com.quezic.ui.screens.soundcloud.ImportSoundCloudViewModel", ((Provider) importSoundCloudViewModelProvider)).put("com.quezic.ui.screens.spotify.ImportSpotifyViewModel", ((Provider) importSpotifyViewModelProvider)).put("com.quezic.ui.screens.library.LibraryViewModel", ((Provider) libraryViewModelProvider)).put("com.quezic.ui.viewmodel.PlayerViewModel", ((Provider) playerViewModelProvider)).put("com.quezic.ui.screens.playlist.PlaylistViewModel", ((Provider) playlistViewModelProvider)).put("com.quezic.ui.screens.search.SearchViewModel", ((Provider) searchViewModelProvider)).put("com.quezic.ui.screens.settings.SettingsViewModel", ((Provider) settingsViewModelProvider)).build();
    }

    @Override
    public Map<String, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<String, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.quezic.ui.screens.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.providePlaylistRepositoryProvider.get(), singletonCImpl.providePlayerControllerProvider.get());

          case 1: // com.quezic.ui.screens.soundcloud.ImportSoundCloudViewModel 
          return (T) new ImportSoundCloudViewModel(singletonCImpl.soundCloudApiServiceProvider.get(), singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.providePlaylistRepositoryProvider.get());

          case 2: // com.quezic.ui.screens.spotify.ImportSpotifyViewModel 
          return (T) new ImportSpotifyViewModel(singletonCImpl.provideSpotifyApiServiceProvider.get(), singletonCImpl.provideSongMatcherServiceProvider.get(), singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.providePlaylistRepositoryProvider.get());

          case 3: // com.quezic.ui.screens.library.LibraryViewModel 
          return (T) new LibraryViewModel(singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.providePlaylistRepositoryProvider.get(), singletonCImpl.provideDownloadManagerProvider.get());

          case 4: // com.quezic.ui.viewmodel.PlayerViewModel 
          return (T) new PlayerViewModel(singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.providePlaylistRepositoryProvider.get(), singletonCImpl.providePlayerControllerProvider.get());

          case 5: // com.quezic.ui.screens.playlist.PlaylistViewModel 
          return (T) new PlaylistViewModel(singletonCImpl.providePlaylistRepositoryProvider.get(), singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.provideRecommendationEngineProvider.get(), singletonCImpl.provideDownloadManagerProvider.get());

          case 6: // com.quezic.ui.screens.search.SearchViewModel 
          return (T) new SearchViewModel(singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.providePlaylistRepositoryProvider.get());

          case 7: // com.quezic.ui.screens.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideProxySettingsProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends QuezicApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends QuezicApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectDownloadService(DownloadService downloadService) {
    }

    @Override
    public void injectPlaybackService(PlaybackService playbackService) {
      injectPlaybackService2(playbackService);
    }

    private PlaybackService injectPlaybackService2(PlaybackService instance) {
      PlaybackService_MembersInjector.injectPlayerController(instance, singletonCImpl.providePlayerControllerProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends QuezicApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<PipedApiService> providePipedApiServiceProvider;

    private Provider<InvidiousApiService> provideInvidiousApiServiceProvider;

    private Provider<ProxySettings> provideProxySettingsProvider;

    private Provider<MusicExtractorService> provideMusicExtractorServiceProvider;

    private Provider<QuezicDatabase> provideDatabaseProvider;

    private Provider<SongDao> provideSongDaoProvider;

    private Provider<DownloadManager> provideDownloadManagerProvider;

    private Provider<DownloadWorker_AssistedFactory> downloadWorker_AssistedFactoryProvider;

    private Provider<MusicRepository> provideMusicRepositoryProvider;

    private Provider<PlaylistDao> providePlaylistDaoProvider;

    private Provider<PlaylistRepository> providePlaylistRepositoryProvider;

    private Provider<AudioVisualizerHelper> audioVisualizerHelperProvider;

    private Provider<PlayerController> providePlayerControllerProvider;

    private Provider<SoundCloudApiService> soundCloudApiServiceProvider;

    private Provider<SpotifyApiService> provideSpotifyApiServiceProvider;

    private Provider<SongMatcherService> provideSongMatcherServiceProvider;

    private Provider<LastFmService> provideLastFmServiceProvider;

    private Provider<RecommendationEngine> provideRecommendationEngineProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return ImmutableMap.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>of("com.quezic.download.DownloadWorker", ((Provider) downloadWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.providePipedApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<PipedApiService>(singletonCImpl, 2));
      this.provideInvidiousApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<InvidiousApiService>(singletonCImpl, 3));
      this.provideProxySettingsProvider = DoubleCheck.provider(new SwitchingProvider<ProxySettings>(singletonCImpl, 4));
      this.provideMusicExtractorServiceProvider = DoubleCheck.provider(new SwitchingProvider<MusicExtractorService>(singletonCImpl, 1));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<QuezicDatabase>(singletonCImpl, 6));
      this.provideSongDaoProvider = DoubleCheck.provider(new SwitchingProvider<SongDao>(singletonCImpl, 5));
      this.provideDownloadManagerProvider = DoubleCheck.provider(new SwitchingProvider<DownloadManager>(singletonCImpl, 7));
      this.downloadWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<DownloadWorker_AssistedFactory>(singletonCImpl, 0));
      this.provideMusicRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<MusicRepository>(singletonCImpl, 8));
      this.providePlaylistDaoProvider = DoubleCheck.provider(new SwitchingProvider<PlaylistDao>(singletonCImpl, 10));
      this.providePlaylistRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<PlaylistRepository>(singletonCImpl, 9));
      this.audioVisualizerHelperProvider = DoubleCheck.provider(new SwitchingProvider<AudioVisualizerHelper>(singletonCImpl, 12));
      this.providePlayerControllerProvider = DoubleCheck.provider(new SwitchingProvider<PlayerController>(singletonCImpl, 11));
      this.soundCloudApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<SoundCloudApiService>(singletonCImpl, 13));
      this.provideSpotifyApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<SpotifyApiService>(singletonCImpl, 14));
      this.provideSongMatcherServiceProvider = DoubleCheck.provider(new SwitchingProvider<SongMatcherService>(singletonCImpl, 15));
      this.provideLastFmServiceProvider = DoubleCheck.provider(new SwitchingProvider<LastFmService>(singletonCImpl, 17));
      this.provideRecommendationEngineProvider = DoubleCheck.provider(new SwitchingProvider<RecommendationEngine>(singletonCImpl, 16));
    }

    @Override
    public void injectQuezicApp(QuezicApp quezicApp) {
      injectQuezicApp2(quezicApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private QuezicApp injectQuezicApp2(QuezicApp instance) {
      QuezicApp_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.quezic.download.DownloadWorker_AssistedFactory 
          return (T) new DownloadWorker_AssistedFactory() {
            @Override
            public DownloadWorker create(Context context, WorkerParameters params) {
              return new DownloadWorker(context, params, singletonCImpl.provideMusicExtractorServiceProvider.get(), singletonCImpl.provideSongDaoProvider.get(), singletonCImpl.provideDownloadManagerProvider.get());
            }
          };

          case 1: // com.quezic.data.remote.MusicExtractorService 
          return (T) AppModule_ProvideMusicExtractorServiceFactory.provideMusicExtractorService(singletonCImpl.providePipedApiServiceProvider.get(), singletonCImpl.provideInvidiousApiServiceProvider.get(), singletonCImpl.provideProxySettingsProvider.get());

          case 2: // com.quezic.data.remote.PipedApiService 
          return (T) AppModule_ProvidePipedApiServiceFactory.providePipedApiService();

          case 3: // com.quezic.data.remote.InvidiousApiService 
          return (T) AppModule_ProvideInvidiousApiServiceFactory.provideInvidiousApiService();

          case 4: // com.quezic.data.local.ProxySettings 
          return (T) AppModule_ProvideProxySettingsFactory.provideProxySettings(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.quezic.data.local.dao.SongDao 
          return (T) DatabaseModule_ProvideSongDaoFactory.provideSongDao(singletonCImpl.provideDatabaseProvider.get());

          case 6: // com.quezic.data.local.QuezicDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // com.quezic.download.DownloadManager 
          return (T) DownloadModule_ProvideDownloadManagerFactory.provideDownloadManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.quezic.domain.repository.MusicRepository 
          return (T) AppModule_ProvideMusicRepositoryFactory.provideMusicRepository(singletonCImpl.provideSongDaoProvider.get(), singletonCImpl.provideMusicExtractorServiceProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 9: // com.quezic.domain.repository.PlaylistRepository 
          return (T) AppModule_ProvidePlaylistRepositoryFactory.providePlaylistRepository(singletonCImpl.providePlaylistDaoProvider.get(), singletonCImpl.provideSongDaoProvider.get());

          case 10: // com.quezic.data.local.dao.PlaylistDao 
          return (T) DatabaseModule_ProvidePlaylistDaoFactory.providePlaylistDao(singletonCImpl.provideDatabaseProvider.get());

          case 11: // com.quezic.player.PlayerController 
          return (T) PlayerModule_ProvidePlayerControllerFactory.providePlayerController(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideMusicRepositoryProvider.get(), singletonCImpl.audioVisualizerHelperProvider.get());

          case 12: // com.quezic.player.AudioVisualizerHelper 
          return (T) new AudioVisualizerHelper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 13: // com.quezic.data.remote.SoundCloudApiService 
          return (T) new SoundCloudApiService();

          case 14: // com.quezic.data.remote.SpotifyApiService 
          return (T) AppModule_ProvideSpotifyApiServiceFactory.provideSpotifyApiService();

          case 15: // com.quezic.domain.service.SongMatcherService 
          return (T) AppModule_ProvideSongMatcherServiceFactory.provideSongMatcherService(singletonCImpl.provideMusicExtractorServiceProvider.get());

          case 16: // com.quezic.domain.recommendation.RecommendationEngine 
          return (T) RecommendationModule_ProvideRecommendationEngineFactory.provideRecommendationEngine(singletonCImpl.provideMusicExtractorServiceProvider.get(), singletonCImpl.provideLastFmServiceProvider.get());

          case 17: // com.quezic.data.remote.LastFmService 
          return (T) RecommendationModule_ProvideLastFmServiceFactory.provideLastFmService();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
