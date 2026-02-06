package com.quezic.download;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.quezic.data.local.dao.SongDao;
import com.quezic.data.remote.MusicExtractorService;
import dagger.internal.DaggerGenerated;
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
public final class DownloadWorker_Factory {
  private final Provider<MusicExtractorService> extractorServiceProvider;

  private final Provider<SongDao> songDaoProvider;

  private final Provider<DownloadManager> downloadManagerProvider;

  public DownloadWorker_Factory(Provider<MusicExtractorService> extractorServiceProvider,
      Provider<SongDao> songDaoProvider, Provider<DownloadManager> downloadManagerProvider) {
    this.extractorServiceProvider = extractorServiceProvider;
    this.songDaoProvider = songDaoProvider;
    this.downloadManagerProvider = downloadManagerProvider;
  }

  public DownloadWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, extractorServiceProvider.get(), songDaoProvider.get(), downloadManagerProvider.get());
  }

  public static DownloadWorker_Factory create(
      Provider<MusicExtractorService> extractorServiceProvider, Provider<SongDao> songDaoProvider,
      Provider<DownloadManager> downloadManagerProvider) {
    return new DownloadWorker_Factory(extractorServiceProvider, songDaoProvider, downloadManagerProvider);
  }

  public static DownloadWorker newInstance(Context context, WorkerParameters params,
      MusicExtractorService extractorService, SongDao songDao, DownloadManager downloadManager) {
    return new DownloadWorker(context, params, extractorService, songDao, downloadManager);
  }
}
