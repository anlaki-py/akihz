package akihz.anlaki.dev.di

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import akihz.anlaki.dev.data.DisplayManagerDataSource
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.data.RefreshRateRepositoryImpl
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDisplayManagerDataSource(
        @ApplicationContext context: Context
    ): DisplayManagerDataSource {
        return DisplayManagerDataSource(context)
    }

    @Provides
    @Singleton
    fun provideShizukuHelper(): ShizukuHelper = ShizukuHelper

    @Provides
    @Singleton
    fun provideRefreshRateRepository(
        dataSource: DisplayManagerDataSource,
        shizukuHelper: ShizukuHelper
    ): RefreshRateRepository {
        return RefreshRateRepositoryImpl(dataSource, shizukuHelper)
    }
}
